import org.junit.Assert.assertEquals
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

        val result = ImageComparator.generate(root, UiContract(File(root, "sample.properties"), "sample", properties), output)

        assertTrue(result.passed)
        assertEquals(4.5, result.anchorResults.single().actualX, 0.0)
        assertEquals(4.0, result.anchorResults.single().actualY, 0.0)
        assertTrue(File(output, "result.json").isFile)
        assertTrue(File(output, "overlay.png").isFile)
    }

    private fun writeImage(
        file: File,
        color: Color,
    ): BufferedImage =
        BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB).also { image ->
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
