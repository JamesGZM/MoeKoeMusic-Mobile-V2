package cn.james.music.feature.login

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
internal fun QrCodeImage(
    layout: LoginLayoutSpec,
    loginUrl: String,
    showCenterBadge: Boolean = true,
) {
    val bitmap = remember(loginUrl) { createQrBitmap(loginUrl) }
    Box(
        modifier =
            Modifier
                .size(layout.qr.codeFrameSize)
                .background(Color.White, RoundedCornerShape(layout.qr.codeFrameRadius))
                .border(
                    BorderStroke(layout.dp(2f), MaterialTheme.colorScheme.outlineVariant),
                    RoundedCornerShape(layout.qr.codeFrameRadius),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.login_qr_code_description),
            modifier = Modifier.size(layout.qr.codeImageSize),
        )
        if (showCenterBadge) {
            Surface(
                modifier = Modifier.size(layout.qr.codeBadgeSize),
                shape = RoundedCornerShape(layout.qr.codeBadgeRadius),
                color = MaterialTheme.colorScheme.primary,
                border = BorderStroke(layout.dp(5f), Color.White),
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(layout.dp(16f)).size(layout.qr.codeBadgeIconSize),
                )
            }
        }
    }
}

private fun createQrBitmap(content: String): Bitmap {
    val matrix =
        QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            QR_BITMAP_SIZE,
            QR_BITMAP_SIZE,
            mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1,
            ),
        )
    val pixels =
        IntArray(QR_BITMAP_SIZE * QR_BITMAP_SIZE) { index ->
            if (matrix[index % QR_BITMAP_SIZE, index / QR_BITMAP_SIZE]) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
        }
    return createBitmap(QR_BITMAP_SIZE, QR_BITMAP_SIZE).apply {
        setPixels(pixels, 0, QR_BITMAP_SIZE, 0, 0, QR_BITMAP_SIZE, QR_BITMAP_SIZE)
    }
}

private const val QR_BITMAP_SIZE = 512
