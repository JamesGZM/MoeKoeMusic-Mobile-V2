package cn.james.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.data.local.LocalImportGateway
import cn.james.music.feature.login.TencentCaptchaResult
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var importGateway: Lazy<LocalImportGateway>

    private val themeViewModel: AppThemeViewModel by viewModels()

    private var afterNotificationPermission: (() -> Unit)? = null
    private var afterMediaPermission: ((Boolean) -> Unit)? = null
    private var captchaResultCallback: ((TencentCaptchaResult) -> Unit)? = null
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            afterNotificationPermission?.invoke()
            afterNotificationPermission =
                null
        }
    private val mediaPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            afterMediaPermission?.invoke(granted)
            afterMediaPermission =
                null
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
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicCoverColors by themeViewModel.dynamicCoverColors.collectAsStateWithLifecycle()
            val showLyricsSupplementalText by themeViewModel.showLyricsSupplementalText.collectAsStateWithLifecycle()
            MoeKoeTheme(themeMode = themeMode) {
                MoeKoeApp(
                    dynamicCoverColors = dynamicCoverColors,
                    showLyricsSupplementalText = showLyricsSupplementalText,
                    hasMediaPermission = ::hasMediaPermission,
                    onRequestMediaPermission = ::requestMediaPermission,
                    onImportCandidates = ::enqueueMediaStore,
                    onLaunchTencentCaptcha = ::launchTencentCaptcha,
                    foundationContent = foundationContent(themeMode, themeViewModel::updateTheme),
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

    private fun requestMediaPermission(onResult: (Boolean) -> Unit) {
        val permission = mediaPermissionName()
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            afterMediaPermission = onResult
            mediaPermission.launch(permission)
        } else {
            onResult(true)
        }
    }

    private fun hasMediaPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, mediaPermissionName()) == PackageManager.PERMISSION_GRANTED

    private fun mediaPermissionName(): String =
        if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
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
