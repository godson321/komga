package org.gotson.komga.infrastructure.image

import org.apache.tika.config.TikaConfig
import org.gotson.komga.infrastructure.mediacontainer.ContentDetector
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Quick standalone test for the double-page cover cropping algorithm.
 * Run: gradlew :komga:test --tests "org.gotson.komga.infrastructure.image.CropDoublePageTest"
 */
class CropDoublePageTest {

  private val converter = ImageConverter(ImageAnalyzer(), ContentDetector(TikaConfig.getDefaultConfig()))

  @org.junit.jupiter.api.Test
  fun `synthetic double-page image should be detected and cropped`() {
    // Create a synthetic 800x600 double-page image (width > height)
    // Left half: dark blue, Right half: light gray, with a 2px black seam in the middle
    val w = 800
    val h = 600
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()

    // Left page - dark blue
    g.color = Color(30, 40, 80)
    g.fillRect(0, 0, w / 2 - 1, h)

    // Seam - black vertical line
    g.color = Color.BLACK
    g.fillRect(w / 2 - 1, 0, 2, h)

    // Right page - light gray
    g.color = Color(200, 200, 210)
    g.fillRect(w / 2 + 1, 0, w / 2 - 1, h)

    g.dispose()

    val bytes = toBytes(img, "png")

    // Should be detected as double page
    val isDouble = converter.isDoublePage(bytes)
    println("Synthetic image (${w}x${h}): isDoublePage = $isDouble")
    assert(isDouble) { "Expected synthetic image to be detected as double-page" }

    // Crop keeping left
    val croppedLeft = converter.cropDoublePage(bytes, keepLeft = true)
    val imgLeft = ImageIO.read(ByteArrayInputStream(croppedLeft))
    println("  Cropped (keepLeft=true):  ${imgLeft.width}x${imgLeft.height}")
    assert(imgLeft.width < w) { "Cropped width should be less than original" }
    assert(imgLeft.height == h) { "Cropped height should equal original" }

    // Crop keeping right
    val croppedRight = converter.cropDoublePage(bytes, keepLeft = false)
    val imgRight = ImageIO.read(ByteArrayInputStream(croppedRight))
    println("  Cropped (keepLeft=false): ${imgRight.width}x${imgRight.height}")
    assert(imgRight.width < w) { "Cropped width should be less than original" }
  }

  @org.junit.jupiter.api.Test
  fun `single-page tall image should NOT be detected as double-page`() {
    // Create a tall single-page image (height > width)
    val w = 400
    val h = 600
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()
    g.color = Color(100, 120, 140)
    g.fillRect(0, 0, w, h)
    g.dispose()

    val bytes = toBytes(img, "png")
    val isDouble = converter.isDoublePage(bytes)
    println("Single-page image (${w}x${h}): isDoublePage = $isDouble")
    assert(!isDouble) { "Expected single-page image to NOT be detected as double-page" }
  }

  @org.junit.jupiter.api.Test
  fun `wraparound cover with borders should be detected by cropCoverRegion`() {
    // Create a synthetic 1500x600 wraparound cover scan:
    // [back cover 300px | spine 50px | FRONT COVER 500px | flap/ads 350px | margin 300px]
    // The front cover area is roughly 33% of total width, typical for real scans.
    val w = 1500
    val h = 600
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()

    // Back cover area (0-300): dark gray with some text-like noise
    g.color = Color(60, 60, 70)
    g.fillRect(0, 0, 300, h)

    // Spine (300-350): black narrow strip
    g.color = Color(20, 20, 20)
    g.fillRect(300, 0, 50, h)

    // Front cover (350-850): vibrant art
    g.color = Color(200, 50, 30)
    g.fillRect(350, 0, 500, h)
    // add some detail to the cover
    g.color = Color(30, 80, 200)
    g.fillRect(400, 100, 200, 300)

    // Flap/ads area (850-1200): lighter content
    g.color = Color(230, 220, 200)
    g.fillRect(850, 0, 350, h)

    // Right margin (1200-1500): similar to back cover
    g.color = Color(180, 170, 160)
    g.fillRect(1200, 0, 300, h)

    g.dispose()

    val bytes = toBytes(img, "png")

    // cropCoverRegion should detect the left and right boundaries
    val cropped = converter.cropCoverRegion(bytes)
    assert(cropped != null) { "Expected cropCoverRegion to detect borders" }

    val croppedImg = ImageIO.read(ByteArrayInputStream(cropped!!))
    println("Wraparound cover (${w}x${h}): cropped to ${croppedImg.width}x${croppedImg.height}")

    // Cropped width should be significantly less, height stays the same
    assert(croppedImg.width < w * 0.85) { "Cropped width ${croppedImg.width} should be significantly less than original $w" }
    assert(croppedImg.height == h) { "Cropped height ${croppedImg.height} should equal original $h" }
  }

  @org.junit.jupiter.api.Test
  fun `simple double-page without borders should return null from cropCoverRegion`() {
    // Create a simple 800x600 double-page image (no border)
    val w = 800
    val h = 600
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()
    g.color = Color(30, 40, 80)
    g.fillRect(0, 0, w / 2, h)
    g.color = Color(200, 200, 210)
    g.fillRect(w / 2, 0, w / 2, h)
    g.dispose()

    val bytes = toBytes(img, "png")

    // Should return null (no significant border)
    val cropped = converter.cropCoverRegion(bytes)
    println("Simple double-page (${w}x${h}): cropCoverRegion returned ${if (cropped == null) "null" else "cropped"}")
    assert(cropped == null) { "Expected cropCoverRegion to return null for a simple double-page image" }
  }

  @org.junit.jupiter.api.Test
  fun `test with real images if provided via system property`() {
    // Run with: gradlew :komga:test --tests "...CropDoublePageTest.test with real*" -DcropTestImages="path1;path2"
    val paths = System.getProperty("cropTestImages")?.split(";")?.filter { it.isNotBlank() }
    if (paths.isNullOrEmpty()) {
      println("No real images provided. Set -DcropTestImages=\"path1;path2\" to test with real images.")
      return
    }

    for (path in paths) {
      val file = File(path.trim())
      if (!file.exists()) {
        println("File not found: $path")
        continue
      }

      println("\n=== Testing: ${file.name} ===")
      val bytes = file.readBytes()
      val img = ImageIO.read(ByteArrayInputStream(bytes))
      println("  Size: ${img.width}x${img.height}, aspect: %.2f".format(img.width.toDouble() / img.height))

      // Test cover region detection
      val coverRegion = converter.cropCoverRegion(bytes)
      if (coverRegion != null) {
        val crImg = ImageIO.read(ByteArrayInputStream(coverRegion))
        val outCR = File(file.parent, "${file.nameWithoutExtension}_cover_region.${file.extension}")
        ImageIO.write(crImg, file.extension.ifBlank { "png" }, outCR)
        println("  Cover region: ${crImg.width}x${crImg.height} -> saved to ${outCR.absolutePath}")
      } else {
        println("  Cover region: no significant border detected")
      }

      val testBytes = coverRegion ?: bytes
      val isDouble = converter.isDoublePage(testBytes)
      println("  isDoublePage: $isDouble")

      if (isDouble) {
        // Crop left half
        val croppedLeft = converter.cropDoublePage(testBytes, keepLeft = true)
        val imgL = ImageIO.read(ByteArrayInputStream(croppedLeft))
        val outLeft = File(file.parent, "${file.nameWithoutExtension}_crop_left.${file.extension}")
        ImageIO.write(imgL, file.extension.ifBlank { "png" }, outLeft)
        println("  Cropped LEFT:  ${imgL.width}x${imgL.height} -> saved to ${outLeft.absolutePath}")

        // Crop right half
        val croppedRight = converter.cropDoublePage(testBytes, keepLeft = false)
        val imgR = ImageIO.read(ByteArrayInputStream(croppedRight))
        val outRight = File(file.parent, "${file.nameWithoutExtension}_crop_right.${file.extension}")
        ImageIO.write(imgR, file.extension.ifBlank { "png" }, outRight)
        println("  Cropped RIGHT: ${imgR.width}x${imgR.height} -> saved to ${outRight.absolutePath}")
      }
    }
  }

  private fun toBytes(img: BufferedImage, format: String): ByteArray {
    val baos = ByteArrayOutputStream()
    ImageIO.write(img, format, baos)
    return baos.toByteArray()
  }
}
