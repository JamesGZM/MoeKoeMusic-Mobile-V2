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

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val skillFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val root = repositoryRoot.get().asFile
        val evals = evalFiles.files.associateBy { it.nameWithoutExtension }
        val failures = mutableListOf<String>()
        verifySkillMetadata(root, failures)
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
            evals[properties.getProperty("evalCase")]?.let { evalFile ->
                failures += verifyAssertionLink(file.name, properties, evalFile.loadProperties())
            }
            if (properties.getProperty("status") == "resolved") {
                listOf("approvedBy", "resolvedCommit").forEach { key ->
                    if (properties.getProperty(key).isNullOrBlank()) failures += "${file.name}: resolved 缺少 $key"
                }
            }
        }
        if (failures.isNotEmpty()) throw GradleException("Skill 治理门禁失败：\n${failures.joinToString("\n") { "- $it" }}")
        logger.lifecycle("Skill 治理门禁通过：${incidentFiles.files.size} 个 incident，${evals.size} 个 eval。")
    }

    private fun verifySkillMetadata(
        root: File,
        failures: MutableList<String>,
    ) {
        val skillsRoot = File(root, ".agents/skills")
        skillsRoot.listFiles()?.filter { File(it, "SKILL.md").isFile }?.sortedBy(File::getName)?.forEach { skillDirectory ->
            val skillName = skillDirectory.name
            val metadata = File(skillDirectory, "agents/openai.yaml")
            if (!metadata.isFile) {
                failures += "$skillName: 缺少 agents/openai.yaml"
                return@forEach
            }
            val values =
                metadata
                    .readLines()
                    .mapNotNull { line ->
                        val match = METADATA_LINE.matchEntire(line) ?: return@mapNotNull null
                        match.groupValues[1] to match.groupValues[2].trim().removeSurrounding("\"")
                    }.toMap()
            METADATA_FIELDS.forEach { field ->
                if (values[field].isNullOrBlank()) failures += "$skillName: openai.yaml 缺少 $field"
            }
            val prompt = values["default_prompt"]
            if (!prompt.isNullOrBlank() && "$$skillName" !in prompt) {
                failures += "$skillName: default_prompt 必须包含 \$$skillName"
            }
            if (!prompt.isNullOrBlank() && prompt.none { it in '\u4e00'..'\u9fff' }) {
                failures += "$skillName: default_prompt 必须包含中文说明"
            }
        }
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
        val METADATA_FIELDS = setOf("display_name", "short_description", "default_prompt")
        val METADATA_LINE = Regex("^\\s{2}(display_name|short_description|default_prompt):\\s*(.+?)\\s*$")
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

internal fun verifyAssertionLink(
    incidentName: String,
    incident: Properties,
    evaluation: Properties,
): List<String> {
    val incidentIds = incident.csvValues("assertionIds")
    if (incidentIds.isEmpty()) return emptyList()
    val failures = mutableListOf<String>()
    val evaluationIds = evaluation.csvValues("assertionIds")
    if (incidentIds.size != incidentIds.toSet().size || incidentIds.any { !ASSERTION_ID.matches(it) }) {
        failures += "$incidentName: assertionIds 必须唯一且使用稳定 ID"
    }
    if (evaluationIds != incidentIds) failures += "$incidentName: eval assertionIds 必须与 incident 完全一致"
    val assertions =
        evaluation
            .getProperty("assertions")
            .orEmpty()
            .split(';')
            .map(String::trim)
            .filter(String::isNotEmpty)
    if (assertions.size != incidentIds.size) failures += "$incidentName: eval assertions 数量必须与 assertionIds 一致"
    return failures
}

private fun Properties.csvValues(key: String): List<String> =
    getProperty(key)
        .orEmpty()
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)

private val ASSERTION_ID = Regex("[a-z][a-z0-9-]*")
