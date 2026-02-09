package org.gotson.komga.infrastructure.mediacontainer.divina

import com.github.gotson.nightcompress.Archive
import com.github.gotson.nightcompress.ReadSupportCompression
import com.github.gotson.nightcompress.ReadSupportFilter
import com.github.gotson.nightcompress.ReadSupportFormat
import io.github.oshai.kotlinlogging.KotlinLogging
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import org.gotson.komga.domain.model.MediaContainerEntry
import org.gotson.komga.domain.model.MediaType
import org.gotson.komga.infrastructure.image.ImageAnalyzer
import org.gotson.komga.infrastructure.mediacontainer.ContentDetector
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

@Configuration(proxyBeanMethods = false)
class Rar5Configuration : BeanDefinitionRegistryPostProcessor {
  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {}

  override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
    val has7z = SevenZipHelper.isAvailable()
    val hasLibarchive = try { Archive.isAvailable() } catch (_: Throwable) { false }
    if (has7z || hasLibarchive) {
      val builder = BeanDefinitionBuilder.genericBeanDefinition(Rar5Extractor::class.java).setLazyInit(true)
      registry.registerBeanDefinition("rar5Extractor", builder.beanDefinition)
      logger.info { "Rar5 extractor is enabled (7z=$has7z, libarchive=$hasLibarchive)" }
    } else {
      logger.warn { "Rar5 extractor is disabled: neither 7-Zip nor libarchive available" }
    }
  }
}

class Rar5Extractor(
  private val contentDetector: ContentDetector,
  private val imageAnalyzer: ImageAnalyzer,
) : DivinaExtractor {
  private val natSortComparator: Comparator<String> = CaseInsensitiveSimpleNaturalComparator.getInstance()
  private val has7z = SevenZipHelper.isAvailable()
  private val hasLibarchive = try { Archive.isAvailable() } catch (_: Throwable) { false }

  override fun mediaTypes(): List<String> = listOf(MediaType.RAR_5.type)

  // ── getEntries ───────────────────────────────────────────────
  override fun getEntries(
    path: Path,
    analyzeDimensions: Boolean,
  ): List<MediaContainerEntry> {
    // Try 7-Zip first (fast header-only listing)
    if (has7z) {
      try {
        val result = getEntriesWith7z(path, analyzeDimensions)
        if (result.isNotEmpty()) return result
      } catch (e: Exception) {
        logger.warn(e) { "getEntries: 7z failed, trying libarchive" }
      }
    }
    // Fallback: libarchive
    if (hasLibarchive) {
      return getEntriesWithLibarchive(path, analyzeDimensions)
    }
    logger.error { "getEntries: no backend available for '$path'" }
    return emptyList()
  }

  private fun getEntriesWith7z(
    path: Path,
    analyzeDimensions: Boolean,
  ): List<MediaContainerEntry> {
    val entries = SevenZipHelper.listEntries(path)
    return entries.map { entry ->
      if (analyzeDimensions) {
        try {
          val buffer = getEntryStream(path, entry.name)
          val mediaType = buffer.inputStream().use { contentDetector.detectMediaType(it) }
          val dimension =
            if (contentDetector.isImage(mediaType))
              buffer.inputStream().use { imageAnalyzer.getDimension(it) }
            else
              null
          MediaContainerEntry(name = entry.name, mediaType = mediaType, dimension = dimension, fileSize = entry.size)
        } catch (e: Exception) {
          logger.warn(e) { "Could not analyze entry: ${entry.name}" }
          MediaContainerEntry(name = entry.name, comment = e.message)
        }
      } else {
        val mediaType = contentDetector.detectMediaTypeByName(entry.name)
        MediaContainerEntry(name = entry.name, mediaType = mediaType, dimension = null, fileSize = entry.size)
      }
    }.sortedWith(compareBy(natSortComparator) { it.name })
  }

  private fun getEntriesWithLibarchive(
    path: Path,
    analyzeDimensions: Boolean,
  ): List<MediaContainerEntry> =
    Archive(path, setOf(if (analyzeDimensions) ReadSupportCompression.ALL else ReadSupportCompression.NONE), setOf(ReadSupportFilter.NONE), setOf(ReadSupportFormat.RAR5)).use { rar ->
      generateSequence { rar.nextEntry }
        .map { entry ->
          if (analyzeDimensions) {
            try {
              val buffer = rar.inputStream.use { it.readBytes() }
              val mediaType = buffer.inputStream().use { contentDetector.detectMediaType(it) }
              val dimension =
                if (contentDetector.isImage(mediaType))
                  buffer.inputStream().use { imageAnalyzer.getDimension(it) }
                else
                  null
              MediaContainerEntry(name = entry.name, mediaType = mediaType, dimension = dimension, fileSize = entry.size)
            } catch (e: Exception) {
              logger.warn(e) { "Could not analyze entry: ${entry.name}" }
              MediaContainerEntry(name = entry.name, comment = e.message)
            }
          } else {
            val mediaType = contentDetector.detectMediaTypeByName(entry.name)
            MediaContainerEntry(name = entry.name, mediaType = mediaType, dimension = null, fileSize = entry.size)
          }
        }.sortedWith(compareBy(natSortComparator) { it.name })
        .toList()
    }

  // ── getEntryStream ──────────────────────────────────────────
  override fun getEntryStream(
    path: Path,
    entryName: String,
  ): ByteArray {
    logger.info { "getEntryStream: path=$path, entryName=$entryName" }

    // 1) Primary: 7-Zip CLI
    if (has7z) {
      try {
        val seven = SevenZipHelper.extractEntry(path, entryName)
        if (seven.isNotEmpty()) return seven
      } catch (t: Throwable) {
        logger.warn(t) { "getEntryStream: 7z failed" }
      }
    }

    // 2) Fallback: libarchive
    if (hasLibarchive) {
      try {
        val result = Archive(path, setOf(ReadSupportCompression.ALL), setOf(ReadSupportFilter.NONE), setOf(ReadSupportFormat.RAR5)).use { rar ->
          generateSequence { rar.nextEntry }
            .firstOrNull { it.name == entryName }
            ?.let { rar.inputStream.use { it.readBytes() } }
            ?: ByteArray(0)
        }
        if (result.isNotEmpty()) {
          logger.info { "getEntryStream: libarchive read ${result.size} bytes" }
          return result
        }
      } catch (e: Exception) {
        logger.warn(e) { "getEntryStream: libarchive failed" }
      }
    }

    // 3) Last resort: junrar (no RAR5 support, kept for completeness)
    logger.info { "getEntryStream: using junrar fallback for '$entryName'" }
    return try {
      com.github.junrar.Archive(path.toFile()).use { rar ->
        val header = rar.fileHeaders.find { it.fileName == entryName }
        if (header != null) {
          val data = rar.getInputStream(header).use { it.readBytes() }
          logger.info { "getEntryStream: junrar read ${data.size} bytes" }
          data
        } else {
          logger.warn { "getEntryStream: entry '$entryName' not found by junrar" }
          ByteArray(0)
        }
      }
    } catch (e: Exception) {
      logger.error(e) { "getEntryStream: junrar fallback also failed" }
      ByteArray(0)
    }
  }
}
