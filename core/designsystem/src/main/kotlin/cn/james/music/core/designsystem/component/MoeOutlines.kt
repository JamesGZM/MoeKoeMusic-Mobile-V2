package cn.james.music.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class MoePassiveOutlineEmphasis {
    Subtle,
    Standard,
}

@Composable
fun MoePassiveOutline(
    emphasis: MoePassiveOutlineEmphasis = MoePassiveOutlineEmphasis.Subtle,
): BorderStroke {
    val alpha =
        when (emphasis) {
            MoePassiveOutlineEmphasis.Subtle -> 0.42f
            MoePassiveOutlineEmphasis.Standard -> 0.72f
        }
    return BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))
}
