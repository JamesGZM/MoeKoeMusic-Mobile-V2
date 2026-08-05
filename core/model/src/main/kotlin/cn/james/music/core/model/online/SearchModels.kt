package cn.james.music.core.model.online

data class Song(
    val id: String,
    val hash: String,
    val title: String,
    val artistName: String,
    val albumId: String?,
    val albumTitle: String?,
    val durationMs: Long,
    val artworkUrl: String?,
)

data class SearchPage(
    val items: List<Song>,
    val page: Int,
    val totalCount: Int,
    val hasMore: Boolean,
)

sealed interface SearchError {
    data object Offline : SearchError

    data object Timeout : SearchError

    data object Connection : SearchError

    data object VerificationRequired : SearchError

    data object AuthenticationRequired : SearchError

    data object ServiceUnavailable : SearchError

    data object Protocol : SearchError

    data object SessionInitialization : SearchError
}

sealed interface SearchResult {
    data class Success(
        val page: SearchPage,
    ) : SearchResult

    data class Failure(
        val error: SearchError,
    ) : SearchResult
}

interface SearchRepository {
    suspend fun searchSongs(
        keyword: String,
        page: Int,
        pageSize: Int = 30,
    ): SearchResult
}
