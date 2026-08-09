import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.Properties

@DisableCachingByDefault(because = "读取当前 Git 变更范围和当轮视觉证据")
abstract class VerifyUiImpactTask : DefaultTask() {
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val registryFile: RegularFileProperty

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
        val contracts = contractFiles.files.map(UiContractParser::parse)
        val registry =
            Properties().apply {
                registryFile
                    .get()
                    .asFile
                    .reader(Charsets.UTF_8)
                    .use(::load)
            }
        val entries = UiImpactPolicy.parse(registry, contracts.mapTo(linkedSetOf(), UiContract::id))
        entries.forEach { entry ->
            entry.sources.forEach { sourcePath ->
                val source = File(root, sourcePath)
                if (!source.isFile) throw GradleException("UI impact registry: source 不存在 $sourcePath")
            }
        }
        val affected = UiImpactPolicy.affectedContracts(entries, changedFiles(root).keys)
        val failures =
            affected.mapNotNull { contractId ->
                val result = File(evidenceRoot.get().asFile, "$contractId/result.properties")
                val properties = Properties().apply { if (result.isFile) result.reader(Charsets.UTF_8).use(::load) }
                if (properties.getProperty("contractId") == contractId && properties.getProperty("passed") == "true") {
                    null
                } else {
                    "$contractId: 公共组件变更后缺少通过的当轮设计符合度证据"
                }
            }
        if (failures.isNotEmpty()) throw GradleException("UI 公共组件影响面门禁失败：\n${failures.joinToString("\n") { "- $it" }}")
        logger.lifecycle("UI 公共组件影响面门禁通过：${affected.size} 个受影响 contract。")
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
