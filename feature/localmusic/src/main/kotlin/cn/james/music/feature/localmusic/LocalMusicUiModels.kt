package cn.james.music.feature.localmusic

import androidx.compose.runtime.Immutable

@JvmInline
internal value class LocalArtworkStorageRef(
    val key: String,
) {
    init {
        require(key.isNotBlank()) { "Artwork storage key must not be blank" }
        require(!key.startsWith('/') && ".." !in key.split('/')) {
            "Artwork storage key must be app-relative"
        }
    }
}

@Immutable
internal sealed interface LocalArtworkUiModel {
    data object None : LocalArtworkUiModel

    data class AppStorage(
        val ref: LocalArtworkStorageRef,
    ) : LocalArtworkUiModel
}

@Immutable
internal data class LocalSongRowUiModel(
    val id: String,
    val title: String,
    val artist: String,
    val durationLabel: String,
    val artwork: LocalArtworkUiModel,
    val isPlaying: Boolean,
)

@Immutable
internal data class LocalImportProgressUiModel(
    val id: String,
    val completedCount: Int,
    val totalCount: Int,
)

internal enum class LocalMusicSortUi {
    Newest,
    Title,
    Artist,
    Duration,
}

internal enum class LocalMusicContentUi {
    EmptyLibrary,
    NoResults,
    Songs,
}

@Immutable
internal sealed interface DeviceCandidateArtistUi {
    data class Known(
        val value: String,
    ) : DeviceCandidateArtistUi

    data object Unknown : DeviceCandidateArtistUi
}

@Immutable
internal data class DeviceCandidateRowUiModel(
    val id: Long,
    val title: String,
    val artist: DeviceCandidateArtistUi,
    val durationLabel: String,
)

internal sealed interface DeviceImportUiState {
    data object PermissionRequired : DeviceImportUiState

    data class Scanning(
        val candidates: List<DeviceCandidateRowUiModel>,
    ) : DeviceImportUiState

    data class Selection(
        val candidates: List<DeviceCandidateRowUiModel>,
        val selectedIds: Set<Long> = emptySet(),
    ) : DeviceImportUiState

    data object Failed : DeviceImportUiState
}

@Immutable
internal data class LocalMusicUiState(
    val songs: List<LocalSongRowUiModel> = emptyList(),
    val content: LocalMusicContentUi = LocalMusicContentUi.EmptyLibrary,
    val activeImport: LocalImportProgressUiModel? = null,
    val deviceImport: DeviceImportUiState = DeviceImportUiState.PermissionRequired,
    val query: String = "",
    val sort: LocalMusicSortUi = LocalMusicSortUi.Newest,
    val playingSongId: String? = null,
    val pendingDeleteSongId: String? = null,
)

internal sealed interface LocalMusicAction {
    data object Back : LocalMusicAction

    data object OpenImporter : LocalMusicAction

    data class QueryChanged(
        val value: String,
    ) : LocalMusicAction

    data class SelectSort(
        val sort: LocalMusicSortUi,
    ) : LocalMusicAction

    data class PlaySong(
        val id: String,
    ) : LocalMusicAction

    data class RequestDeleteSong(
        val id: String,
    ) : LocalMusicAction

    data class ConfirmDeleteSong(
        val id: String,
    ) : LocalMusicAction

    data object DismissDeleteSong : LocalMusicAction

    data class CancelImport(
        val id: String,
    ) : LocalMusicAction
}

internal sealed interface DeviceImportAction {
    data class Open(
        val hasPermission: Boolean,
    ) : DeviceImportAction

    data object Back : DeviceImportAction

    data object RequestPermission : DeviceImportAction

    data class PermissionResult(
        val granted: Boolean,
    ) : DeviceImportAction

    data object Retry : DeviceImportAction

    data class ToggleCandidate(
        val id: Long,
    ) : DeviceImportAction

    data object ToggleAll : DeviceImportAction

    data class Import(
        val ids: List<Long>,
    ) : DeviceImportAction

    data object Cancel : DeviceImportAction
}
