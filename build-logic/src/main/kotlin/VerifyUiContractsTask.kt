import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

abstract class VerifyUiContractsTask : DefaultTask() {
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contractFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val contracts = contractFiles.files.sortedBy(File::getName).map(::parseOrFail)
        val duplicateIds = contracts.groupBy(UiContract::id).filterValues { it.size > 1 }.keys
        if (duplicateIds.isNotEmpty()) throw GradleException("UI contract ID 重复：${duplicateIds.joinToString()}")
        contracts.forEach(::verifyContract)
        logger.lifecycle("UI contract 门禁通过：校验 ${contracts.size} 个状态契约。")
    }

    private fun parseOrFail(file: File): UiContract =
        try {
            UiContractParser.parse(file)
        } catch (error: Exception) {
            throw GradleException(error.message ?: "无法解析 ${file.name}", error)
        }

    private fun verifyContract(contract: UiContract) {
        val properties = contract.properties
        val design = requiredPath(contract, "design.path")
        val evidence = requiredPath(contract, "approval.evidence")
        val screenshotSource = requiredPath(contract, "screenshot.source")
        val golden = requiredPath(contract, "screenshot.golden")
        requireFile(design, contract.id)
        requireFile(evidence, contract.id)
        requireFile(screenshotSource, contract.id)
        requireFile(golden, contract.id)
        val expectedDesignHash = properties.getProperty("design.sha256")
        if (expectedDesignHash != design.sha256()) {
            throw GradleException("${contract.id}: 设计源 SHA-256 已变化")
        }
        if (properties.getProperty("screenshot.goldenSha256") != golden.sha256()) {
            throw GradleException("${contract.id}: screenshot golden SHA-256 已变化")
        }
        val source = screenshotSource.readText()
        val testId = properties.getProperty("screenshot.testId")
        if (!source.contains("@PreviewTest") || !source.contains("fun $testId(")) {
            throw GradleException("${contract.id}: 找不到对应 @PreviewTest $testId")
        }
        if (properties.getProperty("debt.status") == "none") {
            val probeSource = requiredPath(contract, "probe.source")
            val probeGolden = requiredPath(contract, "probe.golden")
            requireFile(probeSource, contract.id)
            requireFile(probeGolden, contract.id)
            if (properties.getProperty("probe.goldenSha256") != probeGolden.sha256()) {
                throw GradleException("${contract.id}: probe golden SHA-256 已变化")
            }
            val probeTestId = properties.getProperty("probe.testId")
            val probeText = probeSource.readText()
            if (!probeText.contains("@PreviewTest") || !probeText.contains("fun $probeTestId(")) {
                throw GradleException("${contract.id}: 找不到对应 probe @PreviewTest $probeTestId")
            }
        }
    }

    private fun requiredPath(
        contract: UiContract,
        key: String,
    ): File = UiContractParser.resolveRepositoryPath(repositoryRoot.get().asFile, contract.id, contract.properties.getProperty(key))

    private fun requireFile(
        file: File,
        contractId: String,
    ) {
        if (!file.isFile) throw GradleException("$contractId: 文件不存在 ${file.relativeTo(repositoryRoot.get().asFile)}")
    }
}

internal fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
