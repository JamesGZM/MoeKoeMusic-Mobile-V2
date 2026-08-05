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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.local.ImportCompletionAction
import cn.james.music.core.model.local.LocalImportSource
import cn.james.music.data.local.LocalImportGateway
import cn.james.music.features.app.MoeKoeAppRoute
import cn.james.music.features.designsystem.FoundationShowcaseRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var importGateway: LocalImportGateway

    private var afterNotificationPermission: (() -> Unit)? = null
    private var afterMediaPermission: (() -> Unit)? = null
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            afterNotificationPermission?.invoke()
            afterNotificationPermission =
                null
        }
    private val mediaPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) afterMediaPermission?.invoke()
            afterMediaPermission =
                null
        }
    private val documentPicker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            uris.forEach { uri -> runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
            if (uris.isNotEmpty()) enqueue(uris, LocalImportSource.DocumentPicker)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }
            MoeKoeTheme(themeMode = themeMode) {
                MoeKoeAppRoute(
                    onChooseFiles = { documentPicker.launch(arrayOf("audio/*")) },
                    onRequestDeviceScan = ::requestMediaPermission,
                    onImportCandidates = ::enqueueMediaStore,
                    openFoundationLab =
                        if (BuildConfig.DEBUG) {
                            { FoundationShowcaseRoute(themeMode, { themeMode = it }) }
                        } else {
                            null
                        },
                )
            }
        }
    }

    private fun enqueue(
        uris: List<Uri>,
        source: LocalImportSource,
    ) {
        withNotificationPermission {
            lifecycleScope.launch { importGateway.enqueue(uris, source, ImportCompletionAction.OpenLocalLibrary) }
        }
    }

    private fun requestMediaPermission(onGranted: () -> Unit) {
        val permission =
            if (Build.VERSION.SDK_INT >=
                33
            ) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            afterMediaPermission = onGranted
            mediaPermission.launch(permission)
        } else {
            onGranted()
        }
    }

    private fun enqueueMediaStore(ids: List<Long>) {
        withNotificationPermission {
            lifecycleScope.launch { importGateway.enqueueMediaStore(ids) }
        }
    }

    private fun withNotificationPermission(block: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            afterNotificationPermission = block
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            block()
        }
    }
}
