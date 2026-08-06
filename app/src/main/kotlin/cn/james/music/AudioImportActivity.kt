package cn.james.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.model.local.ImportCompletionAction
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalImportError
import cn.james.music.core.model.local.LocalImportSource
import cn.james.music.core.model.local.LocalMusicRepository
import cn.james.music.data.local.LocalImportGateway
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AudioImportActivity : ComponentActivity() {
    @Inject lateinit var gateway: LocalImportGateway

    @Inject lateinit var repository: LocalMusicRepository
    internal var message by mutableStateOf("正在准备导入…")
    private var finished by mutableStateOf(false)
    private var pendingIntent: Intent? = null
    private var importObservation: Job? = null
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            pendingIntent?.let(::handle)
            pendingIntent =
                null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoeKoeTheme {
                Column(
                    Modifier.fillMaxSize().padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("导入到 MoeKoe", style = MaterialTheme.typography.headlineMedium)
                    Text(message, Modifier.padding(vertical = 20.dp))
                    if (finished) {
                        Button(onClick = {
                            startActivity(Intent(this@AudioImportActivity, MainActivity::class.java))
                            finish()
                        }) { Text("进入 MoeKoe") }
                    }
                }
            }
        }
        handleWithPermission(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWithPermission(intent)
    }

    private fun handleWithPermission(intent: Intent) {
        if (!intent.hasReadableAudioSource()) {
            handle(intent)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingIntent = intent
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            handle(intent)
        }
    }

    private fun handle(intent: Intent) {
        val uris = intent.audioUris()
        if (!intent.hasReadableAudioSource()) {
            importObservation?.cancel()
            message = "无法读取这个音频来源"
            finished = true
            return
        }
        val isView = intent.action == Intent.ACTION_VIEW
        importObservation?.cancel()
        message = "正在准备导入…"
        finished = false
        importObservation =
            lifecycleScope.launch {
                val batchId =
                    gateway.enqueue(
                        uris,
                        if (isView) LocalImportSource.ExternalView else LocalImportSource.ExternalShare,
                        if (isView) ImportCompletionAction.PlayImportedTrack else ImportCompletionAction.OpenLocalLibrary,
                    )
                repository.observeImports().first { rows ->
                    val progress = rows.firstOrNull { it.batchId == batchId } ?: return@first false
                    message = progress.toUserMessage()
                    (progress.state in terminalStates).also { isTerminal ->
                        if (isTerminal) finished = true
                    }
                }
            }
    }

    @Suppress("DEPRECATION")
    private fun Intent.audioUris(): List<Uri> =
        when (action) {
            Intent.ACTION_VIEW -> {
                listOfNotNull(data)
            }

            Intent.ACTION_SEND -> {
                listOfNotNull(
                    if (Build.VERSION.SDK_INT >=
                        33
                    ) {
                        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        getParcelableExtra(Intent.EXTRA_STREAM)
                    },
                )
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                if (Build.VERSION.SDK_INT >=
                    33
                ) {
                    getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
                } else {
                    getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
                }
            }

            else -> {
                emptyList()
            }
        }

    private fun Intent.hasReadableAudioSource(): Boolean {
        val uris = audioUris()
        return type?.startsWith("audio/") == true && uris.isNotEmpty() && uris.all { it.scheme == "content" }
    }

    private companion object {
        val terminalStates =
            setOf(
                LocalImportBatchState.Completed,
                LocalImportBatchState.PartiallyCompleted,
                LocalImportBatchState.Failed,
                LocalImportBatchState.Cancelled,
            )
    }
}

private fun cn.james.music.core.model.local.LocalImportProgress.toUserMessage(): String =
    when (state) {
        LocalImportBatchState.Queued -> {
            "已加入导入队列"
        }

        LocalImportBatchState.Running -> {
            currentDisplayName?.let { "正在导入 $it · $completedCount/$totalCount" }
                ?: "正在导入 · $completedCount/$totalCount"
        }

        LocalImportBatchState.Completed -> {
            "导入完成 · $completedCount/$totalCount"
        }

        LocalImportBatchState.PartiallyCompleted -> {
            "部分导入成功 · 成功 $completedCount · 失败 $failedCount"
        }

        LocalImportBatchState.Failed -> {
            error.toImportFailureMessage()
        }

        LocalImportBatchState.Cancelled -> {
            "导入已取消 · 已保留 $completedCount 首"
        }
    }

private fun LocalImportError?.toImportFailureMessage(): String =
    when (this) {
        LocalImportError.SourceUnavailable -> "无法读取这个音频来源"
        LocalImportError.UnsupportedFormat -> "文件不是受支持的音频格式"
        LocalImportError.InsufficientSpace -> "存储空间不足，无法完成导入"
        LocalImportError.StorageUnavailable -> "App 音乐目录暂时不可用"
        LocalImportError.PermissionDenied -> "没有读取这个文件的权限"
        LocalImportError.CorruptFile -> "音频文件已损坏或无法解析"
        LocalImportError.Cancelled -> "导入已取消"
        LocalImportError.Unknown, null -> "导入失败，请重新选择文件"
    }
