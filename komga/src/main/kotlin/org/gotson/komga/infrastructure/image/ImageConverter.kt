package org.gotson.komga.infrastructure.image

import io.github.oshai.kotlinlogging.KotlinLogging
import net.coobird.thumbnailator.Thumbnails
import org.gotson.komga.infrastructure.mediacontainer.ContentDetector
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO
import javax.imageio.spi.IIORegistry
import javax.imageio.spi.ImageReaderSpi
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger {}

private const val WEBP_NIGHT_MONKEYS = "com.github.gotson.nightmonkeys.webp.imageio.plugins.WebpImageReaderSpi"

@Service
class ImageConverter(
  private val imageAnalyzer: ImageAnalyzer,
  private val contentDetector: ContentDetector,
) {
  val supportedReadFormats by lazy { ImageIO.getReaderFormatNames().toList() }
  val supportedReadMediaTypes by lazy { ImageIO.getReaderMIMETypes().toList() }
  val supportedWriteFormats by lazy { ImageIO.getWriterFormatNames().toList() }
  val supportedWriteMediaTypes by lazy { ImageIO.getWriterMIMETypes().toList() }

  init {
    chooseWebpReader()
    logger.info { "Supported read formats: $supportedReadFormats" }
    logger.info { "Supported read mediaTypes: $supportedReadMediaTypes" }
    logger.info { "Supported write formats: $supportedWriteFormats" }
    logger.info { "Supported write mediaTypes: $supportedWriteMediaTypes" }
  }

  private fun chooseWebpReader() {
    val providers =
      IIORegistry
        .getDefaultInstance()
        .getServiceProviders(
          ImageReaderSpi::class.java,
          { it is ImageReaderSpi && it.mimeTypes.contains("image/webp") },
          false,
        ).asSequence()
        .toList()

    if (providers.size > 1) {
      logger.debug { "WebP reader providers: ${providers.map { it.javaClass.canonicalName }}" }
      providers.firstOrNull { it.javaClass.canonicalName == WEBP_NIGHT_MONKEYS }?.let { nightMonkeys ->
        (providers - nightMonkeys).forEach {
          logger.debug { "Deregister provider: ${it.javaClass.canonicalName}" }
          IIORegistry.getDefaultInstance().deregisterServiceProvider(it)
        }
      }
    }
  }

  private val supportsTransparency = listOf("png")

  fun canConvertMediaType(
    from: String,
    to: String,
  ) = supportedReadMediaTypes.contains(from) && supportedWriteMediaTypes.contains(to)

  fun convertImage(
    imageBytes: ByteArray,
    format: String,
  ): ByteArray =
    ByteArrayOutputStream().use { baos ->
      val image = ImageIO.read(imageBytes.inputStream())

      val result =
        if (!supportsTransparency.contains(format) && containsAlphaChannel(image)) {
          if (containsTransparency(image))
            logger.info { "Image contains alpha channel but is not opaque, visual artifacts may appear" }
          else
            logger.info { "Image contains alpha channel but is opaque, conversion should not generate any visual artifacts" }
          BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB).also {
            it.createGraphics().drawImage(image, 0, 0, Color.WHITE, null)
          }
        } else {
          image
        }

      ImageIO.write(result, format, baos)

      baos.toByteArray()
    }

  fun resizeImageToByteArray(
    imageBytes: ByteArray,
    format: ImageType,
    size: Int,
  ): ByteArray {
    val builder = resizeImageBuilder(imageBytes, format, size) ?: return imageBytes

    return ByteArrayOutputStream().use {
      builder.toOutputStream(it)
      it.toByteArray()
    }
  }

  fun resizeImageToBufferedImage(
    imageBytes: ByteArray,
    format: ImageType,
    size: Int,
  ): BufferedImage {
    val builder = resizeImageBuilder(imageBytes, format, size) ?: return ImageIO.read(imageBytes.inputStream())

    return builder.asBufferedImage()
  }

  private fun resizeImageBuilder(
    imageBytes: ByteArray,
    format: ImageType,
    size: Int,
  ): Thumbnails.Builder<out InputStream>? {
    val longestEdge =
      imageAnalyzer.getDimension(imageBytes.inputStream())?.let {
        val mediaType = contentDetector.detectMediaType(imageBytes.inputStream())
        val longestEdge = max(it.height, it.width)
        // don't resize if source and target format is the same, and source is smaller than desired
        if (mediaType == format.mediaType && longestEdge <= size) return null
        longestEdge
      }

    // prevent upscaling
    val resizeTo = if (longestEdge != null) min(longestEdge, size) else size

    return Thumbnails
      .of(imageBytes.inputStream())
      .size(resizeTo, resizeTo)
      .imageType(BufferedImage.TYPE_INT_ARGB)
      .outputFormat(format.imageIOFormat)
  }

  /**
   * Checks if an image is a double-page spread (width > height).
   */
  fun isDoublePage(imageBytes: ByteArray): Boolean {
    val dim = imageAnalyzer.getDimension(imageBytes.inputStream()) ?: return false
    return dim.width > dim.height
  }

  /**
   * Crops a double-page spread image by detecting the seam line and keeping one half.
   *
   * The seam is detected by analyzing the center region of a downscaled version of the image,
   * scoring each column by brightness gradient, edge strength, and content variance change.
   *
   * @param imageBytes the source image bytes
   * @param keepLeft if true, keeps the left side of the seam; if false, keeps the right side
   * @return the cropped image as a byte array (PNG format)
   */
  fun cropDoublePage(
    imageBytes: ByteArray,
    keepLeft: Boolean,
  ): ByteArray {
    val original = ImageIO.read(imageBytes.inputStream())

    // downscale for analysis (height = 300px)
    val analysisHeight = 300
    val scale = analysisHeight.toDouble() / original.height
    val analysisWidth = (original.width * scale).roundToInt()
    val analysisImage =
      BufferedImage(analysisWidth, analysisHeight, BufferedImage.TYPE_INT_RGB).also { scaled ->
        val g = scaled.createGraphics()
        g.drawImage(original, 0, 0, analysisWidth, analysisHeight, null)
        g.dispose()
      }

    // detect seam in the center region (35% - 65% of width)
    val searchStart = (analysisWidth * 0.35).roundToInt()
    val searchEnd = (analysisWidth * 0.65).roundToInt()
    val seamColumn = findSeamColumn(analysisImage, searchStart, searchEnd)

    // map back to original image coordinates
    val originalSeamX = (seamColumn / scale).roundToInt().coerceIn(1, original.width - 1)

    // crop
    val cropX = if (keepLeft) 0 else originalSeamX
    val cropWidth = if (keepLeft) originalSeamX else (original.width - originalSeamX)
    val cropped = original.getSubimage(cropX, 0, cropWidth, original.height)

    return ByteArrayOutputStream().use { baos ->
      ImageIO.write(cropped, "png", baos)
      baos.toByteArray()
    }
  }

  /**
   * Finds the best seam column in the given search range by combining:
   * 1. Horizontal gradient strength (edge detection)
   * 2. Brightness transition (difference in mean brightness left vs right of column)
   * 3. Content variance change (difference in variance left vs right of column)
   *
   * Falls back to the center of the search range if no clear seam is detected.
   */
  private fun findSeamColumn(
    image: BufferedImage,
    searchStart: Int,
    searchEnd: Int,
  ): Int {
    val width = image.width
    val height = image.height

    // precompute grayscale column averages and per-column edge strengths
    val columnBrightness = DoubleArray(width)
    val columnEdgeStrength = DoubleArray(width)

    for (x in 0 until width) {
      var brightnessSum = 0.0
      var edgeSum = 0.0
      for (y in 0 until height) {
        val gray = getGray(image, x, y)
        brightnessSum += gray
        if (x in 1 until width - 1) {
          val left = getGray(image, x - 1, y)
          val right = getGray(image, x + 1, y)
          edgeSum += abs(right - left)
        }
      }
      columnBrightness[x] = brightnessSum / height
      columnEdgeStrength[x] = edgeSum / height
    }

    // smooth column brightness with a window to reduce noise
    val smoothed = smoothArray(columnBrightness, 5)

    // score each candidate column
    var bestScore = Double.MIN_VALUE
    var bestColumn = (searchStart + searchEnd) / 2

    // precompute overall stats for normalization
    val maxEdge = columnEdgeStrength.slice(searchStart..searchEnd).maxOrNull() ?: 1.0

    for (x in searchStart..searchEnd) {
      // 1. brightness transition: absolute difference in mean brightness left vs right
      val leftMean = smoothed.slice(max(0, x - 30) until x).average()
      val rightMean = smoothed.slice(x + 1..min(width - 1, x + 30)).average()
      val brightnessTransition = abs(rightMean - leftMean)

      // 2. edge strength at this column (normalized)
      val edgeScore = columnEdgeStrength[x] / maxEdge.coerceAtLeast(1.0)

      // 3. variance change: difference in pixel variance between left and right chunks
      val leftVariance = computeVariance(smoothed, max(0, x - 30), x)
      val rightVariance = computeVariance(smoothed, x + 1, min(width - 1, x + 30))
      val varianceChange = abs(leftVariance - rightVariance)

      // combined score (weighted)
      val score = brightnessTransition * 2.0 + edgeScore * 1.0 + varianceChange * 1.5

      if (score > bestScore) {
        bestScore = score
        bestColumn = x
      }
    }

    logger.debug { "Seam detected at column $bestColumn (score: $bestScore) in range [$searchStart, $searchEnd]" }
    return bestColumn
  }

  private fun getGray(
    image: BufferedImage,
    x: Int,
    y: Int,
  ): Double {
    val rgb = image.getRGB(x, y)
    val r = (rgb shr 16) and 0xFF
    val g = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF
    return 0.299 * r + 0.587 * g + 0.114 * b
  }

  private fun smoothArray(
    arr: DoubleArray,
    windowSize: Int,
  ): DoubleArray {
    val result = DoubleArray(arr.size)
    val half = windowSize / 2
    for (i in arr.indices) {
      val from = max(0, i - half)
      val to = min(arr.size - 1, i + half)
      var sum = 0.0
      for (j in from..to) sum += arr[j]
      result[i] = sum / (to - from + 1)
    }
    return result
  }

  private fun computeVariance(
    arr: DoubleArray,
    from: Int,
    to: Int,
  ): Double {
    if (from >= to) return 0.0
    val slice = arr.slice(from..to)
    val mean = slice.average()
    return slice.sumOf { (it - mean) * (it - mean) } / slice.size
  }

  /**
   * Analyzes a double-page image to determine which side is the front cover.
   * Compares left and right halves by visual complexity (edge density, color richness, brightness variance).
   * The half with higher complexity is more likely to be the front cover.
   *
   * @return true if the left side is the front cover, false if the right side is.
   */
  fun detectCoverSide(imageBytes: ByteArray): Boolean {
    val original = ImageIO.read(imageBytes.inputStream())

    // downscale for analysis
    val analysisHeight = 300
    val scale = analysisHeight.toDouble() / original.height
    val analysisWidth = (original.width * scale).roundToInt()
    val img =
      BufferedImage(analysisWidth, analysisHeight, BufferedImage.TYPE_INT_RGB).also { scaled ->
        val g = scaled.createGraphics()
        g.drawImage(original, 0, 0, analysisWidth, analysisHeight, null)
        g.dispose()
      }

    // find the seam
    val searchStart = (analysisWidth * 0.35).roundToInt()
    val searchEnd = (analysisWidth * 0.65).roundToInt()
    val seamColumn = findSeamColumn(img, searchStart, searchEnd)

    val leftScore = computeHalfComplexity(img, 0, seamColumn)
    val rightScore = computeHalfComplexity(img, seamColumn, analysisWidth)

    logger.debug { "Cover side detection: leftScore=$leftScore, rightScore=$rightScore, seam=$seamColumn" }
    // the side with higher complexity is the cover; default to left on tie
    return leftScore >= rightScore
  }

  /**
   * Computes a visual complexity score for a vertical strip of the image.
   * Combines edge density, color variance, and brightness variance.
   */
  private fun computeHalfComplexity(
    image: BufferedImage,
    xStart: Int,
    xEnd: Int,
  ): Double {
    if (xEnd <= xStart) return 0.0
    val height = image.height
    val width = xEnd - xStart
    val totalPixels = width.toLong() * height

    var brightnessSum = 0.0
    var brightnessSqSum = 0.0
    var edgeSum = 0.0
    var rSum = 0.0; var gSum = 0.0; var bSum = 0.0
    var rSqSum = 0.0; var gSqSum = 0.0; var bSqSum = 0.0

    for (x in xStart until xEnd) {
      for (y in 0 until height) {
        val rgb = image.getRGB(x, y)
        val r = ((rgb shr 16) and 0xFF).toDouble()
        val g = ((rgb shr 8) and 0xFF).toDouble()
        val b = (rgb and 0xFF).toDouble()
        val gray = 0.299 * r + 0.587 * g + 0.114 * b

        brightnessSum += gray
        brightnessSqSum += gray * gray
        rSum += r; gSum += g; bSum += b
        rSqSum += r * r; gSqSum += g * g; bSqSum += b * b

        // horizontal edge (Sobel-like)
        if (x > xStart && x < xEnd - 1) {
          val left = getGray(image, x - 1, y)
          val right = getGray(image, x + 1, y)
          edgeSum += abs(right - left)
        }
      }
    }

    // brightness variance
    val brightnessVar = brightnessSqSum / totalPixels - (brightnessSum / totalPixels).let { it * it }

    // color variance (sum of per-channel variance) — higher means more colorful
    val rVar = rSqSum / totalPixels - (rSum / totalPixels).let { it * it }
    val gVar = gSqSum / totalPixels - (gSum / totalPixels).let { it * it }
    val bVar = bSqSum / totalPixels - (bSum / totalPixels).let { it * it }
    val colorVar = rVar + gVar + bVar

    // edge density (normalized by area)
    val edgeDensity = edgeSum / totalPixels

    // combined score: edge density (most important), color variance, brightness variance
    return edgeDensity * 3.0 + colorVar * 0.01 + brightnessVar * 0.01
  }

  /**
   * Detects and crops the cover region from a full wraparound cover scan.
   *
   * Full cover scans typically include back cover, spine, front cover, and flaps.
   * This method detects the left and right boundaries of the actual front cover by finding
   * strong continuous vertical edges (the cover frame sides). Image height is kept as-is.
   *
   * Algorithm:
   * 1. Downscale for analysis
   * 2. For each column, count how many rows have a strong horizontal gradient (vertical edge continuity)
   * 3. Find the strongest left/right boundaries
   * 4. Crop horizontally, keeping full height
   *
   * @param imageBytes the source image bytes
   * @return the cropped cover region as a byte array (PNG format), or null if no significant border was detected
   */
  fun cropCoverRegion(imageBytes: ByteArray): ByteArray? {
    val original = ImageIO.read(imageBytes.inputStream())

    // downscale for analysis (height = 400px)
    val analysisHeight = 400
    val scale = analysisHeight.toDouble() / original.height
    val analysisWidth = (original.width * scale).roundToInt()
    val img =
      BufferedImage(analysisWidth, analysisHeight, BufferedImage.TYPE_INT_RGB).also { scaled ->
        val g = scaled.createGraphics()
        g.drawImage(original, 0, 0, analysisWidth, analysisHeight, null)
        g.dispose()
      }

    // --- Compute horizontal gradient (detects vertical lines) ---
    val hEdge = Array(analysisWidth) { DoubleArray(analysisHeight) }
    for (x in 1 until analysisWidth - 1) {
      for (y in 1 until analysisHeight - 1) {
        hEdge[x][y] = abs(getGray(img, x + 1, y) - getGray(img, x - 1, y))
      }
    }

    // --- For each column, compute "vertical edge continuity": fraction of rows with strong gradient ---
    val edgeThreshold = 30.0
    val columnContinuity = DoubleArray(analysisWidth)
    for (x in 1 until analysisWidth - 1) {
      var count = 0
      for (y in 1 until analysisHeight - 1) {
        if (hEdge[x][y] > edgeThreshold) count++
      }
      columnContinuity[x] = count.toDouble() / analysisHeight
    }
    val smoothed = smoothArray(columnContinuity, 3)

    // --- Search for left and right boundaries ---
    val minContinuity = 0.15
    val (leftBound, leftVal) = findBoundaryPeak(smoothed, (analysisWidth * 0.05).roundToInt(), (analysisWidth * 0.45).roundToInt())
    val (rightBound, rightVal) = findBoundaryPeak(smoothed, (analysisWidth * 0.55).roundToInt(), (analysisWidth * 0.95).roundToInt())

    logger.debug { "Cover region peaks: left=$leftBound(${"%.3f".format(leftVal)}), right=$rightBound(${"%.3f".format(rightVal)}) (min=$minContinuity)" }

    // Need at least one strong vertical boundary
    if (leftVal < minContinuity && rightVal < minContinuity) {
      logger.debug { "No vertical boundary has continuity >= $minContinuity, no cover frame detected" }
      return null
    }

    // Weak boundary falls back to image edge
    val effectiveLeft = if (leftVal >= minContinuity) leftBound else 0
    val effectiveRight = if (rightVal >= minContinuity) rightBound else analysisWidth

    // map back to original coordinates, keep full height
    val origLeft = (effectiveLeft / scale).roundToInt().coerceIn(0, original.width - 1)
    val origRight = (effectiveRight / scale).roundToInt().coerceIn(origLeft + 1, original.width)
    val cropWidth = origRight - origLeft

    // check if cropped width is significantly smaller than original (< 85%)
    val widthRatio = cropWidth.toDouble() / original.width
    if (widthRatio >= 0.85) {
      logger.debug { "Cover region width ratio $widthRatio >= 0.85, no significant border detected" }
      return null
    }

    logger.info { "Cropping cover region: x=$origLeft..${origRight}, width=${cropWidth} from ${original.width} (width ratio: %.2f)".format(widthRatio) }

    val cropped = original.getSubimage(origLeft, 0, cropWidth, original.height)
    return ByteArrayOutputStream().use { baos ->
      ImageIO.write(cropped, "png", baos)
      baos.toByteArray()
    }
  }

  /**
   * Finds the peak (maximum value) position in the given range of an array.
   * Used to locate boundary edges in continuity projections.
   *
   * @return Pair of (peak index, peak value)
   */
  private fun findBoundaryPeak(
    arr: DoubleArray,
    searchStart: Int,
    searchEnd: Int,
  ): Pair<Int, Double> {
    var bestIdx = (searchStart + searchEnd) / 2
    var bestVal = 0.0
    for (i in searchStart..min(searchEnd, arr.size - 1)) {
      if (arr[i] > bestVal) {
        bestVal = arr[i]
        bestIdx = i
      }
    }
    return Pair(bestIdx, bestVal)
  }

  private fun containsAlphaChannel(image: BufferedImage): Boolean = image.colorModel.hasAlpha()

  private fun containsTransparency(image: BufferedImage): Boolean {
    for (x in 0 until image.width) {
      for (y in 0 until image.height) {
        val pixel = image.getRGB(x, y)
        if (pixel shr 24 == 0x00) return true
      }
    }
    return false
  }
}
