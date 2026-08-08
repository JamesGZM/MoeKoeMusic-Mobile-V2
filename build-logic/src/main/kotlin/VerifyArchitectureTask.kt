import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyArchitectureTask : DefaultTask() {
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @get:Input
    abstract val projectEdges: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val edgeViolations = ArchitecturePolicy.validateEdges(projectEdges.get())
        val importViolations = ArchitecturePolicy.validateImports(repositoryRoot.get().asFile, sourceFiles.files)
        val violations = edgeViolations.map { "非法模块依赖：$it" } + importViolations
        if (violations.isNotEmpty()) {
            throw GradleException("架构门禁失败：\n${violations.joinToString("\n") { "- $it" }}")
        }
        logger.lifecycle("架构门禁通过：校验 ${projectEdges.get().distinct().size} 条模块依赖和 ${sourceFiles.files.size} 个生产源码文件。")
    }
}
