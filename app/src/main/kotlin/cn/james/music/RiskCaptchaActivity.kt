package cn.james.music

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.feature.login.TencentCaptchaResult
import java.io.ByteArrayInputStream
import java.net.URI
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RiskCaptchaActivity : ComponentActivity() {
    private var captchaWebView: WebView? = null
    private var resultDelivered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appId = intent.getStringExtra(EXTRA_APP_ID)
        if (!isValidAppId(appId)) {
            setContent { MoeKoeTheme {} }
            window.decorView.post(::finishWithFailure)
            return
        }
        val captchaAppId = requireNotNull(appId)
        setContent {
            MoeKoeTheme { CaptchaContent(captchaAppId) }
        }
    }

    @Composable
    private fun CaptchaContent(appId: String) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
        ) {
            TextButton(onClick = ::finishCancelled, modifier = Modifier.fillMaxWidth()) {
                Text(getString(R.string.risk_captcha_cancel))
            }
            AndroidView(
                factory = { context -> createCaptchaWebView(context, appId) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Suppress("SetJavaScriptEnabled", "DEPRECATION")
    private fun createCaptchaWebView(
        context: Context,
        appId: String,
    ): WebView =
        WebView(context).also { webView ->
            captchaWebView = webView
            webView.setBackgroundColor(Color.TRANSPARENT)
            with(webView.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                databaseEnabled = false
                saveFormData = false
                cacheMode = WebSettings.LOAD_NO_CACHE
                mediaPlaybackRequiresUserGesture = true
            }
            webView.setDownloadListener { _, _, _, _, _ -> finishWithFailure() }
            webView.webViewClient = SecureCaptchaWebViewClient(::finishWithFailure)
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                finishWithFailure()
                return@also
            }
            WebViewCompat.addWebMessageListener(
                webView,
                MESSAGE_OBJECT,
                setOf(ALLOWED_ORIGIN),
            ) { _, message, sourceOrigin, isMainFrame, _ ->
                if (!isMainFrame || !CaptchaNavigationPolicy.isAllowed(sourceOrigin)) {
                    finishWithFailure()
                    return@addWebMessageListener
                }
                when (val result = TencentCaptchaPayloadParser.parse(message.data ?: "")) {
                    is TencentCaptchaResult.Success -> finishWithSuccess(result)
                    TencentCaptchaResult.Cancelled -> finishCancelled()
                    TencentCaptchaResult.Failure -> finishWithFailure()
                }
            }
            webView.loadDataWithBaseURL(
                BASE_URL,
                captchaHtml(appId),
                "text/html",
                Charsets.UTF_8.name(),
                null,
            )
        }

    override fun onDestroy() {
        captchaWebView?.run {
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }
        captchaWebView = null
        super.onDestroy()
    }

    private fun finishWithSuccess(result: TencentCaptchaResult.Success) {
        if (resultDelivered) return
        resultDelivered = true
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(EXTRA_TICKET, result.ticket)
                .putExtra(EXTRA_RANDOM_STRING, result.randomString),
        )
        finish()
    }

    private fun finishCancelled() {
        if (resultDelivered) return
        resultDelivered = true
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun finishWithFailure() {
        if (resultDelivered) return
        resultDelivered = true
        setResult(Activity.RESULT_FIRST_USER)
        finish()
    }

    companion object {
        private const val EXTRA_APP_ID = "risk_app_id"
        private const val EXTRA_TICKET = "risk_ticket"
        private const val EXTRA_RANDOM_STRING = "risk_random_string"
        private const val MESSAGE_OBJECT = "MoeKoeCaptcha"
        internal const val CAPTCHA_HOST = "turing.captcha.qcloud.com"
        internal const val ALLOWED_ORIGIN = "https://$CAPTCHA_HOST"
        private const val BASE_URL = "$ALLOWED_ORIGIN/"
        private val APP_ID_PATTERN = Regex("^[0-9]{6,20}$")

        fun intent(context: Context, appId: String): Intent = Intent(context, RiskCaptchaActivity::class.java).putExtra(EXTRA_APP_ID, appId)

        fun parseResult(resultCode: Int, data: Intent?): TencentCaptchaResult =
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val ticket = data?.getStringExtra(EXTRA_TICKET)
                    val randomString = data?.getStringExtra(EXTRA_RANDOM_STRING)
                    if (ticket.isNullOrBlank() || randomString.isNullOrBlank()) {
                        TencentCaptchaResult.Failure
                    } else {
                        TencentCaptchaResult.Success(ticket, randomString)
                    }
                }
                Activity.RESULT_CANCELED -> TencentCaptchaResult.Cancelled
                else -> TencentCaptchaResult.Failure
            }

        internal fun isValidAppId(appId: String?): Boolean = appId != null && APP_ID_PATTERN.matches(appId)

        internal fun captchaHtml(appId: String): String {
            require(isValidAppId(appId)) { "Invalid captcha app id" }
            return """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline' $ALLOWED_ORIGIN; connect-src $ALLOWED_ORIGIN; img-src data: $ALLOWED_ORIGIN; style-src 'unsafe-inline' $ALLOWED_ORIGIN; frame-src $ALLOWED_ORIGIN"><script src="$ALLOWED_ORIGIN/TCaptcha.js"></script></head><body><script>const captcha=new TencentCaptcha('$appId',function(result){MoeKoeCaptcha.postMessage(JSON.stringify(result));},{type:'',showHeader:false});captcha.show();</script></body></html>"""
        }
    }
}

internal object CaptchaNavigationPolicy {
    fun isAllowed(uri: Uri?): Boolean = isAllowed(uri?.toString())

    fun isAllowed(rawUri: String?): Boolean =
        runCatching {
            val uri = URI(rawUri ?: return false)
            uri.scheme == "https" &&
                uri.host == RiskCaptchaActivity.CAPTCHA_HOST &&
                uri.userInfo == null &&
                (uri.port == -1 || uri.port == 443)
        }.getOrDefault(false)
}

internal object TencentCaptchaPayloadParser {
    fun parse(payload: String): TencentCaptchaResult =
        runCatching {
            val value = Json.parseToJsonElement(payload).jsonObject
            when (value["ret"]?.jsonPrimitive?.intOrNull) {
                0 -> {
                    val ticket = value["ticket"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it.length <= 4096 }
                    val randomString = value["randstr"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it.length <= 512 }
                    if (ticket == null || randomString == null) TencentCaptchaResult.Failure else TencentCaptchaResult.Success(ticket, randomString)
                }
                2 -> TencentCaptchaResult.Cancelled
                else -> TencentCaptchaResult.Failure
            }
        }.getOrDefault(TencentCaptchaResult.Failure)
}

private class SecureCaptchaWebViewClient(
    private val onFailure: () -> Unit,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
        !CaptchaNavigationPolicy.isAllowed(request?.url)

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? =
        if (CaptchaNavigationPolicy.isAllowed(request?.url)) {
            null
        } else {
            WebResourceResponse("text/plain", Charsets.UTF_8.name(), 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
        }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
        handler?.cancel()
        onFailure()
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        if (request?.isForMainFrame == true) onFailure()
    }
}
