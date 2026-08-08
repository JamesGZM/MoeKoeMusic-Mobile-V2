import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.util.Properties
import kotlin.io.path.createTempDirectory

class UiContractParserTest {
    @Test
    fun `解析完整 contract 并拒绝路径越界`() {
        val root = createTempDirectory("moekoe-contract-").toFile()
        val contractFile = File(root, "sample.properties")
        val properties =
            Properties().apply {
                UiContractParser.requiredKeys.forEach { setProperty(it, "value") }
                setProperty("schemaVersion", "1")
                setProperty("id", "sample")
                setProperty("approval.status", "confirmed")
                setProperty("design.crop", "0,0,10,20")
                setProperty("render.crop", "0,0,10,20")
                setProperty("debt.status", "none")
                setProperty("anchor.card", "0,0,2,255,0,255")
                setProperty("probe.testId", "SampleProbe")
                setProperty("probe.source", "sample.kt")
                setProperty("probe.golden", "probe.png")
                setProperty("probe.goldenSha256", "hash")
                setProperty("probe.rendered", "rendered-probe.png")
            }
        contractFile.writer().use { properties.store(it, null) }

        assertEquals("sample", UiContractParser.parse(contractFile).id)
        assertThrows(IllegalArgumentException::class.java) {
            UiContractParser.resolveRepositoryPath(root, "sample", "../outside.png")
        }
    }

    @Test
    fun `active 债务必须包含期限和范围`() {
        val root = createTempDirectory("moekoe-debt-").toFile()
        val contractFile = File(root, "sample.properties")
        val properties =
            Properties().apply {
                UiContractParser.requiredKeys.forEach { setProperty(it, "value") }
                setProperty("schemaVersion", "1")
                setProperty("id", "sample")
                setProperty("approval.status", "confirmed")
                setProperty("design.crop", "0,0,10,20")
                setProperty("render.crop", "0,0,10,20")
                setProperty("debt.status", "active")
                setProperty("anchor.card", "0,0,2")
            }
        contractFile.writer().use { properties.store(it, null) }

        assertThrows(IllegalArgumentException::class.java) { UiContractParser.parse(contractFile) }
    }

    @Test
    fun `active 债务必须包含可审计基线`() {
        val root = createTempDirectory("moekoe-debt-baseline-").toFile()
        val contractFile = File(root, "sample.properties")
        val properties =
            Properties().apply {
                UiContractParser.requiredKeys.forEach { setProperty(it, "value") }
                setProperty("schemaVersion", "1")
                setProperty("id", "sample")
                setProperty("approval.status", "confirmed")
                setProperty("design.crop", "0,0,10,20")
                setProperty("render.crop", "0,0,10,20")
                setProperty("debt.status", "active")
                setProperty("anchor.card", "0,0,2,255,0,255")
                setProperty("debt.reason", "历史偏差")
                setProperty("debt.owner", "feature-login")
                setProperty("debt.expiresAt", "2026-09-07")
                setProperty("debt.scopePaths", "feature/login")
            }
        contractFile.writer().use { properties.store(it, null) }

        assertThrows(IllegalArgumentException::class.java) { UiContractParser.parse(contractFile) }
    }
}
