package org.gotson.komga.domain.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.domain.persistence.SidecarRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

data class PathMigrationResult(
  val libraryUpdated: Int,
  val seriesUpdated: Int,
  val booksUpdated: Int,
  val sidecarsUpdated: Int,
)

@Service
class LibraryPathMigrationService(
  private val libraryRepository: LibraryRepository,
  private val seriesRepository: SeriesRepository,
  private val bookRepository: BookRepository,
  private val sidecarRepository: SidecarRepository,
) {
  /**
   * Migrate library paths from oldPathPrefix to newPathPrefix.
   * This updates the library root path and all series/book/sidecar URLs
   * without triggering any re-analysis.
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

    // Normalize paths: convert backslashes to forward slashes for URL format
    val oldPathNormalized = oldPathPrefix.replace("\\", "/")
    val newPathNormalized = newPathPrefix.replace("\\", "/")

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

    logger.info { "Migration completed for library $libraryId" }

    return PathMigrationResult(
      libraryUpdated = libraryUpdated,
      seriesUpdated = seriesUpdated,
      booksUpdated = booksUpdated,
      sidecarsUpdated = sidecarsUpdated,
    )
  }
}
