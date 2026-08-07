package cn.james.music.core.designsystem.component.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import cn.james.music.core.designsystem.R

/** The approved rounded chevron used for navigating to the previous destination. */
@Composable
fun MoeNavigateBackIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(R.drawable.ic_moe_navigation_back),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}
