package org.gotson.komga.infrastructure.mediacontainer.divina

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.nio.file.Files
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

object SevenZipHelper {
  private val candidates: List<String> by lazy {
    val list = mutableListOf<String>()

    // 1) Extract embedded 7z binary from classpath resources
    try {
      val embedded = extractEmbedded7z()
      if (embedded != null) {
        list.add(embedded)
        logger.info { "Using embedded 7z: $embedded" }
      }
    } catch (e: Throwable) {
      logger.debug(e) { "Could not extract embedded 7z" }
    }

    // 2) Look for 7z next to the running jar / application directory (e.g. komga-tray)
    try {
      val codeSource = SevenZipHelper::class.java.protectionDomain?.codeSource?.location
      if (codeSource != null) {
        val appDir = File(codeSource.toURI()).parentFile
        for (candidate in listOf(
          File(appDir, "7z.exe"),
          File(appDir, "7zz"),
          File(appDir, "lib/windows/x64/7z.exe"),
          File(appDir.parentFile, "lib/windows/x64/7z.exe"),
        )) {
          if (candidate.exists()) {
            list.add(candidate.absolutePath)
            logger.info { "Found bundled 7z at: ${candidate.absolutePath}" }
          }
        }
      }
    } catch (e: Throwable) {
      logger.debug(e) { "Could not resolve bundled 7z path" }
    }

    // 3) System-installed paths
    list.addAll(listOf(
      "7z", "7za", "7zz",
      "C:\\Program Files\\7-Zip\\7z.exe",
      "C:\\Program Files\\7-Zip\\7za.exe",
      "C:\\Program Files (x86)\\7-Zip\\7z.exe",
      "C:\\Program Files (x86)\\7-Zip\\7za.exe",
    ))
    list
  }

  /**
   * Detect OS/arch and extract the matching 7z binary from classpath to a temp directory.
   * Resources layout: /7z/windows-x64/7z.exe (+7z.dll), /7z/linux-x64/7zz, /7z/linux-arm64/7zz
   */
  private fun extractEmbedded7z(): String? {
    val osName = System.getProperty("os.name", "").lowercase()
    val arch = System.getProperty("os.arch", "").lowercase()

    val (resourceDir, binaryName, extraFiles) = when {
      osName.contains("win") -> Triple("7z/windows-x64", "7z.exe", listOf("7z.dll"))
      osName.contains("linux") || osName.contains("nux") -> {
        val dir = if (arch.contains("aarch64") || arch.contains("arm64")) "7z/linux-arm64" else "7z/linux-x64"
        Triple(dir, "7zz", emptyList())
      }
      else -> return null  // macOS etc. - not bundled
    }

    val tmpDir = Files.createTempDirectory("komga-7z").toFile()
    tmpDir.deleteOnExit()

    // Extract main binary
    val mainStream = SevenZipHelper::class.java.classLoader.getResourceAsStream("$resourceDir/$binaryName")
      ?: return null
    val mainFile = File(tmpDir, binaryName)
    mainStream.use { input -> mainFile.outputStream().use { output -> input.copyTo(output) } }
    mainFile.setExecutable(true)
    mainFile.deleteOnExit()

    // Extract extra files (e.g. 7z.dll for Windows)
    for (extra in extraFiles) {
      val stream = SevenZipHelper::class.java.classLoader.getResourceAsStream("$resourceDir/$extra") ?: continue
      val file = File(tmpDir, extra)
      stream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
      file.deleteOnExit()
    }

    return mainFile.absolutePath
  }

  data class ArchiveEntry(val name: String, val size: Long, val isDirectory: Boolean)

  fun isAvailable(): Boolean =
    candidates.any { exe ->
      try {
        val p = ProcessBuilder(exe).redirectErrorStream(true).start()
        p.inputStream.readBytes()
        p.waitFor(5, TimeUnit.SECONDS)
        true
      } catch (_: Throwable) { false }
    }

  fun listEntries(path: Path): List<ArchiveEntry> {
    for (exe in candidates) {
      try {
        val pb = ProcessBuilder(exe, "l", "-slt", path.toString())
        pb.redirectErrorStream(false)
        val p = pb.start()

        val stderr = Thread {
          try {
            p.errorStream.bufferedReader().useLines { lines ->
              lines.forEach { line -> logger.debug { "7z list: $line" } }
            }
          } catch (_: Throwable) {}
        }
        stderr.isDaemon = true
        stderr.start()

        val output = p.inputStream.bufferedReader().readText()
        val finished = p.waitFor(60, TimeUnit.SECONDS)
        if (!finished) {
          p.destroyForcibly()
          continue
        }
        if (p.exitValue() != 0) continue

        // Parse entries after the "----------" separator
        val entries = mutableListOf<ArchiveEntry>()
        val blocks = output.split("----------")
        if (blocks.size < 2) continue
        val entryBlock = blocks.last()

        // Each entry is separated by blank lines, fields are "Key = Value"
        var currentPath: String? = null
        var currentSize: Long = 0
        var currentIsDir = false

        for (line in entryBlock.lines()) {
          val trimmed = line.trim()
          if (trimmed.isEmpty()) {
            if (currentPath != null && !currentIsDir) {
              entries.add(ArchiveEntry(currentPath, currentSize, false))
            }
            currentPath = null
            currentSize = 0
            currentIsDir = false
            continue
          }
          val eqIdx = trimmed.indexOf(" = ")
          if (eqIdx < 0) continue
          val key = trimmed.substring(0, eqIdx)
          val value = trimmed.substring(eqIdx + 3)
          when (key) {
            "Path" -> currentPath = value
            "Size" -> currentSize = value.toLongOrNull() ?: 0
            "Folder" -> currentIsDir = value == "+"
          }
        }
        // last entry if no trailing blank line
        if (currentPath != null && !currentIsDir) {
          entries.add(ArchiveEntry(currentPath, currentSize, false))
        }

        logger.info { "7z listed ${entries.size} entries from '$path'" }
        return entries
      } catch (t: Throwable) {
        logger.debug(t) { "7z candidate '$exe' failed to list" }
      }
    }
    return emptyList()
  }

  fun extractEntry(path: Path, entryName: String): ByteArray {
    for (exe in candidates) {
      try {
        val pb = ProcessBuilder(exe, "x", "-y", "-so", path.toString(), entryName)
        pb.redirectErrorStream(false)
        val p = pb.start()

        val stderr = Thread {
          try {
            p.errorStream.bufferedReader().useLines { lines ->
              lines.forEach { line -> logger.debug { "7z: $line" } }
            }
          } catch (_: Throwable) {}
        }
        stderr.isDaemon = true
        stderr.start()

        val out = ByteArrayOutputStream()
        p.inputStream.use { it.copyTo(out) }
        val finished = p.waitFor(60, TimeUnit.SECONDS)
        if (!finished) {
          p.destroyForcibly()
          logger.warn { "7z '$exe' timed out reading '$entryName' from '$path'" }
          continue
        }
        val exit = p.exitValue()
        if (exit == 0 && out.size() > 0) {
          logger.info { "7z extracted ${out.size()} bytes for '$entryName'" }
          return out.toByteArray()
        } else {
          logger.warn { "7z '$exe' exit=$exit produced ${out.size()} bytes for '$entryName'" }
        }
      } catch (t: Throwable) {
        logger.debug(t) { "7z candidate '$exe' failed" }
      }
    }
    return ByteArray(0)
  }
}
