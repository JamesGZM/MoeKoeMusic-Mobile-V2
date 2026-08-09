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
                    ":feature:player -> :core:designsystem",
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
                    ":feature:player -> :core:model",
                ),
            )

        assertEquals(listOf(":feature:home -> :data", ":feature:home -> :feature:login", ":feature:player -> :core:model"), violations)
    }

    @Test
    fun `拒绝 Feature 直接导入网络和数据库`() {
        val root = createTempDirectory("moekoe-architecture-").toFile()
        val source =
            writeSource(root, "feature/home/src/main/kotlin/HomeScreen.kt", "import androidx.room.Room\nimport io.ktor.client.HttpClient\n")

        val violations = ArchitecturePolicy.validateImports(root, listOf(source))

        assertEquals(2, violations.size)
        assertTrue(violations.any { it.contains("Room") })
        assertTrue(violations.any { it.contains("网络传输") })
    }

    @Test
    fun `Feature 必须使用受控分隔线和 Switch`() {
        val root = createTempDirectory("moekoe-visual-primitives-").toFile()
        val source =
            writeSource(
                root,
                "feature/settings/src/main/kotlin/SettingsScreen.kt",
                "import androidx.compose.material3.HorizontalDivider\nimport androidx.compose.material3.Switch\n",
            )

        val violations = ArchitecturePolicy.validateImports(root, listOf(source))

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("受控视觉原语") })
    }

    @Test
    fun `Player 生产源码只允许纯 UI model 且禁止视觉字段`() {
        val root = createTempDirectory("moekoe-player-boundary-").toFile()
        val valid =
            writeSource(
                root,
                "feature/player/src/main/kotlin/cn/james/music/feature/player/PlayerModels.kt",
                "@JvmInline value class PlayerArtworkStorageRef(val key: String)\n" +
                    "data class AppStorage(val ref: PlayerArtworkStorageRef)\n" +
                    "data class PlayerArtworkUiModel(val storage: AppStorage)\n" +
                    "data class PlayerItemUiModel(val id: String, val artwork: PlayerArtworkUiModel)",
            )

        assertTrue(ArchitecturePolicy.validateImports(root, listOf(valid)).isEmpty())
        assertTrue(ArchitecturePolicy.validatePlayerFeatureBoundary(root, listOf(valid)).isEmpty())

        val invalidImports =
            writeSource(
                root,
                "feature/player/src/main/kotlin/cn/james/music/feature/player/LeakingPlayer.kt",
                "import cn.james.music.core.model.playback.PlaybackItem\nimport cn.james.music.playback.PlaybackController\nimport java.io.File\nimport android.net.Uri\n",
            )
        assertEquals(4, ArchitecturePolicy.validateImports(root, listOf(invalidImports)).size)

        val invalidModel =
            writeSource(
                root,
                "feature/player/src/main/kotlin/cn/james/music/feature/player/LeakingPlayerModel.kt",
                "@JvmInline value class LeakingStorageRef(val relativePath: String)\n" +
                    "data class AppStorage(val artworkPath: String)\n" +
                    "data class PlayerUiModel(val item: PlaybackItem, val padding: Dp, val tint: Color, val previewArtwork: String)",
            )
        val violations = ArchitecturePolicy.validatePlayerFeatureBoundary(root, listOf(invalidModel))
        assertTrue(violations.any { it.contains("PlaybackItem") })
        assertTrue(violations.any { it.contains("视觉布局") })
        assertTrue(violations.any { it.contains("relativePath") })
        assertTrue(violations.any { it.contains("artworkPath") })
    }

    @Test
    fun `Settings UI model 构造器拒绝领域类型但允许纯语义类型`() {
        val root = createTempDirectory("moekoe-settings-ui-model-").toFile()
        val legal =
            writeSource(
                root,
                "feature/settings/src/main/kotlin/cn/james/music/feature/settings/SettingsUiModels.kt",
                "data class SettingsUiState(val theme: SettingsThemeUi, val problem: SettingsProblemUi?)",
            )
        assertTrue(ArchitecturePolicy.validateFeatureEntryModels(root, listOf(legal)).isEmpty())

        legal.writeText("data class SettingsUiState(val theme: AppThemePreference, val problem: AppSettingsProblem?)")
        val violations = ArchitecturePolicy.validateFeatureEntryModels(root, listOf(legal))
        assertTrue(violations.any { it.contains("AppThemePreference") })
        assertTrue(violations.any { it.contains("AppSettingsProblem") })
    }

    @Test
    fun `My UI model 拒绝领域类型和八个平行资产字段`() {
        val root = createTempDirectory("moekoe-my-ui-model-").toFile()
        val legal =
            writeSource(
                root,
                "feature/my/src/main/kotlin/cn/james/music/feature/my/MyUiModels.kt",
                "data class MyUiState(val problem: MyProfileProblemUi?)\ndata class MyLibraryUi(val quickEntries: List<MyEntryUi>)",
            )
        assertTrue(ArchitecturePolicy.validateFeatureEntryModels(root, listOf(legal)).isEmpty())

        legal.writeText(
            "data class MyUiState(val profile: UserProfile, val error: AuthError?)\n" +
                "data class MyLibraryUi(val likedCount: String?, val followedFriendCount: String?)",
        )
        val violations = ArchitecturePolicy.validateFeatureEntryModels(root, listOf(legal))
        assertTrue(violations.any { it.contains("UserProfile") })
        assertTrue(violations.any { it.contains("AuthError") })
        assertTrue(violations.any { it.contains("likedCount") })
        assertTrue(violations.any { it.contains("followedFriendCount") })
    }

    @Test
    fun `Discover 拒绝平行图片列表和 index 选择状态`() {
        val root = createTempDirectory("moekoe-discover-selection-").toFile()
        val legal =
            writeSource(
                root,
                "feature/discover/src/main/kotlin/cn/james/music/feature/discover/DiscoverModels.kt",
                "data class DiscoverContentUi(val playlists: List<DiscoverPlaylistCardUi>)\nfun Screen(selectedTabId: String, selectedCategoryId: String) = Unit",
            )
        assertTrue(ArchitecturePolicy.validateFeatureEntryModels(root, listOf(legal)).isEmpty())

        legal.writeText(
            "data class DiscoverContentUi(val categoryArtworkRes: List<Int>)\n" +
                "fun Screen(selectedTab: Int, selectedCategory: Int) = Unit",
        )
        val violations = ArchitecturePolicy.validateFeatureEntryModels(root, listOf(legal))
        assertTrue(violations.any { it.contains("categoryArtworkRes") })
        assertEquals(2, violations.count { it.contains("stable id") })
    }

    @Test
    fun `Discover UI model 拒绝视觉和布局字段`() {
        val root = createTempDirectory("moekoe-discover-visual-model-").toFile()
        val legal =
            writeSource(
                root,
                "feature/discover/src/main/kotlin/cn/james/music/feature/discover/DiscoverModels.kt",
                "data class DiscoverRankingUi(val id: String, val tone: DiscoverRankingToneUi)",
            )
        assertTrue(ArchitecturePolicy.validateFeatureEntryModels(root, listOf(legal)).isEmpty())

        legal.writeText("data class DiscoverRankingUi(val badge: Color, val itemPadding: Dp, val layoutMode: String)")
        val violations = ArchitecturePolicy.validateFeatureEntryModels(root, listOf(legal))
        assertTrue(violations.any { it.contains("Color") })
        assertTrue(violations.any { it.contains("Dp") })
        assertTrue(violations.any { it.contains("layoutMode") })
    }

    @Test
    fun `三组 Feature 展示模型必须声明 Immutable`() {
        val root = createTempDirectory("moekoe-immutable-feature-model-").toFile()
        val settings =
            writeSource(
                root,
                "feature/settings/src/main/kotlin/cn/james/music/feature/settings/SettingsUiModels.kt",
                "@Immutable\ninternal data class SettingsUiState(val theme: String)",
            )
        val my =
            writeSource(
                root,
                "feature/my/src/main/kotlin/cn/james/music/feature/my/MyUiModels.kt",
                "@Immutable\ninternal sealed interface MyAccountUiState",
            )
        val discover =
            writeSource(
                root,
                "feature/discover/src/main/kotlin/cn/james/music/feature/discover/DiscoverModels.kt",
                "@Immutable\ninternal enum class DiscoverRankingToneUi { Rising }\ninternal sealed interface DiscoverAction",
            )

        assertTrue(ArchitecturePolicy.validateImmutableFeatureModels(root, listOf(settings, my, discover)).isEmpty())

        discover.writeText("internal data class DiscoverContentUi(val id: String)")
        assertTrue(
            ArchitecturePolicy
                .validateImmutableFeatureModels(root, listOf(settings, my, discover))
                .single()
                .contains("DiscoverContentUi"),
        )
    }

    @Test
    fun `Discover 默认分类拒绝由 index 生成 stable id`() {
        val root = createTempDirectory("moekoe-discover-semantic-id-").toFile()
        val source =
            writeSource(
                root,
                "feature/discover/src/main/kotlin/cn/james/music/feature/discover/DiscoverModels.kt",
                "val discoverDefaultContent = listOf(DiscoverCategoryUi(\"pop\", \"流行\"))",
            )
        assertTrue(ArchitecturePolicy.validateFeatureEntryModels(root, listOf(source)).isEmpty())

        source.writeText(
            "val discoverDefaultContent = labels.mapIndexed { index, label -> DiscoverCategoryUi(\"category-\$index\", label) }",
        )
        assertTrue(ArchitecturePolicy.validateFeatureEntryModels(root, listOf(source)).single().contains("mapIndexed"))
    }

    @Test
    fun `My Navigation 禁止临时拼接两组 entry`() {
        val root = createTempDirectory("moekoe-my-entry-resolution-").toFile()
        val source =
            writeSource(
                root,
                "feature/my/src/main/kotlin/cn/james/music/feature/my/MyNavigation.kt",
                "fun route() = state.library.actionFor(id)",
            )
        assertTrue(ArchitecturePolicy.validateFeatureEntryModels(root, listOf(source)).isEmpty())

        source.writeText("fun route() = (state.library.quickEntries + state.library.collectionEntries).firstOrNull()")
        assertTrue(ArchitecturePolicy.validateFeatureEntryModels(root, listOf(source)).single().contains("join"))
    }

    @Test
    fun `legacy MoeSongRow 路径数量与视觉参数必须精确冻结`() {
        val root = createTempDirectory("moekoe-legacy-song-row-").toFile()
        val path = "feature/home/src/main/kotlin/cn/james/music/feature/home/HomeSections.kt"
        val valid =
            writeSource(root, path, "fun HomeSongRow() { MoeSongRow(style = value, artwork = {}, trailing = {}) }")

        assertTrue(ArchitecturePolicy.validateLegacySongRowCalls(legacySources(root, path, valid)).isEmpty())

        valid.writeText("fun HomeSongRow() = Unit")
        assertTrue(ArchitecturePolicy.validateLegacySongRowCalls(legacySources(root, path, valid)).single().contains("调用数量"))

        valid.writeText("fun HomeSongRow() { MoeSongRow(artwork = {}, trailing = {}) }")
        assertTrue(ArchitecturePolicy.validateLegacySongRowCalls(legacySources(root, path, valid)).single().contains("删除 style"))

        valid.writeText("fun HomeSongRow() { MoeSongRow(style = value, artwork = {}, trailing = {}, shape = value) }")
        assertTrue(ArchitecturePolicy.validateLegacySongRowCalls(legacySources(root, path, valid)).single().contains("新增 shape"))

        valid.writeText("fun HomeSongRow() { MoeSongRow(style = value, artwork = {}, trailing = {}, unregisteredSlot = {}) }")
        assertTrue(
            ArchitecturePolicy
                .validateLegacySongRowCalls(legacySources(root, path, valid))
                .single()
                .contains("新增 unregisteredSlot"),
        )

        val newPath = "feature/discover/src/main/kotlin/cn/james/music/feature/discover/DiscoverSongRow.kt"
        val newCall = writeSource(root, newPath, "fun DiscoverSongRow() { MoeSongRow(title = value) }")
        assertTrue(
            ArchitecturePolicy
                .validateLegacySongRowCalls(legacySources(root) + (newPath to newCall))
                .single()
                .contains("不允许新增"),
        )
    }

    @Test
    fun `冻结 MoeSongRowStyle enum entry`() {
        val root = createTempDirectory("moekoe-song-style-").toFile()
        val valid =
            writeSource(
                root,
                "core/designsystem/src/main/kotlin/cn/james/music/core/designsystem/component/MoeSongRow.kt",
                "enum class MoeSongRowStyle { Standard, Comfortable, Compact, Playlist, PlaylistCurrent, Queue, QueueCurrent }",
            )

        assertTrue(ArchitecturePolicy.validateMoeSongRowStyle(valid).isEmpty())

        valid.writeText(
            "enum class MoeSongRowStyle { Standard, Comfortable, Compact, Playlist, PlaylistCurrent, Queue, QueueCurrent, Experimental }",
        )
        assertTrue(ArchitecturePolicy.validateMoeSongRowStyle(valid).single().contains("entry"))
    }

    @Test
    fun `preview UI property 不再允许临时登记`() {
        val root = createTempDirectory("moekoe-preview-property-").toFile()
        val path = "feature/home/src/main/kotlin/cn/james/music/feature/home/HomeViewModel.kt"
        val valid = writeSource(root, path, "data class HomeContentUi(val artwork: String)")

        assertTrue(ArchitecturePolicy.validatePreviewUiProperties(mapOf(path to valid)).isEmpty())

        val invalid =
            writeSource(
                root,
                path,
                "data class HomeContentUi(val previewArtworkRes: Int?, val previewBadge: String?, val previewBadgeIsError: Boolean, val previewArtworkRes: Int?, val previewSubtitle: String?)",
            )

        assertTrue(ArchitecturePolicy.validatePreviewUiProperties(mapOf(path to invalid)).any { it.contains("previewArtworkRes") })

        invalid.writeText("fun mapper() { val previewUrl = value }\ndata class ProtocolPreview(val previewSong: String)")
        assertTrue(ArchitecturePolicy.validatePreviewUiProperties(mapOf(path to invalid)).isEmpty())
    }

    @Test
    fun `Search 并行 Map 不再允许临时登记`() {
        val root = createTempDirectory("moekoe-search-parallel-map-").toFile()
        val path = "feature/search/src/main/kotlin/cn/james/music/feature/search/SearchViewModel.kt"
        val valid = writeSource(root, path, "data class SearchUiState(val songs: List<String>)")

        assertTrue(ArchitecturePolicy.validateSearchParallelMaps(mapOf(path to valid)).isEmpty())

        val invalid =
            writeSource(
                root,
                path,
                "data class SearchUiState(val songBadges: Map<String, String>, val songArtwork: Map<String, Int>)",
            )

        assertTrue(ArchitecturePolicy.validateSearchParallelMaps(mapOf(path to invalid)).any { it.contains("songBadges") })

        invalid.writeText(
            "fun mapper() { val songBadges = emptyMap<String, String>() }\ndata class SearchCache(val songArtwork: Map<String, Int>)",
        )
        assertTrue(ArchitecturePolicy.validateSearchParallelMaps(mapOf(path to invalid)).isEmpty())
    }

    @Test
    fun `数据驱动组件要求具名 model onEvent 且 modifier 可选`() {
        val root = createTempDirectory("moekoe-data-driven-component-").toFile()
        val owner =
            writeSource(
                root,
                "core/designsystem/src/main/kotlin/MoeDataSongRow.kt",
                "fun MoeDataSongRow(model: Any, onEvent: () -> Unit, modifier: Any) = Unit",
            )
        val valid =
            writeSource(
                root,
                "feature/home/src/main/kotlin/Home.kt",
                "fun Screen() { MoeDataSongRow(model = model, onEvent = {}, modifier = modifier) }",
            )
        val spec = DataDrivenUiComponentSpec("MoeDataSongRow", owner.relativeTo(root).invariantSeparatorsPath)

        assertTrue(ArchitecturePolicy.validateDataDrivenUiComponents(root, listOf(owner, valid), listOf(spec)).isEmpty())

        valid.writeText("fun Screen() { MoeDataSongRow(model = model, onEvent = {}) }")
        assertTrue(ArchitecturePolicy.validateDataDrivenUiComponents(root, listOf(owner, valid), listOf(spec)).isEmpty())

        valid.writeText("fun Screen() { MoeDataSongRow(model = model, onEvent = {}, trailing = {}) }")
        assertTrue(ArchitecturePolicy.validateDataDrivenUiComponents(root, listOf(owner, valid), listOf(spec)).single().contains("只允许"))

        valid.writeText("fun Screen() { MoeDataSongRow(model, onEvent = {}) }")
        assertTrue(ArchitecturePolicy.validateDataDrivenUiComponents(root, listOf(owner, valid), listOf(spec)).single().contains("命名参数"))

        valid.writeText("fun Screen() { MoeDataSongRow(onEvent = {}) }")
        assertTrue(ArchitecturePolicy.validateDataDrivenUiComponents(root, listOf(owner, valid), listOf(spec)).single().contains("model"))

        valid.writeText("fun Screen() { MoeDataSongRow(model = model) }")
        assertTrue(ArchitecturePolicy.validateDataDrivenUiComponents(root, listOf(owner, valid), listOf(spec)).single().contains("onEvent"))
    }

    @Test
    fun `PlayerQueueSheet 登记后只接受 model onEvent 和可选 modifier`() {
        val root = createTempDirectory("moekoe-player-queue-component-").toFile()
        val owner =
            writeSource(
                root,
                "feature/player/src/main/kotlin/cn/james/music/feature/player/PlayerQueueSheet.kt",
                "fun PlayerQueueSheet(model: Any, onEvent: (Any) -> Unit, modifier: Any) = Unit",
            )
        val consumer =
            writeSource(
                root,
                "app/src/main/kotlin/cn/james/music/PlaybackShell.kt",
                "fun Queue() { PlayerQueueSheet(model = model, onEvent = onEvent, modifier = modifier) }",
            )
        val spec = DataDrivenUiComponentSpec("PlayerQueueSheet", owner.relativeTo(root).invariantSeparatorsPath)

        assertTrue(ArchitecturePolicy.validateDataDrivenUiComponents(root, listOf(owner, consumer), listOf(spec)).isEmpty())

        consumer.writeText("fun Queue() { PlayerQueueSheet(model, onEvent = onEvent) }")
        assertTrue(ArchitecturePolicy.validateDataDrivenUiComponents(root, listOf(owner, consumer), listOf(spec)).single().contains("命名参数"))

        consumer.writeText("fun Queue() { PlayerQueueSheet(model = model, onDismiss = {}) }")
        val extraCallback = ArchitecturePolicy.validateDataDrivenUiComponents(root, listOf(owner, consumer), listOf(spec))
        assertTrue(extraCallback.any { it.contains("只允许") })
        assertTrue(extraCallback.any { it.contains("onEvent") })

        consumer.writeText("fun Queue() { PlayerQueueSheet(onEvent = onEvent) }")
        assertTrue(
            ArchitecturePolicy.validateDataDrivenUiComponents(root, listOf(owner, consumer), listOf(spec)).single().contains("model"),
        )
    }

    @Test
    fun `非歌曲行 MoeMediaBadge 不受冻结规则误伤`() {
        val root = createTempDirectory("moekoe-non-song-badge-").toFile()
        val path = "feature/search/src/main/kotlin/cn/james/music/feature/search/SearchArtistHero.kt"
        val source = writeSource(root, path, "fun SearchArtistHero() { MoeMediaBadge(text = badge) }")
        assertTrue(ArchitecturePolicy.validateLegacySongRowCalls(legacySources(root) + (path to source)).isEmpty())
    }

    private fun writeSource(
        root: File,
        path: String,
        text: String,
    ): File =
        File(root, path).apply {
            parentFile.mkdirs()
            writeText(text)
        }

    private fun legacySources(
        root: File,
        overridePath: String? = null,
        overrideFile: File? = null,
    ): Map<String, File> {
        val calls =
            mapOf(
                "feature/home/src/main/kotlin/cn/james/music/feature/home/HomeSections.kt" to
                    listOf("MoeSongRow(style = value, artwork = {}, trailing = {})"),
                "feature/search/src/main/kotlin/cn/james/music/feature/search/SearchSongItem.kt" to
                    listOf("MoeSongRow(modifier = value, style = value, titleLeading = {}, artwork = {}, badges = {}, trailing = {})"),
                "feature/localmusic/src/main/kotlin/cn/james/music/feature/localmusic/LocalMusicComponents.kt" to
                    listOf(
                        "MoeSongRow(modifier = value, style = value, showPlayingIndicator = value, artwork = {}, badges = {}, trailing = {})",
                    ),
                "feature/localmusic/src/main/kotlin/cn/james/music/feature/localmusic/DeviceScanScreen.kt" to
                    listOf(
                        "MoeSongRow(modifier = value, style = value, artwork = {})",
                        "MoeSongRow(style = value, artwork = {}, trailing = {})",
                    ),
                "feature/playlist/src/main/kotlin/cn/james/music/feature/playlist/PlaylistTrackItem.kt" to
                    listOf(
                        "MoeSongRow(modifier = value, style = value, showPlayingIndicator = value, highlightTitleWhenPlaying = value, shape = value, containerColor = value, titleFontWeight = value, leading = {}, artwork = {}, contentTrailing = {}, trailing = {})",
                    ),
                "feature/player/src/main/kotlin/cn/james/music/feature/player/PlayerQueueItem.kt" to
                    listOf(
                        "MoeSongRow(modifier = value, style = value, showPlayingIndicator = value, highlightTitleWhenPlaying = value, shape = value, containerColor = value, titleColor = value, subtitleColor = value, metadataColor = value, playingColor = value, titleFontWeight = value, artwork = {}, artworkTrailing = {}, trailing = {})",
                    ),
            )
        return calls.mapValues { (path, sourceCalls) ->
            if (path == overridePath) requireNotNull(overrideFile) else writeSource(root, path, sourceCalls.joinToString(separator = "\n"))
        }
    }
}
