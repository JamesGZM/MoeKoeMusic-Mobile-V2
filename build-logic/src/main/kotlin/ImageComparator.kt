import java.awt.AlphaComposite
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.util.Properties
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max

internal data class AnchorResult(
    val name: String,
    val expectedX: Double,
    val expectedY: Double,
    val actualX: Double,
    val actualY: Double,
    val error: Double,
    val tolerance: Double,
)

internal data class UiEvidenceResult(
    val contractId: String,
    val meanError: Double,
    val changedRatio: Double,
    val anchorResults: List<AnchorResult>,
    val cumulativeDrift: Double,
    val status: UiEvidenceStatus,
    val sources: UiEvidenceSources,
)

internal enum class UiEvidenceStatus {
    PASS,
    FAIL,
    APPROVED_DEBT,
}

internal data class UiEvidenceSources(
    val contractSha256: String,
    val designSha256: String,
    val renderedSha256: String,
    val probeRenderedSha256: String?,
)

internal object ImageComparator {
    fun generate(
        rootDir: File,
        contract: UiContract,
        outputDir: File,
    ): UiEvidenceResult {
        val properties = contract.properties
        val design = readImage(rootDir, contract, "design.path")
        val rendered = readImage(rootDir, contract, "screenshot.rendered")
        val designCrop = crop(design, parseRect(properties.getProperty("design.crop")), contract.id)
        val renderRect = parseRect(properties.getProperty("render.crop"))
        val renderCrop = crop(rendered, renderRect, contract.id)
        val normalizedRendered = resize(renderCrop, designCrop.width, designCrop.height)
        val masks = parseMasks(properties)
        val deltaThreshold = properties.getProperty("pixel.deltaThreshold", "24").toInt()
        val (meanError, changedRatio) = metrics(designCrop, normalizedRendered, masks, deltaThreshold)

        outputDir.mkdirs()
        ImageIO.write(designCrop, "png", File(outputDir, "normalized-design.png"))
        ImageIO.write(normalizedRendered, "png", File(outputDir, "normalized-rendered.png"))
        ImageIO.write(sideBySide(designCrop, normalizedRendered), "png", File(outputDir, "side-by-side.png"))
        ImageIO.write(overlay(designCrop, normalizedRendered), "png", File(outputDir, "overlay.png"))
        ImageIO.write(diff(designCrop, normalizedRendered, masks), "png", File(outputDir, "diff.png"))

        val anchors =
            if (UiContractParser.hasProbe(properties)) {
                val probe = readImage(rootDir, contract, "probe.rendered")
                parseAnchors(properties).map { anchor -> measureAnchor(anchor, probe, renderRect, designCrop) }
            } else {
                emptyList()
            }
        val cumulativeDrift = cumulativeDrift(properties, anchors)
        val meetsContractThresholds =
            meanError <= properties.getProperty("pixel.meanError.max").toDouble() &&
                changedRatio <= properties.getProperty("pixel.changedRatio.max").toDouble() &&
                anchors.all { it.error <= it.tolerance } &&
                cumulativeDrift <= properties.getProperty("tolerance.cumulativeY").toDouble()
        val sources =
            UiEvidenceSources(
                contractSha256 = contract.file.sha256(),
                designSha256 = sourceHash(rootDir, contract, "design.path"),
                renderedSha256 = sourceHash(rootDir, contract, "screenshot.rendered"),
                probeRenderedSha256 = if (UiContractParser.hasProbe(properties)) sourceHash(rootDir, contract, "probe.rendered") else null,
            )
        val status =
            if (properties.getProperty("debt.status") == "active") {
                if (meetsDebtBaseline(
                        properties,
                        meanError,
                        changedRatio,
                        cumulativeDrift,
                        anchors,
                    )
                ) {
                    UiEvidenceStatus.APPROVED_DEBT
                } else {
                    UiEvidenceStatus.FAIL
                }
            } else if (meetsContractThresholds) {
                UiEvidenceStatus.PASS
            } else {
                UiEvidenceStatus.FAIL
            }
        return UiEvidenceResult(contract.id, meanError, changedRatio, anchors, cumulativeDrift, status, sources).also {
            writeResult(outputDir, it)
        }
    }

    private data class Rect(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    )

    private data class Anchor(
        val name: String,
        val expectedX: Double,
        val expectedY: Double,
        val tolerance: Double,
        val red: Int,
        val green: Int,
        val blue: Int,
    )

    private fun readImage(
        rootDir: File,
        contract: UiContract,
        key: String,
    ): BufferedImage {
        val file = UiContractParser.resolveRepositoryPath(rootDir, contract.id, contract.properties.getProperty(key))
        require(file.isFile) { "${contract.id}: 缺少 $key：${file.relativeTo(rootDir)}" }
        return requireNotNull(ImageIO.read(file)) { "${contract.id}: 无法读取图片 $key" }
    }

    private fun sourceHash(
        rootDir: File,
        contract: UiContract,
        key: String,
    ): String = UiContractParser.resolveRepositoryPath(rootDir, contract.id, contract.properties.getProperty(key)).sha256()

    private fun meetsDebtBaseline(
        properties: Properties,
        meanError: Double,
        changedRatio: Double,
        cumulativeDrift: Double,
        anchors: List<AnchorResult>,
    ): Boolean =
        meanError <= properties.getProperty("debt.baseline.meanError").toDouble() &&
            changedRatio <= properties.getProperty("debt.baseline.changedRatio").toDouble() &&
            cumulativeDrift <= properties.getProperty("debt.baseline.cumulativeDrift").toDouble() &&
            anchors.all { anchor ->
                anchor.error <= properties.getProperty("debt.baseline.anchor.${anchor.name}.error").toDouble()
            }

    private fun parseRect(value: String): Rect {
        val parts = value.split(',').map { it.trim().toInt() }
        return Rect(parts[0], parts[1], parts[2], parts[3])
    }

    private fun crop(
        image: BufferedImage,
        rect: Rect,
        contractId: String,
    ): BufferedImage {
        require(rect.x >= 0 && rect.y >= 0 && rect.x + rect.width <= image.width && rect.y + rect.height <= image.height) {
            "$contractId: crop 超出图片边界"
        }
        return image.getSubimage(rect.x, rect.y, rect.width, rect.height)
    }

    private fun resize(
        image: BufferedImage,
        width: Int,
        height: Int,
    ): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { target ->
            target.createGraphics().use { graphics ->
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                graphics.drawImage(image, 0, 0, width, height, null)
            }
        }

    private fun metrics(
        design: BufferedImage,
        rendered: BufferedImage,
        masks: List<Rect>,
        deltaThreshold: Int,
    ): Pair<Double, Double> {
        var compared = 0L
        var changed = 0L
        var error = 0.0
        for (y in 0 until design.height) {
            for (x in 0 until design.width) {
                if (masks.any { x >= it.x && x < it.x + it.width && y >= it.y && y < it.y + it.height }) continue
                val left = Color(design.getRGB(x, y), true)
                val right = Color(rendered.getRGB(x, y), true)
                val delta = max(abs(left.red - right.red), max(abs(left.green - right.green), abs(left.blue - right.blue)))
                error += (abs(left.red - right.red) + abs(left.green - right.green) + abs(left.blue - right.blue)) / (3.0 * 255.0)
                if (delta > deltaThreshold) changed++
                compared++
            }
        }
        require(compared > 0) { "遮罩覆盖了全部比较区域" }
        return error / compared to changed.toDouble() / compared
    }

    private fun parseMasks(properties: Properties): List<Rect> =
        properties.stringPropertyNames().filter { it.startsWith("mask.") }.sorted().map { key ->
            parseRect(properties.getProperty(key).substringBefore(';'))
        }

    private fun parseAnchors(properties: Properties): List<Anchor> =
        properties.stringPropertyNames().filter { it.startsWith("anchor.") }.sorted().map { key ->
            val values = properties.getProperty(key).split(',').map(String::trim)
            require(values.size == 6) { "$key 必须为 x,y,tolerance,r,g,b" }
            Anchor(
                name = key.substringAfter("anchor."),
                expectedX = values[0].toDouble(),
                expectedY = values[1].toDouble(),
                tolerance = values[2].toDouble(),
                red = values[3].toInt(),
                green = values[4].toInt(),
                blue = values[5].toInt(),
            )
        }

    private fun measureAnchor(
        anchor: Anchor,
        probe: BufferedImage,
        renderRect: Rect,
        design: BufferedImage,
    ): AnchorResult {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var count = 0
        for (y in 0 until probe.height) {
            for (x in 0 until probe.width) {
                val color = Color(probe.getRGB(x, y), true)
                if (color.red == anchor.red && color.green == anchor.green && color.blue == anchor.blue) {
                    minX = minOf(minX, x)
                    minY = minOf(minY, y)
                    maxX = maxOf(maxX, x)
                    count++
                }
            }
        }
        require(count >= 4) { "找不到锚点色标 ${anchor.name}" }
        val markerCenterX = (minX + maxX) / 2.0
        val actualX = (markerCenterX - renderRect.x) * design.width / renderRect.width
        val actualY = (minY - renderRect.y).toDouble() * design.height / renderRect.height
        val error = max(abs(actualX - anchor.expectedX), abs(actualY - anchor.expectedY))
        return AnchorResult(anchor.name, anchor.expectedX, anchor.expectedY, actualX, actualY, error, anchor.tolerance)
    }

    private fun cumulativeDrift(
        properties: Properties,
        anchors: List<AnchorResult>,
    ): Double {
        if (anchors.isEmpty()) return 0.0
        val names =
            properties
                .getProperty("drift.anchors", "")
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
        if (names.size < 2) return 0.0
        val selected = names.map { name -> anchors.single { it.name == name } }
        return selected.zipWithNext().maxOf { (first, second) ->
            abs((second.actualY - first.actualY) - (second.expectedY - first.expectedY))
        }
    }

    private fun sideBySide(
        left: BufferedImage,
        right: BufferedImage,
    ): BufferedImage =
        BufferedImage(left.width * 2, left.height, BufferedImage.TYPE_INT_ARGB).also { target ->
            target.createGraphics().use { graphics ->
                graphics.drawImage(left, 0, 0, null)
                graphics.drawImage(right, left.width, 0, null)
            }
        }

    private fun overlay(
        left: BufferedImage,
        right: BufferedImage,
    ): BufferedImage =
        BufferedImage(left.width, left.height, BufferedImage.TYPE_INT_ARGB).also { target ->
            target.createGraphics().use { graphics ->
                graphics.drawImage(left, 0, 0, null)
                graphics.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
                graphics.drawImage(right, 0, 0, null)
            }
        }

    private fun diff(
        left: BufferedImage,
        right: BufferedImage,
        masks: List<Rect>,
    ): BufferedImage =
        BufferedImage(left.width, left.height, BufferedImage.TYPE_INT_ARGB).also { target ->
            for (y in 0 until left.height) {
                for (x in 0 until left.width) {
                    if (masks.any { x >= it.x && x < it.x + it.width && y >= it.y && y < it.y + it.height }) {
                        target.setRGB(x, y, Color(0x66, 0x66, 0x66).rgb)
                    } else {
                        val a = Color(left.getRGB(x, y), true)
                        val b = Color(right.getRGB(x, y), true)
                        target.setRGB(x, y, Color(abs(a.red - b.red), abs(a.green - b.green), abs(a.blue - b.blue)).rgb)
                    }
                }
            }
        }

    private fun writeResult(
        outputDir: File,
        result: UiEvidenceResult,
    ) {
        Properties()
            .apply {
                setProperty("contractId", result.contractId)
                setProperty("meanError", result.meanError.toString())
                setProperty("changedRatio", result.changedRatio.toString())
                setProperty("cumulativeDrift", result.cumulativeDrift.toString())
                setProperty("status", result.status.name)
                setProperty("passed", (result.status == UiEvidenceStatus.PASS).toString())
                setProperty("contractSha256", result.sources.contractSha256)
                setProperty("designSha256", result.sources.designSha256)
                setProperty("renderedSha256", result.sources.renderedSha256)
                result.sources.probeRenderedSha256?.let { setProperty("probeRenderedSha256", it) }
                result.anchorResults.forEach { anchor -> setProperty("anchor.${anchor.name}.error", anchor.error.toString()) }
            }.also { properties -> File(outputDir, "result.properties").writer().use { properties.store(it, null) } }
        val anchors =
            result.anchorResults.joinToString(",") { anchor ->
                "{\"name\":\"${anchor.name}\",\"expected\":[${anchor.expectedX},${anchor.expectedY}],\"actual\":[${anchor.actualX},${anchor.actualY}],\"error\":${anchor.error},\"tolerance\":${anchor.tolerance}}"
            }
        val json =
            buildString {
                append("{\"contractId\":\"${result.contractId}\",\"status\":\"${result.status}\"")
                append(",\"meanError\":${result.meanError},\"changedRatio\":${result.changedRatio}")
                append(",\"cumulativeDrift\":${result.cumulativeDrift}")
                append(",\"contractSha256\":\"${result.sources.contractSha256}\"")
                append(",\"designSha256\":\"${result.sources.designSha256}\"")
                append(",\"renderedSha256\":\"${result.sources.renderedSha256}\"")
                append(",\"probeRenderedSha256\":${result.sources.probeRenderedSha256?.let { "\"$it\"" } ?: "null"}")
                append(",\"anchors\":[$anchors]}\n")
            }
        File(outputDir, "result.json").writeText(json)
    }
}

private inline fun <T : java.awt.Graphics> T.use(block: (T) -> Unit) {
    try {
        block(this)
    } finally {
        dispose()
    }
}
