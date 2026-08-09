import java.security.MessageDigest
import java.util.Properties

internal enum class AgentExecutionPhase {
    Worktree,
    Staged,
    Committed,
    ;

    companion object {
        fun parse(value: String): AgentExecutionPhase =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: error("agent execution phase 必须为 worktree、staged 或 committed")
    }
}

internal data class AgentExecutionRecord(
    val task: String,
    val authorizedPaths: Set<String>,
    val actualFiles: Set<String>,
    val childChecks: String,
    val unverifiedItems: String,
    val implementedByAgent: String,
    val committedByAgent: String,
    val identityProof: String,
)

internal data class AgentExecutionReceipt(
    val recordContentSha256: String,
    val baseHead: String,
    val stagedFiles: Set<String>,
    val stagedPatchSha256: String,
)

internal object AgentExecutionPolicy {
    fun parse(properties: Properties): AgentExecutionRecord {
        require(properties.getProperty("schemaVersion") == "1") { "agent execution record: schemaVersion 必须为 1" }
        val identityProof = properties.required("identityProof")
        require(identityProof == TRACE_ONLY_IDENTITY_PROOF) { "agent execution record: identityProof 只能为 trace-only" }
        return AgentExecutionRecord(
            task = properties.required("task"),
            authorizedPaths = properties.requiredPaths("authorizedPaths"),
            actualFiles = properties.requiredPaths("actualFiles"),
            childChecks = properties.required("childChecks"),
            unverifiedItems = properties.required("unverifiedItems"),
            implementedByAgent = properties.required("implementedByAgent"),
            committedByAgent = properties.required("committedByAgent"),
            identityProof = identityProof,
        )
    }

    fun parseReceipt(properties: Properties): AgentExecutionReceipt {
        require(properties.getProperty("schemaVersion") == "1") { "agent execution receipt: schemaVersion 必须为 1" }
        return AgentExecutionReceipt(
            recordContentSha256 = properties.required("recordContentSha256"),
            baseHead = properties.required("baseHead"),
            stagedFiles = properties.requiredPaths("stagedFiles"),
            stagedPatchSha256 = properties.required("stagedPatchSha256"),
        )
    }

    fun receiptProperties(receipt: AgentExecutionReceipt): Properties =
        Properties().apply {
            setProperty("schemaVersion", "1")
            setProperty("recordContentSha256", receipt.recordContentSha256)
            setProperty("baseHead", receipt.baseHead)
            setProperty("stagedFiles", receipt.stagedFiles.sorted().joinToString(","))
            setProperty("stagedPatchSha256", receipt.stagedPatchSha256)
        }

    fun validate(
        record: AgentExecutionRecord,
        phase: AgentExecutionPhase,
        changedFiles: Set<String>,
        unstagedFiles: Set<String> = emptySet(),
        commitBody: String = "",
    ): List<String> =
        buildList {
            if (record.actualFiles.isEmpty()) add("agent execution record: actualFiles 不能为空")
            record.actualFiles
                .filterNot { file -> record.authorizedPaths.any { path -> file == path || file.startsWith("$path/") } }
                .forEach { file -> add("agent execution record: actualFiles 越过授权路径 $file") }
            if (record.actualFiles != changedFiles) add("agent execution record: ${phase.name.lowercase()} 变更必须与 actualFiles 精确一致")
            if (phase == AgentExecutionPhase.Staged && unstagedFiles.isNotEmpty()) {
                add("agent execution record: staged 阶段存在未暂存产品变更 ${unstagedFiles.joinToString()}")
            }
            if (phase == AgentExecutionPhase.Committed) {
                val trailers = parseTrailers(commitBody)
                if (trailers[IMPLEMENTED_BY_TRAILER] !=
                    record.implementedByAgent
                ) {
                    add("agent execution record: commit 缺少匹配的 $IMPLEMENTED_BY_TRAILER trailer")
                }
                if (trailers[COMMITTED_BY_TRAILER] !=
                    record.committedByAgent
                ) {
                    add("agent execution record: commit 缺少匹配的 $COMMITTED_BY_TRAILER trailer")
                }
            }
        }

    fun validateReceipt(
        receipt: AgentExecutionReceipt,
        recordContentSha256: String,
        commitParent: String,
        committedFiles: Set<String>,
        committedPatchSha256: String,
    ): List<String> =
        buildList {
            if (receipt.recordContentSha256 != recordContentSha256) add("agent execution receipt: record 内容 hash 不匹配")
            if (receipt.baseHead != commitParent) add("agent execution receipt: commit parent 与 staged base 不匹配")
            if (receipt.stagedFiles != committedFiles) add("agent execution receipt: committed 文件与 staged receipt 不匹配")
            if (receipt.stagedPatchSha256 != committedPatchSha256) add("agent execution receipt: committed patch 与 staged receipt 不匹配")
        }

    fun sha256(content: String): String =
        MessageDigest.getInstance("SHA-256").digest(content.toByteArray()).joinToString("") { "%02x".format(it) }

    fun identityProofIsStrong(record: AgentExecutionRecord): Boolean = record.identityProof != TRACE_ONLY_IDENTITY_PROOF

    private fun Properties.required(key: String): String =
        getProperty(key)?.trim().takeUnless(String?::isNullOrEmpty) ?: error("agent execution record: 缺少 $key")

    private fun Properties.requiredPaths(key: String): Set<String> {
        val paths =
            required(key)
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()
        require(paths.isNotEmpty()) { "agent execution record: $key 不能为空" }
        paths.forEach { path -> require(path.isSafeRepositoryPath()) { "agent execution record: $key 路径非法 $path" } }
        return paths
    }

    private fun parseTrailers(commitBody: String): Map<String, String> =
        commitBody
            .lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf(": ")
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 2).trim()
            }.toMap()

    private fun String.isSafeRepositoryPath(): Boolean = isNotEmpty() && !startsWith('/') && split('/').none { it == ".." }

    private const val TRACE_ONLY_IDENTITY_PROOF = "trace-only"
    private const val IMPLEMENTED_BY_TRAILER = "Implemented-By-Agent"
    private const val COMMITTED_BY_TRAILER = "Committed-By-Agent"
}
