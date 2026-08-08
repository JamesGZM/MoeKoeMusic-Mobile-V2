import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Properties

abstract class VerifyUiFidelityTask : DefaultTask() {
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contractFiles: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val selectedContract: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val evidenceFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val evidenceSourceFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val contracts = contractFiles.files.sortedBy(File::getName).map(UiContractParser::parse)
        val selected = selectedContract.orNull
        val targets = if (selected == null) contracts else contracts.filter { it.id == selected }
        if (selected != null && targets.isEmpty()) throw GradleException("找不到 UI contract：$selected")
        val failures = mutableListOf<String>()
        var passedCount = 0
        var debtCount = 0
        targets.forEach { contract ->
            val resultFile = evidenceFiles.files.singleOrNull { it.parentFile.name == contract.id && it.name == "result.properties" }
            if (resultFile == null) {
                failures += "${contract.id}: 缺少当轮 result.properties"
            } else {
                val result = Properties().apply { resultFile.reader().use(::load) }
                val freshnessFailure = verifyFreshSources(contract, result)
                if (freshnessFailure != null) {
                    failures += freshnessFailure
                } else {
                    val expectedStatus = if (contract.properties.getProperty("debt.status") == "active") "APPROVED_DEBT" else "PASS"
                    if (result.getProperty("status") != expectedStatus) {
                        val failedRegions =
                            result
                                .stringPropertyNames()
                                .filter { it.startsWith("region.") && it.endsWith(".passed") && result.getProperty(it) == "false" }
                                .map { it.removePrefix("region.").removeSuffix(".passed") }
                                .sorted()
                        failures +=
                            "${contract.id}: status=${result.getProperty("status")}, expected=$expectedStatus, mean=${result.getProperty(
                                "meanError",
                            )}, changed=${result.getProperty("changedRatio")}, drift=${result.getProperty("cumulativeDrift")}, " +
                            "failedRegions=${failedRegions.ifEmpty { listOf("none") }.joinToString()}"
                    } else if (expectedStatus == "PASS") {
                        passedCount++
                    } else {
                        debtCount++
                    }
                }
            }
        }
        if (failures.isNotEmpty()) throw GradleException("设计符合度失败：\n${failures.joinToString("\n") { "- $it" }}")
        logger.lifecycle("设计符合度门禁通过：PASS=$passedCount，APPROVED_DEBT=$debtCount。")
    }

    private fun verifyFreshSources(
        contract: UiContract,
        result: Properties,
    ): String? {
        val root = repositoryRoot.get().asFile
        val expected =
            buildMap {
                put("contractSha256", contract.file.sha256())
                put("designSha256", sourceHash(root, contract, "design.path"))
                put("renderedSha256", sourceHash(root, contract, "screenshot.rendered"))
                if (UiContractParser.hasProbe(contract.properties)) put("probeRenderedSha256", sourceHash(root, contract, "probe.rendered"))
            }
        val stale = expected.filter { (key, value) -> result.getProperty(key) != value }.keys
        return if (stale.isEmpty()) null else "${contract.id}: result.properties 来源已陈旧：${stale.joinToString()}"
    }

    private fun sourceHash(
        root: File,
        contract: UiContract,
        key: String,
    ): String = UiContractParser.resolveRepositoryPath(root, contract.id, contract.properties.getProperty(key)).sha256()
}
