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
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.action.MoeButton
import cn.james.music.core.model.local.ImportCompletionAction
import cn.james.music.core.model.local.LocalImportSource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AudioImportActivity : ComponentActivity() {
    private val viewModel: AudioImportViewModel by viewModels()
    internal val message: String get() = viewModel.state.value.message
    private var pendingIntent: Intent? = null
    private var pendingReplacement = false
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            pendingIntent?.let { intent -> handle(intent, replace = pendingReplacement) }
            pendingIntent =
                null
            pendingReplacement = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            MoeKoeTheme {
                Column(
                    Modifier.fillMaxSize().padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("导入到 MoeKoe", style = MaterialTheme.typography.headlineMedium)
                    Text(state.message, Modifier.padding(vertical = 20.dp))
                    if (state.finished) {
                        MoeButton(onClick = {
                            startActivity(Intent(this@AudioImportActivity, MainActivity::class.java))
                            finish()
                        }) { Text("进入 MoeKoe") }
                    }
                }
            }
        }
        handleWithPermission(intent, replace = false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWithPermission(intent, replace = true)
    }

    private fun handleWithPermission(
        intent: Intent,
        replace: Boolean,
    ) {
        if (!intent.hasReadableAudioSource()) {
            handle(intent, replace)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingIntent = intent
            pendingReplacement = replace
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            handle(intent, replace)
        }
    }

    private fun handle(
        intent: Intent,
        replace: Boolean,
    ) {
        val request = intent.toImportRequest()
        if (request == null) return viewModel.rejectInvalidSource()
        if (replace) {
            viewModel.replaceWith(request)
        } else {
            viewModel.resumeOrEnqueue(request)
        }
    }

    private fun Intent.toImportRequest(): ExternalAudioImportRequest? {
        if (!hasReadableAudioSource()) return null
        val isView = action == Intent.ACTION_VIEW
        return ExternalAudioImportRequest(
            uris = audioUris(),
            source = if (isView) LocalImportSource.ExternalView else LocalImportSource.ExternalShare,
            completionAction =
                if (isView) ImportCompletionAction.PlayImportedTrack else ImportCompletionAction.OpenLocalLibrary,
        )
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
}
