import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Properties

abstract class VerifyUiFidelityTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contractFiles: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val selectedContract: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val evidenceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val contracts = contractFiles.files.sortedBy(File::getName).map(UiContractParser::parse)
        val selected = selectedContract.orNull
        val targets = if (selected == null) contracts else contracts.filter { it.id == selected }
        if (selected != null && targets.isEmpty()) throw GradleException("找不到 UI contract：$selected")
        val failures = mutableListOf<String>()
        targets.forEach { contract ->
            if (contract.properties.getProperty("debt.status") == "active") {
                logger.warn("${contract.id}: 历史视觉债务暂未强制，修改范围或到期后将阻断。")
                return@forEach
            }
            val resultFile = evidenceFiles.files.singleOrNull { it.parentFile.name == contract.id && it.name == "result.properties" }
            if (resultFile == null) {
                failures += "${contract.id}: 缺少当轮 result.properties"
            } else {
                val result = Properties().apply { resultFile.reader().use(::load) }
                if (result.getProperty("passed") != "true") {
                    failures +=
                        "${contract.id}: mean=${result.getProperty(
                            "meanError",
                        )}, changed=${result.getProperty("changedRatio")}, drift=${result.getProperty("cumulativeDrift")}"
                }
            }
        }
        if (failures.isNotEmpty()) throw GradleException("设计符合度失败：\n${failures.joinToString("\n") { "- $it" }}")
        logger.lifecycle("设计符合度门禁通过：${targets.count { it.properties.getProperty("debt.status") == "none" }} 个强制 contract。")
    }
}
