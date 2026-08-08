package cn.james.music.feature.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun UserProfileContent(
    profile: UserProfileUi,
    onAction: (UserProfileAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(USER_PROFILE_CONTENT_TAG),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item(key = "hero") { ProfileHero(profile = profile, onAction = onAction) }
        item(key = "overview-title") { ProfileSectionTitle(stringResource(R.string.profile_listening_overview)) }
        item(key = "overview") { ListeningOverview(profile) }
        userProfilePlaylistSection(playlists = profile.playlists, onAction = onAction)
    }
}
