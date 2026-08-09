package cn.james.music.feature.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage

@Composable
internal fun PlayerArtworkRenderer(
    model: PlayerArtworkUiModel,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    when (model) {
        PlayerArtworkUiModel.None -> Unit
        is PlayerArtworkUiModel.Remote ->
            AsyncImage(
                model = model.httpsUrl,
                contentDescription = contentDescription,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

        is PlayerArtworkUiModel.AppStorage ->
            AsyncImage(
                model = LocalContext.current.filesDir.resolve(model.ref.key),
                contentDescription = contentDescription,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

        is PlayerArtworkUiModel.BundledResource ->
            Image(
                painter = painterResource(model.resourceId),
                contentDescription = contentDescription,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
    }
}
