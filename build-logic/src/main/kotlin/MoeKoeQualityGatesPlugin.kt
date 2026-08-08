import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

class MoeKoeQualityGatesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) { "moekoe.quality-gates 只能应用于根项目" }

        val architecture =
            target.tasks.register("verifyArchitecture", VerifyArchitectureTask::class.java) {
                group = "verification"
                description = "校验 MoeKoe 模块依赖方向和生产源码 import 边界。"
                repositoryRoot.set(target.layout.projectDirectory)
                sourceFiles.from(
                    target.fileTree(target.rootDir) {
                        include("app/src/main/**/*.kt", "core/*/src/main/**/*.kt", "data/src/main/**/*.kt")
                        include("feature/*/src/main/**/*.kt", "kugou-api/src/main/**/*.kt", "playback/src/main/**/*.kt")
                        exclude("**/build/**")
                    },
                )
            }
        target.gradle.projectsEvaluated {
            val edges =
                target.rootProject.allprojects
                    .flatMap { project ->
                        project.configurations.flatMap { configuration ->
                            configuration.dependencies
                                .withType(ProjectDependency::class.java)
                                .filter { dependency -> dependency.path != project.path }
                                .map { dependency -> "${project.path} -> ${dependency.path}" }
                        }
                    }.distinct()
                    .sorted()
            architecture.configure { projectEdges.set(edges) }
        }

        target.tasks.register("verifyUiContracts", VerifyUiContractsTask::class.java) {
            group = "verification"
            description = "校验已确认设计、PreviewTest 和 UI contract 的可追溯关系。"
            repositoryRoot.set(target.layout.projectDirectory)
            contractFiles.from(target.fileTree("docs/design/contracts") { include("*.properties") })
        }

        val selectedContractId = target.providers.gradleProperty("moekoe.uiContract")
        val uiEvidenceDirectory = target.layout.buildDirectory.dir("reports/ui-evidence")
        val contracts = target.fileTree("docs/design/contracts") { include("*.properties") }
        val generateEvidence =
            target.tasks.register("generateUiEvidence", GenerateUiEvidenceTask::class.java) {
                group = "verification"
                description = "归一化设计稿与 Compose 渲染并生成叠加、差异和量化结果。"
                repositoryRoot.set(target.layout.projectDirectory)
                contractFiles.from(contracts)
                evidenceSourceFiles.from(uiEvidenceInputs(target, contracts))
                selectedContract.set(selectedContractId)
                outputDirectory.set(uiEvidenceDirectory)
            }
        target.gradle.projectsEvaluated {
            val selectedId = selectedContractId.orNull
            val modules =
                contracts.files
                    .map(UiContractParser::parse)
                    .filter { selectedId == null || it.id == selectedId }
                    .map { it.properties.getProperty("module") }
                    .distinct()
            generateEvidence.configure {
                modules.forEach { module ->
                    target
                        .project(module)
                        .tasks
                        .findByName("validateDebugScreenshotTest")
                        ?.let { dependsOn(it) }
                }
            }
        }
        val verifyFidelity =
            target.tasks.register("verifyUiFidelity", VerifyUiFidelityTask::class.java) {
                group = "verification"
                description = "按 UI contract 阈值阻断不符合确认稿的 Compose 渲染。"
                dependsOn(generateEvidence)
                dependsOn(target.tasks.named("verifyUiContracts"))
                repositoryRoot.set(target.layout.projectDirectory)
                contractFiles.from(contracts)
                selectedContract.set(selectedContractId)
                evidenceFiles.from(target.fileTree(uiEvidenceDirectory) { include("*/result.properties") })
                evidenceSourceFiles.from(uiEvidenceInputs(target, contracts))
            }
        target.tasks.register("verifyUiGoldenChange", VerifyUiGoldenChangeTask::class.java) {
            group = "verification"
            description = "阻止没有设计符合度证据的 screenshot reference 更新。"
            dependsOn(verifyFidelity)
            repositoryRoot.set(target.layout.projectDirectory)
            contractFiles.from(contracts)
            evidenceRoot.set(uiEvidenceDirectory)
            target.providers.gradleProperty("moekoe.changedFilesFile").orNull?.let { path ->
                changedFilesFile.set(target.layout.projectDirectory.file(path))
            }
        }
        target.tasks.register("verifySkillGovernance", VerifySkillGovernanceTask::class.java) {
            group = "verification"
            description = "校验 skill incident、隔离 eval 和人工批准边界。"
            repositoryRoot.set(target.layout.projectDirectory)
            incidentFiles.from(target.fileTree("docs/quality/incidents") { include("*.properties") })
            evalFiles.from(target.fileTree(".agents/evals") { include("*.properties") })
            skillFiles.from(target.fileTree(".agents/skills") { include("*/SKILL.md", "*/agents/openai.yaml") })
        }
        target.tasks.register("checkAgentUpstreamUpdates", CheckAgentUpstreamUpdatesTask::class.java) {
            group = "help"
            description = "只读检查已固定 Android/Compose skill 上游是否出现新 HEAD。"
            lockFile.set(target.layout.projectDirectory.file(".agents/upstreams/agent-skills.properties"))
        }
    }

    private fun uiEvidenceInputs(
        target: Project,
        contracts: org.gradle.api.file.FileTree,
    ) = target.files(
        target.provider {
            contracts.files.flatMap { file ->
                val contract = UiContractParser.parse(file)
                listOfNotNull(
                    contract.file,
                    uiEvidenceInput(target, contract, "design.path"),
                    uiEvidenceInput(target, contract, "screenshot.source"),
                    uiEvidenceInput(target, contract, "screenshot.rendered"),
                    if (UiContractParser.hasProbe(contract.properties)) uiEvidenceInput(target, contract, "probe.source") else null,
                    if (UiContractParser.hasProbe(contract.properties)) uiEvidenceInput(target, contract, "probe.rendered") else null,
                )
            }
        },
    )

    private fun uiEvidenceInput(
        target: Project,
        contract: UiContract,
        key: String,
    ) = UiContractParser.resolveRepositoryPath(target.rootDir, contract.id, contract.properties.getProperty(key))
}
