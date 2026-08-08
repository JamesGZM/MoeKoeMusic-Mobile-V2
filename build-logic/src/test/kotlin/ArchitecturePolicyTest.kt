import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ArchitecturePolicyTest {
    @Test
    fun `允许当前模块依赖和明确播放例外`() {
        val violations =
            ArchitecturePolicy.validateEdges(
                listOf(
                    ":app -> :feature:home",
                    ":data -> :playback",
                    ":feature:localmusic -> :playback",
                    ":feature:foundation -> :playback",
                    ":feature:login -> :core:model",
                ),
            )

        assertTrue(violations.isEmpty())
    }

    @Test
    fun `拒绝 Feature 之间和 Feature 到 data 的依赖`() {
        val violations =
            ArchitecturePolicy.validateEdges(
                listOf(
                    ":feature:home -> :feature:login",
                    ":feature:home -> :data",
                ),
            )

        assertEquals(listOf(":feature:home -> :data", ":feature:home -> :feature:login"), violations)
    }

    @Test
    fun `拒绝 Feature 直接导入网络和数据库`() {
        val root = createTempDirectory("moekoe-architecture-").toFile()
        val source = File(root, "feature/home/src/main/kotlin/HomeScreen.kt")
        source.parentFile.mkdirs()
        source.writeText("import androidx.room.Room\nimport io.ktor.client.HttpClient\n")

        val violations = ArchitecturePolicy.validateImports(root, listOf(source))

        assertEquals(2, violations.size)
        assertTrue(violations.any { it.contains("Room") })
        assertTrue(violations.any { it.contains("网络传输") })
    }
}
