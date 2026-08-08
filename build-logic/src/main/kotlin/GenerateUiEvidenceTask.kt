import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateUiEvidenceTask : DefaultTask() {
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contractFiles: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val selectedContract: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val contracts = contractFiles.files.sortedBy(File::getName).map(UiContractParser::parse)
        val selected = selectedContract.orNull
        val targets = if (selected == null) contracts else contracts.filter { it.id == selected }
        if (selected != null && targets.isEmpty()) throw GradleException("找不到 UI contract：$selected")
        targets.forEach { contract ->
            try {
                val result =
                    ImageComparator.generate(
                        repositoryRoot.get().asFile,
                        contract,
                        outputDirectory.dir(contract.id).get().asFile,
                    )
                if (contract.properties.getProperty("debt.status") == "active") {
                    logger.lifecycle("${contract.id}: 已生成指标，当前登记为历史视觉债务，不计为通过。")
                } else {
                    logger.lifecycle("${contract.id}: mean=${result.meanError}, changed=${result.changedRatio}, passed=${result.passed}")
                }
            } catch (error: Exception) {
                throw GradleException("${contract.id}: 无法生成视觉证据：${error.message}", error)
            }
        }
    }
}
