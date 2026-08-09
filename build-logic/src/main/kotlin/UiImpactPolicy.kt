import java.util.Properties

internal data class UiImpactEntry(
    val id: String,
    val source: String,
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
                .filter { it.startsWith("component.") && it.endsWith(".source") }
                .map { it.removePrefix("component.").removeSuffix(".source") }
                .sorted()
        require(ids.isNotEmpty()) { "UI impact registry: 至少登记一个公共组件" }
        val entries =
            ids.map { id ->
                require(COMPONENT_ID.matches(id)) { "UI impact registry: 非法组件 ID $id" }
                val source = properties.getProperty("component.$id.source")?.trim().orEmpty()
                require(source.isNotEmpty() && !source.startsWith('/') && source.split('/').none { it == ".." }) {
                    "UI impact registry: $id source 必须是仓库内相对路径"
                }
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
                UiImpactEntry(id = id, source = source, contractIds = contracts)
            }
        val duplicateSources = entries.groupBy(UiImpactEntry::source).filterValues { it.size > 1 }.keys
        require(duplicateSources.isEmpty()) { "UI impact registry: source 重复 ${duplicateSources.joinToString()}" }
        return entries
    }

    fun affectedContracts(
        entries: List<UiImpactEntry>,
        changedPaths: Set<String>,
    ): Set<String> = entries.filter { it.source in changedPaths }.flatMapTo(linkedSetOf(), UiImpactEntry::contractIds)

    private val COMPONENT_ID = Regex("[A-Za-z][A-Za-z0-9_-]*")
}
