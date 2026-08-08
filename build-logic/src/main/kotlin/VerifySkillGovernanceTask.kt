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
import java.util.Properties

abstract class VerifySkillGovernanceTask : DefaultTask() {
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val incidentFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val evalFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val root = repositoryRoot.get().asFile
        val evals = evalFiles.files.associateBy { it.nameWithoutExtension }
        val failures = mutableListOf<String>()
        evals.forEach { (id, file) ->
            val properties = file.loadProperties()
            requireFields(file, properties, EVAL_FIELDS, failures)
            if (properties.getProperty("id") != id) failures += "${file.name}: id 必须与文件名一致"
            val skill = File(root, ".agents/skills/${properties.getProperty("skill")}/SKILL.md")
            if (!skill.isFile) failures += "${file.name}: skill 不存在 ${properties.getProperty("skill")}"
        }
        incidentFiles.files.forEach { file ->
            val properties = file.loadProperties()
            requireFields(file, properties, INCIDENT_FIELDS, failures)
            if (properties.getProperty("id") != file.nameWithoutExtension) failures += "${file.name}: id 必须与文件名一致"
            if (properties.getProperty("evalCase") !in evals) failures += "${file.name}: evalCase 不存在"
            if (properties.getProperty("status") == "resolved") {
                listOf("approvedBy", "resolvedCommit").forEach { key ->
                    if (properties.getProperty(key).isNullOrBlank()) failures += "${file.name}: resolved 缺少 $key"
                }
            }
        }
        if (failures.isNotEmpty()) throw GradleException("Skill 治理门禁失败：\n${failures.joinToString("\n") { "- $it" }}")
        logger.lifecycle("Skill 治理门禁通过：${incidentFiles.files.size} 个 incident，${evals.size} 个 eval。")
    }

    private fun requireFields(
        file: File,
        properties: Properties,
        fields: Set<String>,
        failures: MutableList<String>,
    ) {
        val missing = fields.filter { properties.getProperty(it).isNullOrBlank() }
        if (missing.isNotEmpty()) failures += "${file.name}: 缺少 ${missing.joinToString()}"
        if (properties.getProperty("schemaVersion") != "1") failures += "${file.name}: schemaVersion 必须为 1"
    }

    private fun File.loadProperties(): Properties = Properties().apply { reader(Charsets.UTF_8).use(::load) }

    private companion object {
        val INCIDENT_FIELDS =
            setOf(
                "schemaVersion",
                "id",
                "status",
                "detectedAt",
                "taskType",
                "affectedSkill",
                "missedBy",
                "evidence",
                "rootCause",
                "candidateRule",
                "expectedAssertion",
                "evalCase",
                "owner",
            )
        val EVAL_FIELDS = setOf("schemaVersion", "id", "skill", "kind", "task", "fixture", "assertions", "status")
    }
}
