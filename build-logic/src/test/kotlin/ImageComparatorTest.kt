import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.Properties
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory

class ImageComparatorTest {
    @Test
    fun `生成可复跑指标并从 probe 图片读取真实锚点`() {
        val root = createTempDirectory("moekoe-image-compare-").toFile()
        writeImage(File(root, "design.png"), Color.WHITE)
        writeImage(File(root, "rendered.png"), Color.WHITE)
        writeImage(File(root, "probe.png"), Color.WHITE).also { image ->
            for (y in 4..5) for (x in 4..5) image.setRGB(x, y, Color.MAGENTA.rgb)
            ImageIO.write(image, "png", File(root, "probe.png"))
        }
        val properties =
            Properties().apply {
                setProperty("design.path", "design.png")
                setProperty("screenshot.rendered", "rendered.png")
                setProperty("probe.rendered", "probe.png")
                setProperty("design.crop", "0,0,10,10")
                setProperty("render.crop", "0,0,10,10")
                setProperty("debt.status", "none")
                setProperty("anchor.center", "4.5,4,0,255,0,255")
                setProperty("tolerance.cumulativeY", "0")
                setProperty("pixel.meanError.max", "0")
                setProperty("pixel.changedRatio.max", "0")
            }
        val output = File(root, "output")
        File(root, "sample.properties").writeText("id=sample\n")

        val result = ImageComparator.generate(root, UiContract(File(root, "sample.properties"), "sample", properties), output)

        assertEquals(UiEvidenceStatus.PASS, result.status)
        assertEquals(File(root, "sample.properties").sha256(), result.sources.contractSha256)
        assertEquals(File(root, "design.png").sha256(), result.sources.designSha256)
        assertEquals(File(root, "rendered.png").sha256(), result.sources.renderedSha256)
        assertEquals(File(root, "probe.png").sha256(), result.sources.probeRenderedSha256)
        assertEquals(4.5, result.anchorResults.single().actualX, 0.0)
        assertEquals(4.0, result.anchorResults.single().actualY, 0.0)
        assertTrue(File(output, "result.json").isFile)
        assertTrue(File(output, "overlay.png").isFile)
    }

    @Test
    fun `active 债务仅在不劣于登记指标时输出 APPROVED_DEBT`() {
        val root = createTempDirectory("moekoe-image-debt-").toFile()
        writeImage(File(root, "design.png"), Color.WHITE)
        writeImage(File(root, "rendered.png"), Color.WHITE)
        val properties =
            Properties().apply {
                setProperty("design.path", "design.png")
                setProperty("screenshot.rendered", "rendered.png")
                setProperty("design.crop", "0,0,10,10")
                setProperty("render.crop", "0,0,10,10")
                setProperty("debt.status", "active")
                setProperty("pixel.meanError.max", "0")
                setProperty("pixel.changedRatio.max", "0")
                setProperty("tolerance.cumulativeY", "0")
                setProperty("debt.baseline.meanError", "0")
                setProperty("debt.baseline.changedRatio", "0")
                setProperty("debt.baseline.cumulativeDrift", "0")
            }
        File(root, "sample.properties").writeText("id=sample\n")

        val result = ImageComparator.generate(root, UiContract(File(root, "sample.properties"), "sample", properties), File(root, "output"))

        assertEquals(UiEvidenceStatus.APPROVED_DEBT, result.status)
        val output = Properties().apply { File(root, "output/result.properties").reader().use(::load) }
        assertEquals("APPROVED_DEBT", output.getProperty("status"))
        assertEquals("false", output.getProperty("passed"))
    }

    @Test
    fun `active 债务不能绕过局部区域失败`() {
        val root = createTempDirectory("moekoe-image-debt-region-").toFile()
        writeImage(File(root, "design.png"), Color.WHITE)
        writeImage(File(root, "rendered.png"), Color.WHITE).also { image ->
            image.setRGB(1, 1, Color.BLACK.rgb)
            ImageIO.write(image, "png", File(root, "rendered.png"))
        }
        val properties =
            baseProperties("0,0,10,10").apply {
                setProperty("debt.status", "active")
                setProperty("debt.baseline.meanError", "1")
                setProperty("debt.baseline.changedRatio", "1")
                setProperty("debt.baseline.cumulativeDrift", "0")
                setProperty("region.title", "0,0,2,2;0;0")
            }
        File(root, "sample.properties").writeText("id=sample\n")

        val result = ImageComparator.generate(root, UiContract(File(root, "sample.properties"), "sample", properties), File(root, "output"))

        assertFalse(result.regionResults.single().passed)
        assertEquals(UiEvidenceStatus.FAIL, result.status)
    }

    @Test
    fun `局部错误不能被全页平均值稀释`() {
        val root = createTempDirectory("moekoe-image-region-").toFile()
        writeImage(File(root, "design.png"), Color.WHITE, 100, 100)
        writeImage(File(root, "rendered.png"), Color.WHITE, 100, 100).also { image ->
            val graphics = image.createGraphics()
            try {
                graphics.color = Color.BLACK
                graphics.fillRect(40, 40, 10, 10)
            } finally {
                graphics.dispose()
            }
            ImageIO.write(image, "png", File(root, "rendered.png"))
        }
        val properties = baseProperties("0,0,100,100").apply { setProperty("region.typography", "40,40,10,10;0.10;0.10") }
        File(root, "sample.properties").writeText("id=sample\n")

        val output = File(root, "output")
        val result = ImageComparator.generate(root, UiContract(File(root, "sample.properties"), "sample", properties), output)

        assertTrue(result.meanError <= 0.02)
        assertTrue(result.changedRatio <= 0.02)
        assertFalse(result.regionResults.single().passed)
        assertEquals(UiEvidenceStatus.FAIL, result.status)
        val evidence = Properties().apply { File(output, "result.properties").reader().use(::load) }
        assertEquals("false", evidence.getProperty("region.typography.passed"))
        assertTrue(File(output, "result.json").readText().contains("\"regions\""))
    }

    @Test
    fun `局部通过不能覆盖全页失败`() {
        val root = createTempDirectory("moekoe-image-global-failure-").toFile()
        writeImage(File(root, "design.png"), Color.WHITE)
        writeImage(File(root, "rendered.png"), Color.WHITE).also { image ->
            image.setRGB(9, 9, Color.BLACK.rgb)
            ImageIO.write(image, "png", File(root, "rendered.png"))
        }
        val properties =
            baseProperties("0,0,10,10").apply {
                setProperty("pixel.meanError.max", "0")
                setProperty("pixel.changedRatio.max", "0")
                setProperty("region.title", "0,0,2,2;0;0")
            }
        File(root, "sample.properties").writeText("id=sample\n")

        val result = ImageComparator.generate(root, UiContract(File(root, "sample.properties"), "sample", properties), File(root, "output"))

        assertTrue(result.regionResults.single().passed)
        assertEquals(UiEvidenceStatus.FAIL, result.status)
    }

    private fun baseProperties(crop: String): Properties =
        Properties().apply {
            setProperty("design.path", "design.png")
            setProperty("screenshot.rendered", "rendered.png")
            setProperty("design.crop", crop)
            setProperty("render.crop", crop)
            setProperty("debt.status", "none")
            setProperty("tolerance.cumulativeY", "0")
            setProperty("pixel.meanError.max", "0.02")
            setProperty("pixel.changedRatio.max", "0.02")
        }

    private fun writeImage(
        file: File,
        color: Color,
        width: Int = 10,
        height: Int = 10,
    ): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { image ->
            val graphics = image.createGraphics()
            try {
                graphics.color = color
                graphics.fillRect(0, 0, image.width, image.height)
            } finally {
                graphics.dispose()
            }
            ImageIO.write(image, "png", file)
        }
}
