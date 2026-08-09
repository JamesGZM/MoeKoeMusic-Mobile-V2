@file:Suppress("ktlint:standard:function-naming")

package cn.james.music.feature.localmusic

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeHorizontalDivider
import cn.james.music.core.designsystem.component.MoePassiveOutline
import cn.james.music.core.designsystem.component.MoePassiveOutlineEmphasis
import cn.james.music.core.designsystem.component.action.MoeTextButton
import cn.james.music.core.designsystem.component.input.MoeSearchField
import cn.james.music.core.designsystem.component.input.MoeSearchFieldStyle
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBarAction
import cn.james.music.core.designsystem.component.navigation.MoeTopBarTitleEmphasis
import cn.james.music.core.designsystem.component.overlay.MoeAlertDialog

@Composable
internal fun LocalMusicScreen(
    state: LocalMusicUiState,
    onAction: (LocalMusicAction) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        MoeStandardTopBar(
            title = stringResource(R.string.local_music_title),
            navigationContentDescription = stringResource(R.string.local_music_back),
            onNavigateBack = { onAction(LocalMusicAction.Back) },
            titleEmphasis = MoeTopBarTitleEmphasis.Strong,
            actions = {
                MoeStandardTopBarAction(
                    imageVector = Icons.Default.SystemUpdateAlt,
                    contentDescription = stringResource(R.string.local_music_open_import),
                    onClick = { onAction(LocalMusicAction.OpenImporter) },
                )
            },
        )
        state.activeImport?.let { progress ->
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
                MoeTextButton(onClick = { onAction(LocalMusicAction.CancelImport(progress.id)) }) {
                    Text(stringResource(R.string.local_music_cancel))
                }
            }
        }
        if (state.content != LocalMusicContentUi.EmptyLibrary) {
            LocalMusicSearchField(
                value = state.query,
                onValueChange = { value -> onAction(LocalMusicAction.QueryChanged(value)) },
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
                items(LocalMusicSortUi.entries, key = LocalMusicSortUi::name) { option ->
                    FilterChip(
                        selected = state.sort == option,
                        onClick = { onAction(LocalMusicAction.SelectSort(option)) },
                        label = {
                            Text(
                                stringResource(option.labelRes),
                                style =
                                    MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    ),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector =
                                    when (option) {
                                        LocalMusicSortUi.Newest -> Icons.Default.History
                                        LocalMusicSortUi.Title -> Icons.Default.SortByAlpha
                                        LocalMusicSortUi.Artist -> Icons.Default.PersonOutline
                                        LocalMusicSortUi.Duration -> Icons.Default.Schedule
                                    },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                labelColor = MaterialTheme.colorScheme.primary,
                                iconColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                            ),
                        border = MoePassiveOutline(MoePassiveOutlineEmphasis.Standard),
                    )
                }
            }
        }
        when (state.content) {
            LocalMusicContentUi.EmptyLibrary -> {
                LocalMusicMessage(
                    title = stringResource(R.string.local_music_empty_title),
                    message = stringResource(R.string.local_music_empty_message),
                )
            }

            LocalMusicContentUi.NoResults -> {
                LocalMusicMessage(
                    title = stringResource(R.string.local_music_no_results_title),
                    message = stringResource(R.string.local_music_no_results_message),
                )
            }

            LocalMusicContentUi.Songs -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 19.dp,
                            top = 11.dp,
                            end = 8.dp,
                            bottom = MoeKoeTheme.spacing.medium,
                        ),
                ) {
                    itemsIndexed(state.songs, key = { _, song -> song.id }) { index, song ->
                        LocalMusicRow(
                            song = song,
                            onClick = { onAction(LocalMusicAction.PlaySong(song.id)) },
                            onDelete = { onAction(LocalMusicAction.RequestDeleteSong(song.id)) },
                            modifier = if (index == 0) Modifier.localMusicLayoutProbe(PROBE_LIBRARY_LIST) else Modifier,
                        )
                        MoeHorizontalDivider(startIndent = 74.dp)
                    }
                }
            }
        }
    }

    state.pendingDeleteSongId?.let { id ->
        MoeAlertDialog(
            title = stringResource(R.string.local_music_delete_title),
            message = stringResource(R.string.local_music_delete_message),
            confirmLabel = stringResource(R.string.local_music_delete),
            dismissLabel = stringResource(R.string.local_music_cancel),
            onDismissRequest = { onAction(LocalMusicAction.DismissDeleteSong) },
            onConfirm = { onAction(LocalMusicAction.ConfirmDeleteSong(id)) },
            destructive = true,
        )
    }
}

private val LocalMusicSortUi.labelRes: Int
    @StringRes
    get() =
        when (this) {
            LocalMusicSortUi.Newest -> R.string.local_music_sort_newest
            LocalMusicSortUi.Title -> R.string.local_music_sort_title
            LocalMusicSortUi.Artist -> R.string.local_music_sort_artist
            LocalMusicSortUi.Duration -> R.string.local_music_sort_duration
        }

@Composable
private fun LocalMusicSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    MoeSearchField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        style = MoeSearchFieldStyle.Standalone,
    )
}
