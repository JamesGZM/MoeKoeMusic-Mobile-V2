package cn.james.music.feature.localmusic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.designsystem.component.MoeSongRowStyle
import cn.james.music.core.designsystem.component.MoeHorizontalDivider
import cn.james.music.core.designsystem.component.action.MoeButton
import cn.james.music.core.designsystem.component.action.MoeTextButton
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar
import cn.james.music.core.model.local.DeviceAudioCandidate

@Composable
internal fun LocalMusicImportScreen(
    state: DeviceImportUiState,
    onBack: () -> Unit,
    onGrantPermission: () -> Unit,
    onRetry: () -> Unit,
    onToggleCandidate: (Long) -> Unit,
    onToggleAll: () -> Unit,
    onImport: (List<Long>) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        MoeStandardTopBar(
            title = stringResource(R.string.local_music_import_title),
            navigationContentDescription = stringResource(R.string.local_music_back),
            onNavigateBack = onBack,
        )
        when (state) {
            DeviceImportUiState.PermissionRequired -> PermissionRequiredContent(onGrantPermission)
            is DeviceImportUiState.Scanning -> ScanningContent(state.candidates)
            is DeviceImportUiState.Selection ->
                SelectionContent(
                    state = state,
                    onToggleCandidate = onToggleCandidate,
                    onToggleAll = onToggleAll,
                    onImport = onImport,
                )
            DeviceImportUiState.Failed -> ScanFailedContent(onRetry)
        }
    }
}

@Composable
private fun PermissionRequiredContent(onGrantPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().offset(y = (-39).dp).padding(horizontal = MoeKoeTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(112.dp).localMusicLayoutProbe(PROBE_PERMISSION_HERO),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = stringResource(R.string.local_music_permission_title),
            modifier = Modifier.padding(top = MoeKoeTheme.spacing.large),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.local_music_permission_message),
            modifier = Modifier.padding(top = MoeKoeTheme.spacing.compact),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        MoeButton(
            onClick = onGrantPermission,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = MoeKoeTheme.spacing.large)
                    .localMusicLayoutProbe(PROBE_PERMISSION_ACTION),
        ) {
            Text(stringResource(R.string.local_music_grant_permission))
        }
        Text(
            text = stringResource(R.string.local_music_permission_privacy),
            modifier = Modifier.padding(top = MoeKoeTheme.spacing.compact),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScanningContent(candidates: List<DeviceAudioCandidate>) {
    Column(Modifier.fillMaxSize().padding(top = 14.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MoeKoeTheme.spacing.large, vertical = MoeKoeTheme.spacing.medium)
                    .localMusicLayoutProbe(PROBE_SCAN_HEADER),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            Column(modifier = Modifier.padding(start = MoeKoeTheme.spacing.medium)) {
                Text(
                    text = stringResource(R.string.local_music_scanning_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.local_music_discovered_count, candidates.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = MoeKoeTheme.spacing.large))
        ScanningCandidateList(
            candidates = candidates,
            probeName = PROBE_SCAN_LIST,
            contentTopPadding = 21.dp,
        )
    }
}

@Composable
private fun SelectionContent(
    state: DeviceImportUiState.Selection,
    onToggleCandidate: (Long) -> Unit,
    onToggleAll: () -> Unit,
    onImport: (List<Long>) -> Unit,
) {
    val candidates = state.candidates
    val selectedIds = state.selectedIds
    Column(Modifier.fillMaxSize().padding(top = 14.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .localMusicLayoutProbe(PROBE_SELECTION_HEADER)
                    .padding(start = MoeKoeTheme.spacing.large, end = MoeKoeTheme.spacing.compact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.local_music_scan_complete),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.local_music_scan_result_count, candidates.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (candidates.isNotEmpty()) {
                MoeTextButton(
                    onClick = onToggleAll,
                ) {
                    Text(
                        stringResource(
                            if (selectedIds.size == candidates.size) R.string.local_music_clear_selection else R.string.local_music_select_all,
                        ),
                    )
                }
            }
        }
        if (candidates.isEmpty()) {
            LocalMusicMessage(
                title = stringResource(R.string.local_music_scan_empty_title),
                message = stringResource(R.string.local_music_scan_empty_message),
                modifier = Modifier.weight(1f),
            )
        } else {
            SelectionCandidateList(
                state = state,
                onToggle = onToggleCandidate,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(R.string.local_music_import_copy_note),
            modifier = Modifier.fillMaxWidth().padding(horizontal = MoeKoeTheme.spacing.medium),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        MoeButton(
            onClick = { onImport(selectedIds.toList()) },
            enabled = selectedIds.isNotEmpty(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MoeKoeTheme.spacing.medium)
                    .localMusicLayoutProbe(PROBE_SELECTION_ACTION),
        ) {
            Text(stringResource(R.string.local_music_import_selected, selectedIds.size))
        }
    }
}

@Composable
private fun ScanningCandidateList(
    candidates: List<DeviceAudioCandidate>,
    modifier: Modifier = Modifier,
    probeName: String? = null,
    contentTopPadding: Dp = MoeKoeTheme.spacing.small,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = MoeKoeTheme.spacing.medium,
                top = contentTopPadding,
                end = MoeKoeTheme.spacing.medium,
                bottom = MoeKoeTheme.spacing.small,
            ),
    ) {
        itemsIndexed(candidates, key = { _, candidate -> candidate.mediaStoreId }) { index, candidate ->
            MoeSongRow(
                title = candidate.displayName.substringBeforeLast('.'),
                subtitle = candidate.artist ?: stringResource(R.string.local_music_unknown_artist),
                metadata = formatDuration(candidate.durationMs),
                onClick = null,
                style = MoeSongRowStyle.Comfortable,
                modifier = if (index == 0 && probeName != null) Modifier.localMusicLayoutProbe(probeName) else Modifier,
                artwork = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
            MoeHorizontalDivider(startIndent = 74.dp)
        }
    }
}

@Composable
private fun SelectionCandidateList(
    state: DeviceImportUiState.Selection,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = MoeKoeTheme.spacing.medium,
                top = MoeKoeTheme.spacing.small,
                end = MoeKoeTheme.spacing.medium,
                bottom = MoeKoeTheme.spacing.small,
            ),
    ) {
        itemsIndexed(state.candidates, key = { _, candidate -> candidate.mediaStoreId }) { _, candidate ->
            MoeSongRow(
                title = candidate.displayName.substringBeforeLast('.'),
                subtitle = candidate.artist ?: stringResource(R.string.local_music_unknown_artist),
                metadata = formatDuration(candidate.durationMs),
                onClick = { onToggle(candidate.mediaStoreId) },
                style = MoeSongRowStyle.Comfortable,
                artwork = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailing = {
                    Checkbox(
                        checked = candidate.mediaStoreId in state.selectedIds,
                        onCheckedChange = { onToggle(candidate.mediaStoreId) },
                    )
                },
            )
            MoeHorizontalDivider(startIndent = 74.dp)
        }
    }
}

@Composable
private fun ScanFailedContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(MoeKoeTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LocalMusicMessage(
            title = stringResource(R.string.local_music_scan_failed_title),
            message = stringResource(R.string.local_music_scan_failed_message),
            modifier = Modifier.fillMaxWidth(),
        )
        MoeButton(onClick = onRetry, modifier = Modifier.padding(top = MoeKoeTheme.spacing.medium)) {
            Text(stringResource(R.string.local_music_retry))
        }
    }
}
