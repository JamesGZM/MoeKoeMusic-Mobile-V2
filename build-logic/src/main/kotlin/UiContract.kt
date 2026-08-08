import java.io.File
import java.time.LocalDate
import java.util.Properties

internal data class UiContract(
    val file: File,
    val id: String,
    val properties: Properties,
)

internal object UiContractParser {
    val requiredKeys =
        setOf(
            "schemaVersion",
            "id",
            "module",
            "state",
            "design.path",
            "design.sha256",
            "design.width",
            "design.height",
            "approval.status",
            "approval.evidence",
            "screenshot.testId",
            "screenshot.source",
            "screenshot.golden",
            "screenshot.goldenSha256",
            "screenshot.rendered",
            "viewport.widthDp",
            "viewport.heightDp",
            "locale",
            "theme",
            "fontScale",
            "design.crop",
            "render.crop",
            "tolerance.fixed",
            "tolerance.cumulativeY",
            "debt.status",
        )

    fun parse(file: File): UiContract {
        val properties = Properties().apply { file.reader(Charsets.UTF_8).use(::load) }
        val missing = requiredKeys.filter { properties.getProperty(it).isNullOrBlank() }
        require(missing.isEmpty()) { "${file.name}: 缺少字段 ${missing.joinToString()}" }
        require(properties.getProperty("schemaVersion") == "1") { "${file.name}: 不支持的 schemaVersion" }
        val id = properties.getProperty("id")
        require(file.nameWithoutExtension == id) { "${file.name}: 文件名必须与 id 一致" }
        require(properties.getProperty("approval.status") == "confirmed") { "$id: 设计尚未确认" }
        require(properties.stringPropertyNames().any { it.startsWith("anchor.") }) { "$id: 至少登记一个固定锚点" }
        validateCrop(id, "design.crop", properties.getProperty("design.crop"))
        validateCrop(id, "render.crop", properties.getProperty("render.crop"))
        validateDebt(id, properties)
        if (properties.getProperty("debt.status") == "none") {
            listOf("probe.testId", "probe.source", "probe.golden", "probe.goldenSha256", "probe.rendered").forEach { key ->
                require(!properties.getProperty(key).isNullOrBlank()) { "$id: 强制 contract 缺少 $key" }
            }
        }
        return UiContract(file, id, properties)
    }

    fun resolveRepositoryPath(
        rootDir: File,
        contractId: String,
        value: String,
    ): File {
        require(value.isNotBlank()) { "$contractId: 路径不能为空" }
        val candidate = File(value)
        require(!candidate.isAbsolute && value.split('/', '\\').none { it == ".." }) {
            "$contractId: 路径必须位于仓库内：$value"
        }
        val resolved = File(rootDir, value).canonicalFile
        require(resolved.toPath().startsWith(rootDir.canonicalFile.toPath())) { "$contractId: 路径越界：$value" }
        return resolved
    }

    private fun validateCrop(
        id: String,
        key: String,
        value: String,
    ) {
        val parts = value.split(',').map(String::trim)
        require(parts.size == 4 && parts.all { it.toIntOrNull() != null }) { "$id: $key 必须为 x,y,width,height" }
        require(parts[2].toInt() > 0 && parts[3].toInt() > 0) { "$id: $key 宽高必须大于零" }
    }

    private fun validateDebt(
        id: String,
        properties: Properties,
    ) {
        when (properties.getProperty("debt.status")) {
            "none" -> {
                Unit
            }

            "active" -> {
                listOf(
                    "debt.reason",
                    "debt.owner",
                    "debt.expiresAt",
                    "debt.scopePaths",
                    "debt.baseline.meanError",
                    "debt.baseline.changedRatio",
                    "debt.baseline.cumulativeDrift",
                    "debt.baseline.designSha256",
                    "debt.baseline.renderedSha256",
                ).forEach { key ->
                    require(!properties.getProperty(key).isNullOrBlank()) { "$id: active 债务缺少 $key" }
                }
                LocalDate.parse(properties.getProperty("debt.expiresAt"))
                listOf("debt.baseline.meanError", "debt.baseline.changedRatio", "debt.baseline.cumulativeDrift").forEach { key ->
                    require(properties.getProperty(key).toDoubleOrNull() != null) { "$id: $key 必须是数字" }
                }
                if (hasProbe(properties)) {
                    require(!properties.getProperty("debt.baseline.probeRenderedSha256").isNullOrBlank()) {
                        "$id: 带 probe 的 active 债务缺少 debt.baseline.probeRenderedSha256"
                    }
                    properties.stringPropertyNames().filter { it.startsWith("anchor.") }.forEach { anchor ->
                        val key = "debt.baseline.$anchor.error"
                        require(properties.getProperty(key)?.toDoubleOrNull() != null) { "$id: 带 probe 的 active 债务缺少 $key" }
                    }
                }
            }

            else -> {
                error("$id: debt.status 只能是 none 或 active")
            }
        }
    }

    fun hasProbe(properties: Properties): Boolean = !properties.getProperty("probe.rendered").isNullOrBlank()
}
