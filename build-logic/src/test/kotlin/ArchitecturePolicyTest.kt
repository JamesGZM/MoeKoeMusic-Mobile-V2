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

    @Test
    fun `Feature 必须使用受控分隔线和 Switch`() {
        val root = createTempDirectory("moekoe-visual-primitives-").toFile()
        val source = File(root, "feature/settings/src/main/kotlin/SettingsScreen.kt")
        source.parentFile.mkdirs()
        source.writeText(
            """
            import androidx.compose.material3.HorizontalDivider
            import androidx.compose.material3.Switch
            """.trimIndent(),
        )

        val violations = ArchitecturePolicy.validateImports(root, listOf(source))

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("受控视觉原语") })
    }

    @Test
    fun `歌曲型页面适配器必须委托唯一母组件`() {
        val root = createTempDirectory("moekoe-song-item-").toFile()
        val valid = File(root, "feature/search/src/main/kotlin/SearchScreen.kt")
        val invalid = File(root, "feature/playlist/src/main/kotlin/PlaylistScreen.kt")
        valid.parentFile.mkdirs()
        invalid.parentFile.mkdirs()
        valid.writeText("fun SearchSongRow() { MoeSongRow(title = \"fixture\") }")
        invalid.writeText("fun PlaylistTrackRow() { Row { } }")

        val violations = ArchitecturePolicy.validateSongItems(root, listOf(valid, invalid))

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("PlaylistTrackRow"))
    }
}
