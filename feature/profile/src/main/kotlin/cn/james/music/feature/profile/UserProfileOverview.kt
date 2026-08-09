package cn.james.music.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.component.MoePassiveOutline
import cn.james.music.core.designsystem.component.MoeVerticalDivider

@Composable
internal fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 15.dp, bottom = 8.dp)
                .testTag(USER_PROFILE_OVERVIEW_TITLE_TAG)
                .userProfileBoundsProbe(PROBE_OVERVIEW_TITLE_TOP, PROBE_OVERVIEW_TITLE_BOTTOM),
    )
}

@Composable
internal fun ListeningOverview(profile: UserProfileUi) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .userProfileLayoutProbe(PROBE_OVERVIEW),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = MoePassiveOutline(),
    ) {
        if (largeText) {
            Column(Modifier.padding(vertical = 10.dp)) {
                OverviewItem(profile.levelLabel, null, stringResource(R.string.profile_level), Modifier.fillMaxWidth())
                OverviewItem(
                    profile.listeningDurationValue,
                    stringResource(R.string.profile_hours_unit),
                    stringResource(R.string.profile_listening_duration),
                    Modifier.fillMaxWidth(),
                )
                OverviewItem(
                    profile.musicAgeValue,
                    stringResource(R.string.profile_years_unit),
                    stringResource(R.string.profile_music_age),
                    Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(86.dp).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OverviewItem(profile.levelLabel, null, stringResource(R.string.profile_level), Modifier.weight(1f))
                MoeVerticalDivider(Modifier.height(38.dp))
                OverviewItem(
                    profile.listeningDurationValue,
                    stringResource(R.string.profile_hours_unit),
                    stringResource(R.string.profile_listening_duration),
                    Modifier.weight(1f),
                )
                MoeVerticalDivider(Modifier.height(38.dp))
                OverviewItem(
                    profile.musicAgeValue,
                    stringResource(R.string.profile_years_unit),
                    stringResource(R.string.profile_music_age),
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OverviewItem(
    value: String,
    unit: String?,
    label: String,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alignByBaseline(),
            )
            unit?.let {
                Spacer(Modifier.width(3.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
