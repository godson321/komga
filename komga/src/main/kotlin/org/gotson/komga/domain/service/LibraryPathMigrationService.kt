package org.gotson.komga.domain.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.domain.persistence.SidecarRepository
import org.gotson.komga.infrastructure.configuration.KomgaProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

data class PathMigrationResult(
  val libraryUpdated: Int,
  val seriesUpdated: Int,
  val booksUpdated: Int,
  val sidecarsUpdated: Int,
  val mediaReset: Int,
)

@Service
class LibraryPathMigrationService(
  private val libraryRepository: LibraryRepository,
  private val seriesRepository: SeriesRepository,
  private val bookRepository: BookRepository,
  private val sidecarRepository: SidecarRepository,
  private val mediaRepository: MediaRepository,
  private val komgaProperties: KomgaProperties,
) {
  /**
   * In-memory set of library IDs that have been recently path-migrated.
   * Used as a fast-path; persistent flag files are the authoritative source.
   */
  private val migratedLibraryIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

  /**
   * Returns the path for the persistent migration flag file for a given library.
   */
  private fun flagFilePath(libraryId: String) =
    Path(komgaProperties.database.file).parent.resolve("path-migrated-$libraryId")

  /**
   * Persist a migration flag to disk so it survives application restarts.
   */
  private fun persistMigrationFlag(libraryId: String) {
    try {
      val flagFile = flagFilePath(libraryId)
      if (!flagFile.exists()) flagFile.createFile()
      logger.info { "Persisted migration flag for library $libraryId at $flagFile" }
    } catch (e: Exception) {
      logger.error(e) { "Failed to persist migration flag for library $libraryId" }
    }
  }

  /**
   * Check and consume the migration flag for a library.
   * Returns true if the library was recently path-migrated (from either in-memory or persistent flag).
   * The flag is consumed (removed) after being read.
   */
  fun consumeMigrationFlag(libraryId: String): Boolean {
    val inMemory = migratedLibraryIds.remove(libraryId)
    val flagFile = flagFilePath(libraryId)
    val onDisk = flagFile.exists()
    if (onDisk) {
      try {
        flagFile.deleteIfExists()
        logger.info { "Consumed persistent migration flag for library $libraryId" }
      } catch (e: Exception) {
        logger.error(e) { "Failed to delete migration flag file for library $libraryId" }
      }
    }
    return inMemory || onDisk
  }

  /**
   * Migrate library paths from oldPathPrefix to newPathPrefix.
   * This updates the library root path and all series/book/sidecar URLs
   * without triggering any re-analysis.
   *
   * Also resets any OUTDATED media status to READY and persists a migration flag
   * so the next scan (including auto-scan on startup) will skip analysis.
   *
   * @param libraryId The library to migrate
   * @param oldPathPrefix The old path prefix (e.g., "W:" or "W:/Comics")
   * @param newPathPrefix The new path prefix (e.g., "Z:" or "Z:/Media/Comics")
   * @return PathMigrationResult with counts of updated records
   */
  @Transactional
  fun migratePath(
    libraryId: String,
    oldPathPrefix: String,
    newPathPrefix: String,
  ): PathMigrationResult {
    logger.info { "Migrating library $libraryId paths from '$oldPathPrefix' to '$newPathPrefix'" }

    // Convert paths to URL-encoded format to match database storage
    // Paths in the database are stored as URL-encoded strings (e.g., "file:/W:/14%20%E6%BC%AB%E7%94%BB")
    val oldPathNormalized = Paths.get(oldPathPrefix).toUri().toURL().toString().removePrefix("file:").trimEnd('/')
    val newPathNormalized = Paths.get(newPathPrefix).toUri().toURL().toString().removePrefix("file:").trimEnd('/')

    // Update library root path (stored as URL, e.g., "file:/W:/Comics")
    val libraryUpdated = libraryRepository.migrateRootPath(libraryId, oldPathNormalized, newPathNormalized)
    logger.info { "Updated $libraryUpdated library root path" }

    // Update series URLs
    val seriesUpdated = seriesRepository.migrateUrls(libraryId, oldPathNormalized, newPathNormalized)
    logger.info { "Updated $seriesUpdated series URLs" }

    // Update book URLs
    val booksUpdated = bookRepository.migrateUrls(libraryId, oldPathNormalized, newPathNormalized)
    logger.info { "Updated $booksUpdated book URLs" }

    // Update sidecar URLs
    val sidecarsUpdated = sidecarRepository.migrateUrls(libraryId, oldPathNormalized, newPathNormalized)
    logger.info { "Updated $sidecarsUpdated sidecar URLs" }

    // Reset OUTDATED media status to READY so existing OUTDATED books don't trigger analysis
    val mediaReset = mediaRepository.resetOutdatedStatusByLibraryId(libraryId)
    if (mediaReset > 0) {
      logger.info { "Reset $mediaReset OUTDATED media to READY for library $libraryId" }
    }

    logger.info { "Migration completed for library $libraryId" }

    // Set both in-memory flag (for immediate scan) and persistent flag (survives restart)
    migratedLibraryIds.add(libraryId)
    persistMigrationFlag(libraryId)
    logger.info { "Library $libraryId marked as migrated, next scan will skip analysis for matched books" }

    return PathMigrationResult(
      libraryUpdated = libraryUpdated,
      seriesUpdated = seriesUpdated,
      booksUpdated = booksUpdated,
      sidecarsUpdated = sidecarsUpdated,
      mediaReset = mediaReset,
    )
  }
}
