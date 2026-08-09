import java.io.File

internal data class DataDrivenUiComponentSpec(
    val name: String,
    val ownerSource: String,
)

internal object ArchitecturePolicy {
    fun validateEdges(edges: Iterable<String>): List<String> =
        edges
            .distinct()
            .sorted()
            .mapNotNull { edge ->
                val (source, target) = edge.split("->", limit = 2).map(String::trim)
                if (isAllowed(source, target)) null else "$source -> $target"
            }

    fun validateImports(
        rootDir: File,
        sourceFiles: Iterable<File>,
    ): List<String> =
        sourceFiles.flatMap { file ->
            val relative = file.relativeTo(rootDir).invariantSeparatorsPath
            val module = moduleOf(relative) ?: return@flatMap emptyList()
            file.readLines().mapIndexedNotNull { index, line ->
                val imported = IMPORT.matchEntire(line.trim())?.groupValues?.get(1) ?: return@mapIndexedNotNull null
                forbiddenReason(module, imported)?.let { reason -> "$relative:${index + 1}: $reason ($imported)" }
            }
        }

    fun validateProductionUiRules(
        rootDir: File,
        sourceFiles: Iterable<File>,
    ): List<String> {
        val sources = sourceFiles.associateBy { it.relativeTo(rootDir).invariantSeparatorsPath }
        return buildList {
            addAll(validateLegacySongRowCalls(sources))
            addAll(validateMoeSongRowStyle(sources[SONG_ROW_OWNER]))
            addAll(validatePreviewUiProperties(sources))
            addAll(validateSearchParallelMaps(sources))
            addAll(validateDataDrivenUiComponents(rootDir, sourceFiles, DATA_DRIVEN_UI_COMPONENTS))
        }
    }

    fun validateSongItems(
        rootDir: File,
        sourceFiles: Iterable<File>,
    ): List<String> = validateProductionUiRules(rootDir, sourceFiles)

    fun validateDataDrivenUiComponents(
        rootDir: File,
        sourceFiles: Iterable<File>,
        components: Iterable<DataDrivenUiComponentSpec>,
    ): List<String> {
        val specs = components.toList()
        val duplicateNames = specs.groupBy(DataDrivenUiComponentSpec::name).filterValues { it.size > 1 }.keys
        require(duplicateNames.isEmpty()) { "数据驱动组件登记重复：${duplicateNames.joinToString()}" }
        return specs.flatMap { spec ->
            sourceFiles.flatMap { file ->
                val relative = file.relativeTo(rootDir).invariantSeparatorsPath
                if (relative == spec.ownerSource) {
                    emptyList()
                } else {
                    findFunctionCalls(file.readText(), spec.name).mapNotNull { call ->
                        val argumentNames = namedArgumentNames(call.arguments)
                        when {
                            call.hasPositionalArguments -> {
                                "$relative: ${spec.name} 只能使用命名参数 model、onEvent、modifier"
                            }

                            argumentNames - DATA_DRIVEN_COMPONENT_ARGUMENTS != emptySet<String>() -> {
                                "$relative: ${spec.name} 只允许 model、onEvent、modifier，实际为 ${argumentNames.joinToString()}"
                            }

                            DATA_DRIVEN_REQUIRED_ARGUMENTS - argumentNames != emptySet<String>() -> {
                                "$relative: ${spec.name} 必须具名传入 ${DATA_DRIVEN_REQUIRED_ARGUMENTS.joinToString()}"
                            }

                            else -> {
                                null
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun validateLegacySongRowCalls(sources: Map<String, File>): List<String> {
        val violations = mutableListOf<String>()
        LEGACY_SONG_ROW_CALLS.forEach { (relative, expected) ->
            val file = sources[relative]
            if (file == null) {
                violations += "$relative: 已冻结的 MoeSongRow 调用文件不存在"
                return@forEach
            }
            val calls = findFunctionCalls(file.readText(), "MoeSongRow")
            if (calls.size != expected.size) {
                violations += "$relative: MoeSongRow 调用数量必须固定为 ${expected.size}，实际为 ${calls.size}"
                return@forEach
            }
            calls.forEachIndexed { index, call ->
                val visualArguments = namedArgumentNames(call.arguments) - SONG_ROW_DATA_ARGUMENTS
                if (visualArguments != expected[index]) {
                    val added = visualArguments - expected[index]
                    val removed = expected[index] - visualArguments
                    violations +=
                        "$relative: 第 ${index + 1} 个 MoeSongRow 视觉参数必须精确冻结，新增 ${added.joinToString()}，删除 ${removed.joinToString()}"
                }
            }
        }
        sources.filterKeys { it != SONG_ROW_OWNER && it !in LEGACY_SONG_ROW_CALLS }.forEach { (relative, file) ->
            if (findFunctionCalls(file.readText(), "MoeSongRow").isNotEmpty()) {
                violations += "$relative: 不允许新增 MoeSongRow 调用点，后续必须先登记为数据驱动组件"
            }
        }
        return violations
    }

    internal fun validateMoeSongRowStyle(owner: File?): List<String> {
        if (owner == null) return listOf("$SONG_ROW_OWNER: MoeSongRowStyle 所有者文件不存在")
        val source = owner.readText()
        val enumMatch = MOE_SONG_ROW_STYLE_ENUM.find(source) ?: return listOf("$SONG_ROW_OWNER: 缺少 MoeSongRowStyle enum")
        val opening = source.indexOf('{', enumMatch.range.last)
        val body = extractDelimited(source, opening, '{', '}') ?: return listOf("$SONG_ROW_OWNER: MoeSongRowStyle enum 未闭合")
        val entries = body.split(',').mapNotNull { segment -> IDENTIFIER.find(segment)?.value }.toSet()
        return if (entries == FROZEN_MOE_SONG_ROW_STYLES) {
            emptyList()
        } else {
            listOf("$SONG_ROW_OWNER: MoeSongRowStyle entry 必须冻结为 ${FROZEN_MOE_SONG_ROW_STYLES.joinToString()}")
        }
    }

    internal fun validatePreviewUiProperties(sources: Map<String, File>): List<String> =
        sources.flatMap { (relative, file) ->
            val actual = uiModelMainConstructorProperties(file.readText(), PREVIEW_PROPERTY)
            val allowed = LEGACY_PREVIEW_PROPERTIES[relative].orEmpty()
            buildList {
                (actual.keys + allowed.keys).sorted().forEach { name ->
                    if (actual[name] != allowed[name]) {
                        add("$relative: preview UI property $name 未在临时 allowlist 中登记")
                    }
                }
            }
        }

    internal fun validateSearchParallelMaps(sources: Map<String, File>): List<String> =
        sources.flatMap { (relative, file) ->
            val actual = uiModelMainConstructorProperties(file.readText(), SEARCH_PARALLEL_MAP)
            val allowed = FROZEN_SEARCH_PARALLEL_MAPS
            buildList {
                (actual.keys + allowed.keys).sorted().forEach { name ->
                    if (actual[name] != allowed[name]) {
                        add("$relative: 并行歌曲 Map $name 仅允许当前 Search 临时登记")
                    }
                }
            }
        }

    private fun uiModelMainConstructorProperties(
        source: String,
        propertyPattern: Regex,
    ): Map<String, Int> {
        val code = maskNonCode(source)
        return UI_MODEL_DATA_CLASS
            .findAll(code)
            .flatMap { match -> extractDelimited(code, match.range.last, '(', ')').orEmpty().let(propertyPattern::findAll) }
            .map { it.groupValues[1] }
            .groupingBy { it }
            .eachCount()
    }

    private fun isAllowed(
        source: String,
        target: String,
    ): Boolean =
        when {
            source == ":app" -> {
                target.startsWith(":feature:") ||
                    target in setOf(":data", ":playback", ":core:designsystem", ":core:model")
            }

            source == ":data" -> {
                target in setOf(":core:model", ":core:database", ":kugou-api", ":playback")
            }

            source == ":playback" -> {
                target == ":core:model"
            }

            source == ":kugou-api" -> {
                target == ":core:common"
            }

            source == ":feature:localmusic" || source == ":feature:foundation" -> {
                target in setOf(":core:model", ":core:designsystem", ":playback")
            }

            source.startsWith(":feature:") -> {
                target in setOf(":core:model", ":core:designsystem")
            }

            source.startsWith(":core:") -> {
                false
            }

            else -> {
                false
            }
        }

    private fun moduleOf(relativePath: String): String? =
        when {
            relativePath.startsWith("app/src/main/") -> ":app"
            relativePath.startsWith("data/src/main/") -> ":data"
            relativePath.startsWith("playback/src/main/") -> ":playback"
            relativePath.startsWith("kugou-api/src/main/") -> ":kugou-api"
            relativePath.startsWith("feature/") -> relativePath.substringAfter("feature/").substringBefore('/').let { ":feature:$it" }
            relativePath.startsWith("core/") -> relativePath.substringAfter("core/").substringBefore('/').let { ":core:$it" }
            else -> null
        }

    private fun forbiddenReason(
        module: String,
        imported: String,
    ): String? {
        if (module.startsWith(":feature:")) {
            if (imported in RAW_VISUAL_PRIMITIVES) return "Feature 必须使用 :core:designsystem 的受控视觉原语"
            if (imported.startsWith("cn.james.music.data.")) return "Feature 不得依赖 data 实现"
            if (imported.startsWith("cn.james.music.kugou.")) return "Feature 不得依赖酷狗协议实现"
            if (imported.startsWith("androidx.room.")) return "Feature 不得直接访问 Room"
            if (imported.startsWith("androidx.datastore.")) return "Feature 不得直接访问 DataStore"
            if (imported.startsWith("androidx.media3.exoplayer.")) return "Feature 不得直接访问 ExoPlayer"
            if (imported.startsWith("io.ktor.") || imported.startsWith("okhttp3.")) return "Feature 不得直接访问网络传输"
            val targetFeature = FEATURE_IMPORT.find(imported)?.groupValues?.get(1)
            val ownFeature = module.substringAfterLast(':')
            if (targetFeature != null && targetFeature != ownFeature) return "Feature 不得依赖其他 Feature 实现"
        }
        if (module in setOf(":data", ":playback", ":kugou-api") && imported.startsWith("cn.james.music.feature.")) {
            return "底层模块不得反向依赖 Feature"
        }
        if (module == ":kugou-api") {
            if (imported.startsWith("androidx.compose.")) return "kugou-api 不得依赖 Compose"
            if (imported.startsWith("androidx.media3.")) return "kugou-api 不得依赖 Media3"
            if (imported == "android.app.Activity" || imported == "android.app.Service") return "kugou-api 不得依赖 Android 页面或 Service"
        }
        return null
    }

    private fun findFunctionCalls(
        source: String,
        functionName: String,
    ): List<FunctionCall> {
        val code = maskNonCode(source)
        val calls = mutableListOf<FunctionCall>()
        var index = 0
        while (index < code.length) {
            val found = code.indexOf(functionName, index)
            if (found < 0) break
            val before = code.getOrNull(found - 1)
            val after = code.getOrNull(found + functionName.length)
            val open = code.indexOfFirstNonWhitespace(found + functionName.length)
            if (before?.isIdentifierPart() != true && after?.isIdentifierPart() != true && code.getOrNull(open) == '(' &&
                !isFunctionDeclaration(code, found)
            ) {
                val arguments = extractDelimited(code, open, '(', ')')
                if (arguments != null) calls += FunctionCall(arguments)
            }
            index = found + functionName.length
        }
        return calls
    }

    private fun namedArgumentNames(arguments: String): Set<String> =
        splitTopLevelArguments(arguments).mapNotNull { argument -> NAMED_ARGUMENT.find(argument)?.groupValues?.get(1) }.toSet()

    private fun splitTopLevelArguments(arguments: String): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        var depth = 0
        arguments.forEachIndexed { index, character ->
            when (character) {
                '(', '{', '[' -> {
                    depth += 1
                }

                ')', '}', ']' -> {
                    depth -= 1
                }

                ',' -> {
                    if (depth == 0) {
                        result += arguments.substring(start, index)
                        start = index + 1
                    }
                }
            }
        }
        result += arguments.substring(start)
        return result.filter(String::isNotBlank)
    }

    private fun extractDelimited(
        source: String,
        openingIndex: Int,
        opening: Char,
        closing: Char,
    ): String? {
        if (source.getOrNull(openingIndex) != opening) return null
        var depth = 0
        for (index in openingIndex until source.length) {
            when (source[index]) {
                opening -> {
                    depth += 1
                }

                closing -> {
                    depth -= 1
                    if (depth == 0) return source.substring(openingIndex + 1, index)
                }
            }
        }
        return null
    }

    private fun maskNonCode(source: String): String {
        val masked = StringBuilder(source.length)
        var index = 0
        while (index < source.length) {
            when {
                source.startsWith("//", index) -> {
                    while (index < source.length && source[index] != '\n') {
                        masked.append(' ')
                        index += 1
                    }
                }

                source.startsWith("/*", index) -> {
                    masked.append("  ")
                    index += 2
                    while (index < source.length && !source.startsWith("*/", index)) {
                        masked.append(if (source[index] == '\n') '\n' else ' ')
                        index += 1
                    }
                    if (index < source.length) {
                        masked.append("  ")
                        index += 2
                    }
                }

                source.startsWith("\"\"\"", index) -> {
                    masked.append("   ")
                    index += 3
                    while (index < source.length && !source.startsWith("\"\"\"", index)) {
                        masked.append(if (source[index] == '\n') '\n' else ' ')
                        index += 1
                    }
                    if (index < source.length) {
                        masked.append("   ")
                        index += 3
                    }
                }

                source[index] == '\"' || source[index] == '\'' -> {
                    val quote = source[index]
                    masked.append(' ')
                    index += 1
                    while (index < source.length) {
                        val character = source[index]
                        masked.append(if (character == '\n') '\n' else ' ')
                        index += 1
                        if (character == '\\' && index < source.length) {
                            masked.append(if (source[index] == '\n') '\n' else ' ')
                            index += 1
                        } else if (character == quote) {
                            break
                        }
                    }
                }

                else -> {
                    masked.append(source[index])
                    index += 1
                }
            }
        }
        return masked.toString()
    }

    private fun String.indexOfFirstNonWhitespace(startIndex: Int): Int {
        var index = startIndex
        while (getOrNull(index)?.isWhitespace() == true) index += 1
        return index
    }

    private fun Char.isIdentifierPart(): Boolean = isLetterOrDigit() || this == '_'

    private fun isFunctionDeclaration(
        source: String,
        functionStart: Int,
    ): Boolean = source.substring(0, functionStart).trimEnd().endsWith("fun")

    private data class FunctionCall(
        val arguments: String,
    ) {
        val hasPositionalArguments: Boolean
            get() = splitTopLevelArguments(arguments).any { NAMED_ARGUMENT.find(it) == null }
    }

    private val IMPORT = Regex("import\\s+([A-Za-z0-9_.*]+)")
    private val FEATURE_IMPORT = Regex("^cn\\.james\\.music\\.feature\\.([^.]+)\\.")
    private val RAW_VISUAL_PRIMITIVES =
        setOf(
            "androidx.compose.material3.HorizontalDivider",
            "androidx.compose.material3.VerticalDivider",
            "androidx.compose.material3.Switch",
        )
    private val NAMED_ARGUMENT = Regex("^\\s*([A-Za-z][A-Za-z0-9_]*)\\s*=")
    private val PREVIEW_PROPERTY = Regex("\\b(?:val|var)\\s+(preview[A-Za-z0-9_]*)\\b")
    private val SEARCH_PARALLEL_MAP = Regex("\\b(?:val|var)\\s+(songBadges|songArtwork)\\b")
    private val UI_MODEL_DATA_CLASS = Regex("data\\s+class\\s+[A-Za-z][A-Za-z0-9_]*(?:Ui|UiState|UiModel)\\s*\\(")
    private val MOE_SONG_ROW_STYLE_ENUM = Regex("enum\\s+class\\s+MoeSongRowStyle\\s*")
    private val IDENTIFIER = Regex("[A-Za-z][A-Za-z0-9_]*")
    private const val SONG_ROW_OWNER =
        "core/designsystem/src/main/kotlin/cn/james/music/core/designsystem/component/MoeSongRow.kt"
    private val FROZEN_MOE_SONG_ROW_STYLES =
        setOf("Standard", "Comfortable", "Compact", "Playlist", "PlaylistCurrent", "Queue", "QueueCurrent")
    private val SONG_ROW_DATA_ARGUMENTS = setOf("title", "subtitle", "metadata", "onClick", "isPlaying")
    private val LEGACY_SONG_ROW_CALLS =
        mapOf(
            "feature/home/src/main/kotlin/cn/james/music/feature/home/HomeSections.kt" to
                listOf(setOf("style", "artwork", "trailing")),
            "feature/search/src/main/kotlin/cn/james/music/feature/search/SearchSongItem.kt" to
                listOf(setOf("modifier", "style", "titleLeading", "artwork", "badges", "trailing")),
            "feature/localmusic/src/main/kotlin/cn/james/music/feature/localmusic/LocalMusicComponents.kt" to
                listOf(setOf("modifier", "style", "showPlayingIndicator", "artwork", "badges", "trailing")),
            "feature/localmusic/src/main/kotlin/cn/james/music/feature/localmusic/DeviceScanScreen.kt" to
                listOf(
                    setOf("modifier", "style", "artwork"),
                    setOf("style", "artwork", "trailing"),
                ),
            "feature/playlist/src/main/kotlin/cn/james/music/feature/playlist/PlaylistTrackItem.kt" to
                listOf(
                    setOf(
                        "modifier",
                        "style",
                        "showPlayingIndicator",
                        "highlightTitleWhenPlaying",
                        "shape",
                        "containerColor",
                        "titleFontWeight",
                        "leading",
                        "artwork",
                        "contentTrailing",
                        "trailing",
                    ),
                ),
            "feature/player/src/main/kotlin/cn/james/music/feature/player/PlayerQueueItem.kt" to
                listOf(
                    setOf(
                        "modifier",
                        "style",
                        "showPlayingIndicator",
                        "highlightTitleWhenPlaying",
                        "shape",
                        "containerColor",
                        "titleColor",
                        "subtitleColor",
                        "metadataColor",
                        "playingColor",
                        "titleFontWeight",
                        "artwork",
                        "artworkTrailing",
                        "trailing",
                    ),
                ),
        )
    private val LEGACY_PREVIEW_PROPERTIES = emptyMap<String, Map<String, Int>>()
    private val FROZEN_SEARCH_PARALLEL_MAPS = emptyMap<String, Int>()
    private val DATA_DRIVEN_UI_COMPONENTS = emptyList<DataDrivenUiComponentSpec>()
    private val DATA_DRIVEN_COMPONENT_ARGUMENTS = setOf("model", "onEvent", "modifier")
    private val DATA_DRIVEN_REQUIRED_ARGUMENTS = setOf("model", "onEvent")
}
