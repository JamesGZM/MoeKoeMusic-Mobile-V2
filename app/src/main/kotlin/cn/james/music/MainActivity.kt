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
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.local.ImportCompletionAction
import cn.james.music.core.model.local.LocalImportSource
import cn.james.music.data.local.LocalImportGateway
import cn.james.music.feature.login.TencentCaptchaResult
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var importGateway: Lazy<LocalImportGateway>

    private var afterNotificationPermission: (() -> Unit)? = null
    private var afterMediaPermission: (() -> Unit)? = null
    private var captchaResultCallback: ((TencentCaptchaResult) -> Unit)? = null
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
    private val captchaLauncher =
        registerForActivityResult(
            object : ActivityResultContract<String, TencentCaptchaResult>() {
                override fun createIntent(
                    context: android.content.Context,
                    input: String,
                ): Intent = RiskCaptchaActivity.intent(context, input)

                override fun parseResult(
                    resultCode: Int,
                    intent: Intent?,
                ): TencentCaptchaResult = RiskCaptchaActivity.parseResult(resultCode, intent)
            },
        ) { result ->
            captchaResultCallback?.invoke(result)
            captchaResultCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }
            MoeKoeTheme(themeMode = themeMode) {
                MoeKoeApp(
                    onChooseFiles = { documentPicker.launch(arrayOf("audio/*")) },
                    onRequestDeviceScan = ::requestMediaPermission,
                    onImportCandidates = ::enqueueMediaStore,
                    onLaunchTencentCaptcha = ::launchTencentCaptcha,
                    foundationContent = foundationContent(themeMode) { themeMode = it },
                )
            }
        }
    }

    private fun launchTencentCaptcha(
        appId: String,
        onResult: (TencentCaptchaResult) -> Unit,
    ) {
        captchaResultCallback?.invoke(TencentCaptchaResult.Failure)
        captchaResultCallback = onResult
        captchaLauncher.launch(appId)
    }

    private fun enqueue(
        uris: List<Uri>,
        source: LocalImportSource,
    ) {
        withNotificationPermission {
            lifecycleScope.launch { importGateway.get().enqueue(uris, source, ImportCompletionAction.OpenLocalLibrary) }
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
            lifecycleScope.launch { importGateway.get().enqueueMediaStore(ids) }
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
