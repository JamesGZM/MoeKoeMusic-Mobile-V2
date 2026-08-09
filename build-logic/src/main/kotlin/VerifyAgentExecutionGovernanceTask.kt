import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.Properties

@DisableCachingByDefault(because = "读取当前 Git 暂存和提交状态，并在 staged 阶段写入顺序收据")
abstract class VerifyAgentExecutionGovernanceTask : DefaultTask() {
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val recordFile: RegularFileProperty

    @get:Input
    abstract val phase: Property<String>

    @get:OutputDirectory
    abstract val receiptDirectory: DirectoryProperty

    @TaskAction
    fun verify() {
        val root = repositoryRoot.get().asFile
        val recordPath = recordFile.orNull?.asFile ?: throw GradleException("缺少 -Pmoekoe.agentExecutionRecord=<record.properties>")
        val recordContent = recordPath.readText()
        val record = Properties().apply { recordContent.reader().use(::load) }.let(AgentExecutionPolicy::parse)
        val executionPhase = AgentExecutionPhase.parse(phase.get())
        val changedFiles = changedFiles(root, executionPhase)
        val unstagedFiles = if (executionPhase == AgentExecutionPhase.Staged) unstagedFiles(root) else emptySet()
        val commitBody = if (executionPhase == AgentExecutionPhase.Committed) gitOutput(root, "show", "-s", "--format=%B", "HEAD") else ""
        val failures = AgentExecutionPolicy.validate(record, executionPhase, changedFiles, unstagedFiles, commitBody).toMutableList()
        val receiptPath = receiptPath(recordContent)
        when (executionPhase) {
            AgentExecutionPhase.Worktree -> Unit
            AgentExecutionPhase.Staged -> if (failures.isEmpty()) writeReceipt(receiptPath, recordContent, root, changedFiles)
            AgentExecutionPhase.Committed -> failures += verifyCommittedReceipt(receiptPath, recordContent, root, changedFiles)
        }
        if (failures.isNotEmpty()) throw GradleException("Agent 执行治理门禁失败：\n${failures.joinToString("\n") { "- $it" }}")
        logger.lifecycle("Agent 执行治理门禁通过：${record.task} (${executionPhase.name.lowercase()})。identityProof=trace-only 仅证明记录一致性。")
    }

    private fun receiptPath(recordContent: String): File =
        File(receiptDirectory.get().asFile, "${AgentExecutionPolicy.sha256(recordContent)}.properties")

    private fun writeReceipt(
        receiptPath: File,
        recordContent: String,
        root: File,
        stagedFiles: Set<String>,
    ) {
        val receipt =
            AgentExecutionReceipt(
                recordContentSha256 = AgentExecutionPolicy.sha256(recordContent),
                baseHead = gitOutput(root, "rev-parse", "HEAD").trim(),
                stagedFiles = stagedFiles,
                stagedPatchSha256 = AgentExecutionPolicy.sha256(gitOutput(root, "diff", "--cached", "--binary")),
            )
        receiptPath.parentFile.mkdirs()
        receiptPath.writer().use { AgentExecutionPolicy.receiptProperties(receipt).store(it, null) }
    }

    private fun verifyCommittedReceipt(
        receiptPath: File,
        recordContent: String,
        root: File,
        committedFiles: Set<String>,
    ): List<String> {
        if (!receiptPath.isFile) return listOf("agent execution receipt: 缺少 staged 顺序收据 ${receiptPath.name}")
        val receipt = Properties().apply { receiptPath.reader().use(::load) }.let(AgentExecutionPolicy::parseReceipt)
        return AgentExecutionPolicy.validateReceipt(
            receipt = receipt,
            recordContentSha256 = AgentExecutionPolicy.sha256(recordContent),
            commitParent = gitOutput(root, "rev-parse", "HEAD^").trim(),
            committedFiles = committedFiles,
            committedPatchSha256 = AgentExecutionPolicy.sha256(gitOutput(root, "show", "--format=", "--binary", "HEAD")),
        )
    }

    private fun changedFiles(
        root: File,
        phase: AgentExecutionPhase,
    ): Set<String> =
        when (phase) {
            AgentExecutionPhase.Worktree -> {
                gitLines(root, "status", "--porcelain=v1", "--untracked-files=all")
                    .filter {
                        it.length >= 4
                    }.mapTo(linkedSetOf()) { line -> line.substring(3).substringAfterLast(" -> ") }
            }

            AgentExecutionPhase.Staged -> {
                gitLines(root, "diff", "--cached", "--name-only").toSet()
            }

            AgentExecutionPhase.Committed -> {
                gitLines(root, "diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD").toSet()
            }
        }

    private fun unstagedFiles(root: File): Set<String> =
        buildSet {
            addAll(gitLines(root, "diff", "--name-only"))
            addAll(
                gitLines(root, "status", "--porcelain=v1", "--untracked-files=all").filter { it.startsWith("?? ") }.map { line ->
                    line.substring(3)
                },
            )
        }

    private fun gitLines(
        root: File,
        vararg arguments: String,
    ): List<String> = gitOutput(root, *arguments).lineSequence().filter(String::isNotBlank).toList()

    private fun gitOutput(
        root: File,
        vararg arguments: String,
    ): String {
        val process = ProcessBuilder(listOf("git") + arguments).directory(root).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.waitFor() != 0) throw GradleException("无法读取 Git 执行记录：${output.trim()}")
        return output
    }
}
