@file:Suppress("ktlint:standard:function-naming")

package cn.james.music.feature.localmusic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.action.MoeTextButton
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBarAction
import cn.james.music.core.designsystem.component.navigation.MoeTopBarTitleEmphasis
import cn.james.music.core.designsystem.component.overlay.MoeAlertDialog
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicSort

@Composable
internal fun LocalMusicScreen(
    state: LocalMusicUiState,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onPlay: (LocalMusic, List<LocalMusic>) -> Unit,
    onDelete: (String) -> Unit,
    onCancelImport: (String) -> Unit,
) {
    var deleting by remember { mutableStateOf<LocalMusic?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(LocalMusicSort.Newest) }
    val visible = remember(state.music, query, sort) { filterAndSortMusic(state.music, query, sort) }

    Column(Modifier.fillMaxSize()) {
        MoeStandardTopBar(
            title = stringResource(R.string.local_music_title),
            navigationContentDescription = stringResource(R.string.local_music_back),
            onNavigateBack = onBack,
            titleEmphasis = MoeTopBarTitleEmphasis.Strong,
            actions = {
                MoeStandardTopBarAction(
                    imageVector = Icons.Default.SystemUpdateAlt,
                    contentDescription = stringResource(R.string.local_music_open_import),
                    onClick = onImport,
                )
            },
        )
        state.imports
            .firstOrNull { progress ->
                progress.state == LocalImportBatchState.Running ||
                    progress.state == LocalImportBatchState.Queued
            }?.let { progress ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MoeKoeTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.local_music_import_progress,
                                progress.completedCount,
                                progress.totalCount,
                            ),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    MoeTextButton(onClick = { onCancelImport(progress.batchId) }) {
                        Text(stringResource(R.string.local_music_cancel))
                    }
                }
            }
        if (state.music.isNotEmpty()) {
            LocalMusicSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.local_music_search_label),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MoeKoeTheme.spacing.medium,
                            vertical = 6.dp,
                        ).offset(y = 3.dp)
                        .localMusicLayoutProbe(PROBE_LIBRARY_SEARCH),
            )
            LazyRow(
                modifier = Modifier.padding(top = 4.dp),
                contentPadding = PaddingValues(horizontal = MoeKoeTheme.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small),
            ) {
                items(LocalMusicSort.entries, key = LocalMusicSort::name) { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { sort = option },
                        label = { Text(stringResource(option.labelRes), style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector =
                                    when (option) {
                                        LocalMusicSort.Newest -> Icons.Default.History
                                        LocalMusicSort.Title -> Icons.Default.SortByAlpha
                                        LocalMusicSort.Artist -> Icons.Default.PersonOutline
                                        LocalMusicSort.Duration -> Icons.Default.Schedule
                                    },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                labelColor = MaterialTheme.colorScheme.primary,
                                iconColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                            ),
                    )
                }
            }
        }
        when {
            state.music.isEmpty() -> {
                LocalMusicMessage(
                    title = stringResource(R.string.local_music_empty_title),
                    message = stringResource(R.string.local_music_empty_message),
                )
            }

            visible.isEmpty() -> {
                LocalMusicMessage(
                    title = stringResource(R.string.local_music_no_results_title),
                    message = stringResource(R.string.local_music_no_results_message),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 19.dp,
                            top = 9.dp,
                            end = 8.dp,
                            bottom = MoeKoeTheme.spacing.medium,
                        ),
                ) {
                    itemsIndexed(visible, key = { _, music -> music.id }) { index, music ->
                        LocalMusicRow(
                            music = music,
                            isPlaying = music.id == state.playingSongId,
                            onClick = { onPlay(music, visible) },
                            onDelete = { deleting = music },
                            modifier = if (index == 0) Modifier.localMusicLayoutProbe(PROBE_LIBRARY_LIST) else Modifier,
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    deleting?.let { music ->
        MoeAlertDialog(
            title = stringResource(R.string.local_music_delete_title),
            message = stringResource(R.string.local_music_delete_message),
            confirmLabel = stringResource(R.string.local_music_delete),
            dismissLabel = stringResource(R.string.local_music_cancel),
            onDismissRequest = { deleting = null },
            onConfirm = {
                onDelete(music.id)
                deleting = null
            },
            destructive = true,
        )
    }
}

@Composable
private fun LocalMusicSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.height(44.dp),
        decorationBox = { innerTextField ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                }
            }
        },
    )
}
