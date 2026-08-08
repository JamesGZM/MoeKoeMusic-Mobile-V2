import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.util.Properties

@DisableCachingByDefault(because = "查询外部 Git 上游 HEAD")
abstract class CheckAgentUpstreamUpdatesTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val lockFile: RegularFileProperty

    @TaskAction
    fun check() {
        val properties =
            Properties().apply {
                lockFile
                    .get()
                    .asFile
                    .reader(Charsets.UTF_8)
                    .use(::load)
            }
        listOf("androidSkills", "composeSkill").forEach { name ->
            val repository = properties.getProperty("$name.repository") ?: throw GradleException("缺少 $name.repository")
            val pinned = properties.getProperty("$name.commit") ?: throw GradleException("缺少 $name.commit")
            val process = ProcessBuilder("git", "ls-remote", repository, "HEAD").redirectErrorStream(true).start()
            val output =
                process.inputStream
                    .bufferedReader()
                    .readText()
                    .trim()
            if (process.waitFor() != 0) throw GradleException("无法查询 $name：$output")
            val latest = output.substringBefore('\t')
            if (latest == pinned) {
                logger.lifecycle("$name: 已是固定上游 HEAD $pinned")
            } else {
                logger.lifecycle("$name: 发现上游候选 $latest；当前仍固定 $pinned，必须审计和人工批准后更新。")
            }
        }
    }
}
