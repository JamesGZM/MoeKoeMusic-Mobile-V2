package cn.james.music.feature.discover

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun WeeklyHero(
    @DrawableRes artworkRes: Int,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            Modifier
                .padding(horizontal = DiscoverDimensions.horizontalPadding, vertical = 12.dp)
                .then(modifier)
                .fillMaxWidth()
                .height(215.dp)
                .clip(RoundedCornerShape(14.dp))
                .semantics { contentDescription = "discover-weekly-hero" },
    ) {
        Image(
            painter = painterResource(artworkRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier =
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Color.White.copy(alpha = 0.96f),
                        0.42f to Color.White.copy(alpha = 0.72f),
                        0.68f to Color.Transparent,
                    ),
                ),
        )
        Column(
            modifier = Modifier.align(Alignment.CenterStart).offset(y = 12.dp).padding(start = 18.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.discover_weekly_title),
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF17191C),
            )
            Text(
                text = stringResource(R.string.discover_weekly_subtitle),
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 13.sp,
                color = Color(0xFF5F6368),
            )
            DiscoverHeroButton(onClick = onPlay)
        }
    }
}

@Composable
private fun DiscoverHeroButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .height(48.dp)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.height(34.dp),
            shape = RoundedCornerShape(11.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.discover_listen_now), fontSize = 13.sp)
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 3.dp).size(16.dp),
                )
            }
        }
    }
}
