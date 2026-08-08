import org.gradle.api.GradleException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
                setProperty("contract.kind", "structure")
                setProperty("structure.coverage", "core-page")
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

    @Test
    fun `显式登记的 regression golden 归属同一结构 contract`() {
        val properties =
            Properties().apply {
                setProperty("screenshot.golden", "reference/content-light.png")
                setProperty("probe.golden", "reference/layout-probe.png")
                setProperty("regression.golden.dark", "reference/content-dark.png")
                setProperty("regression.golden.largeText20", "reference/content-large-text-20.png")
            }

        val mapped = UiContractParser.goldenPaths(properties)

        assertEquals(
            setOf(
                "reference/content-light.png",
                "reference/layout-probe.png",
                "reference/content-dark.png",
                "reference/content-large-text-20.png",
            ),
            mapped,
        )
        assertTrue("reference/content-dark.png" in mapped)
        assertFalse("reference/content-wide.png" in mapped)
    }

    @Test
    fun `同一 golden 不能映射到多个 contract`() {
        fun contract(id: String) =
            UiContract(
                file = File("$id.properties"),
                id = id,
                properties = Properties().apply { setProperty("screenshot.golden", "reference/shared.png") },
            )

        assertThrows(GradleException::class.java) {
            indexContractsByGolden(listOf(contract("first"), contract("second")))
        }
    }
}
