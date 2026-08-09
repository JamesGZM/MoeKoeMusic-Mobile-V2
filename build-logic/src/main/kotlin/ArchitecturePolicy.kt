import java.io.File

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

    fun validateSongItems(
        rootDir: File,
        sourceFiles: Iterable<File>,
    ): List<String> =
        sourceFiles.flatMap { file ->
            val relative = file.relativeTo(rootDir).invariantSeparatorsPath
            val source = file.readText()
            SONG_ITEM_FUNCTION
                .findAll(source)
                .mapNotNull { match ->
                    val name = match.groupValues[1]
                    when {
                        name == "MoeSongRow" && relative != SONG_ITEM_OWNER -> {
                            "$relative: $name 只能由 :core:designsystem 定义"
                        }

                        name == "MoeSongRow" -> {
                            null
                        }

                        extractFunctionBody(source, match.range.last + 1)?.contains("MoeSongRow(") != true -> {
                            "$relative: $name 必须委托唯一歌曲母组件 MoeSongRow"
                        }

                        else -> {
                            null
                        }
                    }
                }.toList()
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
            relativePath.startsWith("app/src/main/") -> {
                ":app"
            }

            relativePath.startsWith("data/src/main/") -> {
                ":data"
            }

            relativePath.startsWith("playback/src/main/") -> {
                ":playback"
            }

            relativePath.startsWith("kugou-api/src/main/") -> {
                ":kugou-api"
            }

            relativePath.startsWith("feature/") -> {
                relativePath.substringAfter("feature/").substringBefore('/').let { ":feature:$it" }
            }

            relativePath.startsWith("core/") -> {
                relativePath.substringAfter("core/").substringBefore('/').let { ":core:$it" }
            }

            else -> {
                null
            }
        }

    private fun forbiddenReason(
        module: String,
        imported: String,
    ): String? {
        if (module.startsWith(":feature:")) {
            if (imported in RAW_VISUAL_PRIMITIVES) {
                return "Feature 必须使用 :core:designsystem 的受控视觉原语"
            }
            if (imported.startsWith("cn.james.music.data.")) return "Feature 不得依赖 data 实现"
            if (imported.startsWith("cn.james.music.kugou.")) return "Feature 不得依赖酷狗协议实现"
            if (imported.startsWith("androidx.room.")) return "Feature 不得直接访问 Room"
            if (imported.startsWith("androidx.datastore.")) return "Feature 不得直接访问 DataStore"
            if (imported.startsWith("androidx.media3.exoplayer.")) return "Feature 不得直接访问 ExoPlayer"
            if (imported.startsWith("io.ktor.") || imported.startsWith("okhttp3.")) {
                return "Feature 不得直接访问网络传输"
            }
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
            if (imported == "android.app.Activity" || imported == "android.app.Service") {
                return "kugou-api 不得依赖 Android 页面或 Service"
            }
        }
        return null
    }

    private fun extractFunctionBody(
        source: String,
        fromIndex: Int,
    ): String? {
        val opening = source.indexOf('{', fromIndex).takeIf { it >= 0 } ?: return null
        var depth = 0
        for (index in opening until source.length) {
            when (source[index]) {
                '{' -> {
                    depth += 1
                }

                '}' -> {
                    depth -= 1
                    if (depth == 0) return source.substring(opening, index + 1)
                }
            }
        }
        return null
    }

    private val IMPORT = Regex("import\\s+([A-Za-z0-9_.*]+)")
    private val FEATURE_IMPORT = Regex("^cn\\.james\\.music\\.feature\\.([^.]+)\\.")
    private val RAW_VISUAL_PRIMITIVES =
        setOf(
            "androidx.compose.material3.HorizontalDivider",
            "androidx.compose.material3.VerticalDivider",
            "androidx.compose.material3.Switch",
        )
    private val SONG_ITEM_FUNCTION = Regex("fun\\s+([A-Za-z0-9_]*(?:Song|Track|Queue|Music)[A-Za-z0-9_]*(?:Row|Item))\\s*\\(")
    private const val SONG_ITEM_OWNER =
        "core/designsystem/src/main/kotlin/cn/james/music/core/designsystem/component/MoeSongRow.kt"
}
