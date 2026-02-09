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

      val isDouble = converter.isDoublePage(bytes)
      println("  isDoublePage: $isDouble")

      if (isDouble) {
        // Crop left half
        val croppedLeft = converter.cropDoublePage(bytes, keepLeft = true)
        val imgL = ImageIO.read(ByteArrayInputStream(croppedLeft))
        val outLeft = File(file.parent, "${file.nameWithoutExtension}_crop_left.${file.extension}")
        ImageIO.write(imgL, file.extension.ifBlank { "png" }, outLeft)
        println("  Cropped LEFT:  ${imgL.width}x${imgL.height} -> saved to ${outLeft.absolutePath}")

        // Crop right half
        val croppedRight = converter.cropDoublePage(bytes, keepLeft = false)
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
