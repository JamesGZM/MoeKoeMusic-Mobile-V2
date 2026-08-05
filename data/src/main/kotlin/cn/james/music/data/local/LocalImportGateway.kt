package cn.james.music.data.local

import android.net.Uri
import cn.james.music.core.model.local.ImportCompletionAction
import cn.james.music.core.model.local.LocalImportSource

interface LocalImportGateway {
    suspend fun enqueue(
        uris: List<Uri>,
        source: LocalImportSource,
        completionAction: ImportCompletionAction,
    ): String

    suspend fun enqueueMediaStore(mediaStoreIds: List<Long>): String
}
