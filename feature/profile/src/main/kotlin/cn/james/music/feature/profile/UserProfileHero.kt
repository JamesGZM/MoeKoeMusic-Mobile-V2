package cn.james.music.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeVerticalDivider

@Composable
internal fun ProfileHero(
    profile: UserProfileUi,
    onAction: (UserProfileAction) -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 3.dp)
                .userProfileLayoutProbe(PROBE_HERO),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    ),
                ),
            ),
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(104.dp),
            )
            Column(Modifier.padding(start = 16.dp, top = 27.dp, end = 16.dp, bottom = 9.dp)) {
                if (largeText) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        ProfileAvatar(profile)
                        ProfileIdentity(profile, Modifier.fillMaxWidth().padding(top = 12.dp))
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        ProfileAvatar(profile)
                        ProfileIdentity(profile, Modifier.weight(1f).padding(start = 16.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                ProfileRelations(profile = profile, onAction = onAction)
                Spacer(Modifier.height(9.dp))
                EditProfileButton(onClick = { onAction(UserProfileAction.Edit) })
            }
        }
    }
}

@Composable
private fun EditProfileButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(USER_PROFILE_EDIT_TOUCH_TAG)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag(USER_PROFILE_EDIT_VISUAL_TAG)
                    .userProfileBoundsProbe(PROBE_EDIT_TOP, PROBE_EDIT_BOTTOM),
            shape = RoundedCornerShape(9.dp),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.profile_edit),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(profile: UserProfileUi) {
    Surface(
        modifier = Modifier.size(84.dp),
        shape = CircleShape,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Image(
            painter = painterResource(profile.avatarRes),
            contentDescription = stringResource(R.string.profile_avatar),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
    }
}

@Composable
private fun ProfileIdentity(
    profile: UserProfileUi,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = profile.nickname,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            profile.vipLabel?.let { ProfileBadge(it, vip = true) }
            ProfileBadge(profile.levelLabel, vip = false)
        }
        Text(
            text = profile.signature,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileBadge(
    label: String,
    vip: Boolean,
) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (vip) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MoeKoeTheme.extraColors.vipGold,
                )
                Spacer(Modifier.width(3.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ProfileRelations(
    profile: UserProfileUi,
    onAction: (UserProfileAction) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically) {
        RelationItem(
            profile.followingCount,
            stringResource(R.string.profile_following),
            { onAction(UserProfileAction.Following) },
            Modifier.weight(1f),
        )
        MoeVerticalDivider(Modifier.height(34.dp))
        RelationItem(
            profile.followerCount,
            stringResource(R.string.profile_followers),
            { onAction(UserProfileAction.Followers) },
            Modifier.weight(1f),
        )
        MoeVerticalDivider(Modifier.height(34.dp))
        RelationItem(
            profile.friendCount,
            stringResource(R.string.profile_friends),
            { onAction(UserProfileAction.Friends) },
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun RelationItem(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
