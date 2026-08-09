import java.util.Properties

internal data class UiImpactEntry(
    val id: String,
    val sources: Set<String>,
    val contractIds: Set<String>,
)

internal object UiImpactPolicy {
    fun parse(
        properties: Properties,
        knownContractIds: Set<String>,
    ): List<UiImpactEntry> {
        require(properties.getProperty("schemaVersion") == "1") { "UI impact registry: 不支持的 schemaVersion" }
        val ids =
            properties
                .stringPropertyNames()
                .mapNotNull(::componentIdForSourceKey)
                .sorted()
                .distinct()
        require(ids.isNotEmpty()) { "UI impact registry: 至少登记一个公共组件" }
        val entries =
            ids.map { id ->
                require(COMPONENT_ID.matches(id)) { "UI impact registry: 非法组件 ID $id" }
                val source = properties.getProperty("component.$id.source")?.trim()
                val sources = properties.getProperty("component.$id.sources")?.trim()
                require(!(source != null && sources != null)) { "UI impact registry: $id 只能使用 source 或 sources" }
                val sourcePaths = parseSources(id, sources ?: source.orEmpty())
                val contracts =
                    properties
                        .getProperty("component.$id.contracts")
                        ?.split(',')
                        ?.map(String::trim)
                        ?.filter(String::isNotEmpty)
                        ?.toSet()
                        .orEmpty()
                require(contracts.isNotEmpty()) { "UI impact registry: $id 未登记受影响 contract" }
                val unknown = contracts - knownContractIds
                require(unknown.isEmpty()) { "UI impact registry: $id 含未知 contract ${unknown.joinToString()}" }
                UiImpactEntry(id = id, sources = sourcePaths, contractIds = contracts)
            }
        val duplicateSources =
            entries
                .flatMap { entry -> entry.sources.map { source -> source to entry.id } }
                .groupBy({ it.first }, { it.second })
                .filterValues { it.size > 1 }
                .keys
        require(duplicateSources.isEmpty()) { "UI impact registry: source 重复 ${duplicateSources.joinToString()}" }
        return entries
    }

    fun affectedContracts(
        entries: List<UiImpactEntry>,
        changedPaths: Set<String>,
    ): Set<String> =
        entries
            .filter { entry ->
                entry.sources.any(changedPaths::contains)
            }.flatMapTo(linkedSetOf(), UiImpactEntry::contractIds)

    private fun componentIdForSourceKey(key: String): String? =
        when {
            key.startsWith("component.") && key.endsWith(".source") -> key.removePrefix("component.").removeSuffix(".source")
            key.startsWith("component.") && key.endsWith(".sources") -> key.removePrefix("component.").removeSuffix(".sources")
            else -> null
        }

    private fun parseSources(
        id: String,
        rawSources: String,
    ): Set<String> {
        val sources =
            rawSources
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()
        require(sources.isNotEmpty()) { "UI impact registry: $id 未登记 source" }
        sources.forEach { source ->
            require(source.isSafeRepositoryPath()) { "UI impact registry: $id source 必须是仓库内相对路径" }
        }
        return sources
    }

    private fun String.isSafeRepositoryPath(): Boolean = isNotEmpty() && !startsWith('/') && split('/').none { it == ".." }

    private val COMPONENT_ID = Regex("[A-Za-z][A-Za-z0-9_-]*")
}
