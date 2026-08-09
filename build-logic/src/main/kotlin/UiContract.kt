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
            "contract.kind",
            "structure.coverage",
            "state.spec",
            "state.testSource",
            "state.testPattern",
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
            "tolerance.profile",
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
        require(properties.getProperty("contract.kind") == "structure") { "$id: contract.kind 必须为 structure" }
        require(properties.getProperty("structure.coverage") in setOf("core-page", "dialog", "sheet")) {
            "$id: structure.coverage 必须为 core-page、dialog 或 sheet"
        }
        require(properties.stringPropertyNames().any { it.startsWith("anchor.") }) { "$id: 至少登记一个固定锚点" }
        properties.stringPropertyNames().filter { it.startsWith("regression.golden.") }.forEach { key ->
            require(key.removePrefix("regression.golden.").isNotBlank()) { "$id: regression golden 状态名不能为空" }
            require(!properties.getProperty(key).isNullOrBlank()) { "$id: $key 路径不能为空" }
        }
        validateCrop(id, "design.crop", properties.getProperty("design.crop"))
        validateCrop(id, "render.crop", properties.getProperty("render.crop"))
        validateRegions(id, properties)
        validateToleranceProfile(id, properties)
        validateStressCases(id, properties)
        validateDebt(id, properties)
        if (properties.getProperty("debt.status") == "none") {
            listOf("probe.testId", "probe.source", "probe.golden", "probe.goldenSha256", "probe.rendered").forEach { key ->
                require(!properties.getProperty(key).isNullOrBlank()) { "$id: 强制 contract 缺少 $key" }
            }
        }
        return UiContract(file, id, properties)
    }

    private fun validateToleranceProfile(
        id: String,
        properties: Properties,
    ) {
        val fixed = properties.getProperty("tolerance.fixed").toDoubleOrNull()
        val cumulative = properties.getProperty("tolerance.cumulativeY").toDoubleOrNull()
        require(fixed != null && fixed >= 0.0) { "$id: tolerance.fixed 必须是非负数字" }
        require(cumulative != null && cumulative >= 0.0) { "$id: tolerance.cumulativeY 必须是非负数字" }
        when (properties.getProperty("tolerance.profile")) {
            "strict" -> {
                require(fixed <= STRICT_FIXED_MAX && cumulative <= STRICT_CUMULATIVE_Y_MAX) {
                    "$id: strict contract 必须满足 fixed<=${STRICT_FIXED_MAX.toInt()}、cumulativeY<=${STRICT_CUMULATIVE_Y_MAX.toInt()}"
                }
                if (properties.getProperty("structure.coverage") == "core-page") {
                    val regionCount = properties.stringPropertyNames().count { it.startsWith("region.") }
                    require(regionCount >= STRICT_CORE_PAGE_REGION_MIN) {
                        "$id: strict core-page 至少需要 $STRICT_CORE_PAGE_REGION_MIN 个局部视觉 region"
                    }
                }
            }

            "migration" -> {
                listOf("migration.reason", "migration.owner", "migration.expiresAt").forEach { key ->
                    require(!properties.getProperty(key).isNullOrBlank()) { "$id: migration contract 缺少 $key" }
                }
                val expiresAt = LocalDate.parse(properties.getProperty("migration.expiresAt"))
                require(!expiresAt.isBefore(LocalDate.now())) { "$id: migration contract 已于 $expiresAt 到期" }
            }

            else -> {
                error("$id: tolerance.profile 只能是 strict 或 migration")
            }
        }
    }

    private fun validateStressCases(
        id: String,
        properties: Properties,
    ) {
        properties.getProperty("stress.singleLineInput")?.let { value ->
            require(value == "true" || value == "false") { "$id: stress.singleLineInput 必须是 true 或 false" }
            if (value == "true") {
                val regression = properties.getProperty("stress.longText.regression")
                require(!regression.isNullOrBlank()) { "$id: 单行输入 contract 缺少 stress.longText.regression" }
                require(!properties.getProperty("regression.golden.$regression").isNullOrBlank()) {
                    "$id: stress.longText.regression=$regression 未绑定 regression golden"
                }
            }
        }
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

    private fun validateRegions(
        id: String,
        properties: Properties,
    ) {
        val designCrop = parseContractRect(id, "design.crop", properties.getProperty("design.crop"))
        val excluded =
            properties
                .stringPropertyNames()
                .filter { it.startsWith("mask.") || it.startsWith("dynamic.") }
                .associateWith { key -> parseContractRect(id, key, properties.getProperty(key).substringBefore(';')) }
        properties.stringPropertyNames().filter { it.startsWith("region.") }.sorted().forEach { key ->
            val name = key.removePrefix("region.")
            require(REGION_NAME.matches(name)) { "$id: $key 名称必须匹配 ${REGION_NAME.pattern}" }
            val parts = properties.getProperty(key).split(';').map(String::trim)
            require(parts.size == 3) { "$id: $key 必须为 x,y,width,height;meanErrorMax;changedRatioMax" }
            val rect = parseContractRect(id, key, parts[0])
            require(rect.fitsWithin(designCrop.width, designCrop.height)) { "$id: $key 超出 design crop 边界" }
            parts.drop(1).forEach { threshold ->
                val value = threshold.toDoubleOrNull()
                require(value != null && value in 0.0..1.0) { "$id: $key 阈值必须位于 [0,1]" }
            }
            excluded.forEach { (excludedKey, excludedRect) ->
                require(!rect.overlaps(excludedRect)) { "$id: $key 不得与 $excludedKey 相交" }
            }
        }
    }

    private fun parseContractRect(
        id: String,
        key: String,
        value: String,
    ): ContractRect {
        val parts = value.split(',').map(String::trim)
        require(parts.size == 4 && parts.all { it.toIntOrNull() != null }) { "$id: $key 必须为 x,y,width,height" }
        val values = parts.map(String::toInt)
        require(values[0] >= 0 && values[1] >= 0 && values[2] > 0 && values[3] > 0) { "$id: $key 坐标非负且宽高必须大于零" }
        return ContractRect(values[0], values[1], values[2], values[3])
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

    fun goldenPaths(properties: Properties): Set<String> =
        buildSet {
            properties.getProperty("screenshot.golden")?.takeIf(String::isNotBlank)?.let(::add)
            properties.getProperty("probe.golden")?.takeIf(String::isNotBlank)?.let(::add)
            properties
                .stringPropertyNames()
                .filter { it.startsWith("regression.golden.") }
                .mapNotNull { properties.getProperty(it)?.takeIf(String::isNotBlank) }
                .forEach(::add)
        }

    fun regressionGoldenPaths(properties: Properties): Set<String> =
        properties
            .stringPropertyNames()
            .filter { it.startsWith("regression.golden.") }
            .mapTo(linkedSetOf()) { properties.getProperty(it) }

    private data class ContractRect(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    ) {
        fun fitsWithin(
            outerWidth: Int,
            outerHeight: Int,
        ): Boolean = x <= outerWidth && y <= outerHeight && width <= outerWidth - x && height <= outerHeight - y

        fun overlaps(other: ContractRect): Boolean =
            x < other.x + other.width && x + width > other.x && y < other.y + other.height && y + height > other.y
    }

    private val REGION_NAME = Regex("[A-Za-z][A-Za-z0-9_-]*")
    private const val STRICT_FIXED_MAX = 2.0
    private const val STRICT_CUMULATIVE_Y_MAX = 3.0
    private const val STRICT_CORE_PAGE_REGION_MIN = 3
}
