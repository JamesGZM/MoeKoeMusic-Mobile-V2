import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.time.LocalDate
import java.util.Properties

@DisableCachingByDefault(because = "读取当前 Git 变更范围")
abstract class VerifyUiGoldenChangeTask : DefaultTask() {
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contractFiles: ConfigurableFileCollection

    @get:Internal
    abstract val evidenceRoot: DirectoryProperty

    @get:Internal
    abstract val changedFilesFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val root = repositoryRoot.get().asFile
        val changed = changedFiles(root)
        val contracts = contractFiles.files.map(UiContractParser::parse)
        val failures = mutableListOf<String>()
        changed.keys.filter { it.contains("/src/screenshotTestDebug/reference/") && it.endsWith(".png") }.forEach { golden ->
            val contract =
                contracts.singleOrNull {
                    it.properties.getProperty("screenshot.golden") == golden || it.properties.getProperty("probe.golden") == golden
                }
            if (contract == null) {
                failures += "$golden: reference screenshot 没有 UI contract"
            } else if (contract.properties.getProperty("debt.status") == "none") {
                val result = File(evidenceRoot.get().asFile, "${contract.id}/result.properties")
                if (!result.isFile ||
                    Properties().apply { if (result.isFile) result.reader().use(::load) }.getProperty("passed") != "true"
                ) {
                    failures += "$golden: 缺少通过的当轮设计符合度证据"
                }
            }
        }
        contracts.filter { it.properties.getProperty("debt.status") == "active" }.forEach { contract ->
            val properties = contract.properties
            val expired = !LocalDate.now().isBefore(LocalDate.parse(properties.getProperty("debt.expiresAt")))
            val scopes = properties.getProperty("debt.scopePaths").split(',').map(String::trim)
            val touched = changed.keys.any { file -> scopes.any { scope -> file == scope || file.startsWith("$scope/") } }
            val contractPath = contract.file.relativeTo(root).invariantSeparatorsPath
            val enrollingDebt = changed[contractPath] == "A"
            if (expired || (touched && !enrollingDebt)) {
                failures += "${contract.id}: 历史债务${if (expired) "已到期" else "范围被修改"}，必须先消除偏差"
            }
        }
        if (failures.isNotEmpty()) throw GradleException("UI 基线变更门禁失败：\n${failures.joinToString("\n") { "- $it" }}")
        logger.lifecycle("UI 基线变更门禁通过：检查 ${changed.size} 个变更文件。")
    }

    private fun changedFiles(root: File): Map<String, String> {
        val provided = changedFilesFile.orNull?.asFile
        if (provided?.isFile == true) return parseNameStatus(provided.readLines())
        val process =
            ProcessBuilder("git", "status", "--porcelain=v1", "--untracked-files=all")
                .directory(root)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readLines()
        if (process.waitFor() != 0) throw GradleException("无法读取 Git 变更范围：${output.joinToString(" ")}")
        return output.filter { it.length >= 4 }.associate { line ->
            val rawStatus = line.take(2)
            val rawPath = line.substring(3).substringAfterLast(" -> ")
            rawPath to if (rawStatus == "??" || 'A' in rawStatus) "A" else "M"
        }
    }

    private fun parseNameStatus(lines: List<String>): Map<String, String> =
        lines.map(String::trim).filter(String::isNotEmpty).associate { line ->
            val parts = line.split('\t')
            if (parts.size >= 2) parts.last() to parts.first().take(1) else line to "M"
        }
}
