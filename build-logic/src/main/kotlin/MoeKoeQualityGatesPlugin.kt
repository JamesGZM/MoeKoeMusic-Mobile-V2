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
    }
}
