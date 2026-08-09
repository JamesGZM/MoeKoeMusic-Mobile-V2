package cn.james.music.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.settings.AppSettingsProblem
import cn.james.music.core.model.settings.AppSettingsRepository
import cn.james.music.core.model.settings.AppSettingsUpdateResult
import cn.james.music.core.model.settings.AppThemePreference
import cn.james.music.core.model.settings.LyricsTextSizePreference
import cn.james.music.core.model.settings.PlaybackQualityPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel
    @Inject
    constructor(
        private val repository: AppSettingsRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(SettingsUiState())
        val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

        private var themeUpdateJob: Job? = null
        private var autoSkipUpdateJob: Job? = null
        private var dynamicCoverColorsUpdateJob: Job? = null
        private var lyricsSupplementalTextUpdateJob: Job? = null
        private var lyricsTextSizeUpdateJob: Job? = null
        private var playbackQualityUpdateJob: Job? = null
        private var retryRequest: RetryRequest? = null
        private var themeUpdateGeneration = 0L
        private var autoSkipUpdateGeneration = 0L
        private var dynamicCoverColorsUpdateGeneration = 0L
        private var lyricsSupplementalTextUpdateGeneration = 0L
        private var lyricsTextSizeUpdateGeneration = 0L
        private var playbackQualityUpdateGeneration = 0L
        private var persistedTheme = AppThemePreference.System
        private var persistedAutoSkipFailedPlayback = true
        private var persistedDynamicCoverColors = true
        private var persistedLyricsSupplementalText = true
        private var persistedLyricsTextSize = LyricsTextSizePreference.Standard
        private var persistedPlaybackQuality = PlaybackQualityPreference.Standard

        init {
            viewModelScope.launch {
                repository.settings.collect { snapshot ->
                    persistedTheme = snapshot.settings.theme
                    persistedAutoSkipFailedPlayback = snapshot.settings.autoSkipFailedPlayback
                    persistedDynamicCoverColors = snapshot.settings.dynamicCoverColors
                    persistedLyricsSupplementalText = snapshot.settings.showLyricsSupplementalText
                    persistedLyricsTextSize = snapshot.settings.lyricsTextSize
                    persistedPlaybackQuality = snapshot.settings.playbackQuality
                    mutableState.update { current ->
                        val theme =
                            if (current.savingTheme == null || current.savingTheme.toDomain() == snapshot.settings.theme) {
                                snapshot.settings.theme.toUi()
                            } else {
                                current.theme
                            }
                        val autoSkip =
                            if (
                                current.savingAutoSkipFailedPlayback == null ||
                                current.savingAutoSkipFailedPlayback == snapshot.settings.autoSkipFailedPlayback
                            ) {
                                snapshot.settings.autoSkipFailedPlayback
                            } else {
                                current.autoSkipFailedPlayback
                            }
                        val dynamicColors =
                            if (
                                current.savingDynamicCoverColors == null ||
                                current.savingDynamicCoverColors == snapshot.settings.dynamicCoverColors
                            ) {
                                snapshot.settings.dynamicCoverColors
                            } else {
                                current.dynamicCoverColors
                            }
                        val supplementalText =
                            if (current.savingLyricsSupplementalText == null ||
                                current.savingLyricsSupplementalText == snapshot.settings.showLyricsSupplementalText
                            ) {
                                snapshot.settings.showLyricsSupplementalText
                            } else {
                                current.showLyricsSupplementalText
                            }
                        val lyricsTextSize =
                            if (
                                current.savingLyricsTextSize == null ||
                                current.savingLyricsTextSize.toDomain() == snapshot.settings.lyricsTextSize
                            ) {
                                snapshot.settings.lyricsTextSize.toUi()
                            } else {
                                current.lyricsTextSize
                            }
                        val playbackQuality =
                            if (
                                current.savingPlaybackQuality == null ||
                                current.savingPlaybackQuality.toDomain() == snapshot.settings.playbackQuality
                            ) {
                                snapshot.settings.playbackQuality.toUi()
                            } else {
                                current.playbackQuality
                            }
                        current.withGroups(
                            theme = theme,
                            autoSkipFailedPlayback = autoSkip,
                            dynamicCoverColors = dynamicColors,
                            showLyricsSupplementalText = supplementalText,
                            lyricsTextSize = lyricsTextSize,
                            playbackQuality = playbackQuality,
                            problem = if (current.problem == SettingsProblemUi.Write) current.problem else snapshot.problem?.toUi(),
                        )
                    }
                }
            }
        }

        fun onAction(action: SettingsAction) {
            when (action) {
                SettingsAction.Back -> Unit
                is SettingsAction.SelectTheme -> selectTheme(action.theme)
                SettingsAction.OpenTheme -> mutableState.update { it.copy(overlay = SettingsOverlay.ThemeSelection) }
                SettingsAction.OpenAbout -> mutableState.update { it.copy(overlay = SettingsOverlay.About) }
                is SettingsAction.SetAutoSkipFailedPlayback -> setAutoSkipFailedPlayback(action.enabled)
                is SettingsAction.SetDynamicCoverColors -> setDynamicCoverColors(action.enabled)
                is SettingsAction.SetShowLyricsSupplementalText -> setShowLyricsSupplementalText(action.enabled)
                is SettingsAction.SelectLyricsTextSize -> selectLyricsTextSize(action.size)
                SettingsAction.OpenLyricsTextSize -> mutableState.update { it.copy(overlay = SettingsOverlay.LyricsTextSizeSelection) }
                is SettingsAction.SelectPlaybackQuality -> selectPlaybackQuality(action.quality)
                SettingsAction.OpenPlaybackQuality -> mutableState.update { it.copy(overlay = SettingsOverlay.PlaybackQualitySelection) }
                SettingsAction.DismissOverlay -> mutableState.update { it.copy(overlay = null) }
                SettingsAction.Retry -> retry()
                SettingsAction.DismissProblem -> dismissProblem()
            }
        }

        private fun selectTheme(theme: SettingsThemeUi) {
            mutableState.update { it.copy(overlay = null) }
            if (theme == mutableState.value.theme && mutableState.value.savingTheme == null) return
            val generation = ++themeUpdateGeneration
            themeUpdateJob?.cancel()
            themeUpdateJob =
                viewModelScope.launch {
                    retryRequest = null
                    mutableState.update { current ->
                        current.withGroups(savingTheme = theme, problem = null, canRetry = false)
                    }
                    when (repository.setTheme(theme.toDomain())) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation == themeUpdateGeneration) {
                                mutableState.update { current -> current.withGroups(theme = theme, savingTheme = null) }
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation == themeUpdateGeneration) {
                                retryRequest = RetryRequest.Theme(theme)
                                mutableState.update { current ->
                                    current.withGroups(
                                        theme = persistedTheme.toUi(),
                                        savingTheme = null,
                                        problem = SettingsProblemUi.Write,
                                        canRetry = true,
                                    )
                                }
                            }
                        }
                    }
                }
        }

        private fun setAutoSkipFailedPlayback(enabled: Boolean) {
            if (enabled == mutableState.value.autoSkipFailedPlayback && mutableState.value.savingAutoSkipFailedPlayback == null) return
            val generation = ++autoSkipUpdateGeneration
            autoSkipUpdateJob?.cancel()
            autoSkipUpdateJob =
                viewModelScope.launch {
                    retryRequest = null
                    mutableState.update { current ->
                        current.withGroups(savingAutoSkipFailedPlayback = enabled, problem = null, canRetry = false)
                    }
                    when (repository.setAutoSkipFailedPlayback(enabled)) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation == autoSkipUpdateGeneration) {
                                mutableState.update { current ->
                                    current.withGroups(autoSkipFailedPlayback = enabled, savingAutoSkipFailedPlayback = null)
                                }
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation == autoSkipUpdateGeneration) {
                                retryRequest = RetryRequest.AutoSkipFailedPlayback(enabled)
                                mutableState.update { current ->
                                    current.withGroups(
                                        autoSkipFailedPlayback = persistedAutoSkipFailedPlayback,
                                        savingAutoSkipFailedPlayback = null,
                                        problem = SettingsProblemUi.Write,
                                        canRetry = true,
                                    )
                                }
                            }
                        }
                    }
                }
        }

        private fun setDynamicCoverColors(enabled: Boolean) {
            if (enabled == mutableState.value.dynamicCoverColors && mutableState.value.savingDynamicCoverColors == null) return
            val generation = ++dynamicCoverColorsUpdateGeneration
            dynamicCoverColorsUpdateJob?.cancel()
            dynamicCoverColorsUpdateJob =
                viewModelScope.launch {
                    retryRequest = null
                    mutableState.update { current ->
                        current.withGroups(savingDynamicCoverColors = enabled, problem = null, canRetry = false)
                    }
                    when (repository.setDynamicCoverColors(enabled)) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation == dynamicCoverColorsUpdateGeneration) {
                                mutableState.update { current ->
                                    current.withGroups(dynamicCoverColors = enabled, savingDynamicCoverColors = null)
                                }
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation == dynamicCoverColorsUpdateGeneration) {
                                retryRequest = RetryRequest.DynamicCoverColors(enabled)
                                mutableState.update { current ->
                                    current.withGroups(
                                        dynamicCoverColors = persistedDynamicCoverColors,
                                        savingDynamicCoverColors = null,
                                        problem = SettingsProblemUi.Write,
                                        canRetry = true,
                                    )
                                }
                            }
                        }
                    }
                }
        }

        private fun setShowLyricsSupplementalText(enabled: Boolean) {
            if (enabled == mutableState.value.showLyricsSupplementalText && mutableState.value.savingLyricsSupplementalText == null) return
            val generation = ++lyricsSupplementalTextUpdateGeneration
            lyricsSupplementalTextUpdateJob?.cancel()
            lyricsSupplementalTextUpdateJob =
                viewModelScope.launch {
                    retryRequest = null
                    mutableState.update { current ->
                        current.withGroups(savingLyricsSupplementalText = enabled, problem = null, canRetry = false)
                    }
                    when (repository.setShowLyricsSupplementalText(enabled)) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation == lyricsSupplementalTextUpdateGeneration) {
                                mutableState.update { current ->
                                    current.withGroups(showLyricsSupplementalText = enabled, savingLyricsSupplementalText = null)
                                }
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation == lyricsSupplementalTextUpdateGeneration) {
                                retryRequest = RetryRequest.LyricsSupplementalText(enabled)
                                mutableState.update { current ->
                                    current.withGroups(
                                        showLyricsSupplementalText = persistedLyricsSupplementalText,
                                        savingLyricsSupplementalText = null,
                                        problem = SettingsProblemUi.Write,
                                        canRetry = true,
                                    )
                                }
                            }
                        }
                    }
                }
        }

        private fun selectLyricsTextSize(size: SettingsLyricsTextSizeUi) {
            mutableState.update { it.copy(overlay = null) }
            if (size == mutableState.value.lyricsTextSize && mutableState.value.savingLyricsTextSize == null) return
            val generation = ++lyricsTextSizeUpdateGeneration
            lyricsTextSizeUpdateJob?.cancel()
            lyricsTextSizeUpdateJob =
                viewModelScope.launch {
                    retryRequest = null
                    mutableState.update { current ->
                        current.withGroups(savingLyricsTextSize = size, problem = null, canRetry = false)
                    }
                    when (repository.setLyricsTextSize(size.toDomain())) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation == lyricsTextSizeUpdateGeneration) {
                                mutableState.update { current ->
                                    current.withGroups(lyricsTextSize = size, savingLyricsTextSize = null)
                                }
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation == lyricsTextSizeUpdateGeneration) {
                                retryRequest = RetryRequest.LyricsTextSize(size)
                                mutableState.update { current ->
                                    current.withGroups(
                                        lyricsTextSize = persistedLyricsTextSize.toUi(),
                                        savingLyricsTextSize = null,
                                        problem = SettingsProblemUi.Write,
                                        canRetry = true,
                                    )
                                }
                            }
                        }
                    }
                }
        }

        private fun selectPlaybackQuality(quality: SettingsPlaybackQualityUi) {
            mutableState.update { it.copy(overlay = null) }
            if (quality == mutableState.value.playbackQuality && mutableState.value.savingPlaybackQuality == null) return
            val generation = ++playbackQualityUpdateGeneration
            playbackQualityUpdateJob?.cancel()
            playbackQualityUpdateJob =
                viewModelScope.launch {
                    retryRequest = null
                    mutableState.update { current ->
                        current.withGroups(savingPlaybackQuality = quality, problem = null, canRetry = false)
                    }
                    when (repository.setPlaybackQuality(quality.toDomain())) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation == playbackQualityUpdateGeneration) {
                                mutableState.update { current ->
                                    current.withGroups(playbackQuality = quality, savingPlaybackQuality = null)
                                }
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation == playbackQualityUpdateGeneration) {
                                retryRequest = RetryRequest.PlaybackQuality(quality)
                                mutableState.update { current ->
                                    current.withGroups(
                                        playbackQuality = persistedPlaybackQuality.toUi(),
                                        savingPlaybackQuality = null,
                                        problem = SettingsProblemUi.Write,
                                        canRetry = true,
                                    )
                                }
                            }
                        }
                    }
                }
        }

        private fun retry() {
            when (val request = retryRequest) {
                null -> Unit
                is RetryRequest.Theme -> selectTheme(request.theme)
                is RetryRequest.AutoSkipFailedPlayback -> setAutoSkipFailedPlayback(request.enabled)
                is RetryRequest.DynamicCoverColors -> setDynamicCoverColors(request.enabled)
                is RetryRequest.LyricsSupplementalText -> setShowLyricsSupplementalText(request.enabled)
                is RetryRequest.LyricsTextSize -> selectLyricsTextSize(request.size)
                is RetryRequest.PlaybackQuality -> selectPlaybackQuality(request.quality)
            }
        }

        private fun dismissProblem() {
            retryRequest = null
            mutableState.update { it.copy(problem = null, canRetry = false) }
        }

        private sealed interface RetryRequest {
            data class Theme(
                val theme: SettingsThemeUi,
            ) : RetryRequest

            data class AutoSkipFailedPlayback(
                val enabled: Boolean,
            ) : RetryRequest

            data class DynamicCoverColors(
                val enabled: Boolean,
            ) : RetryRequest

            data class LyricsSupplementalText(
                val enabled: Boolean,
            ) : RetryRequest

            data class LyricsTextSize(
                val size: SettingsLyricsTextSizeUi,
            ) : RetryRequest

            data class PlaybackQuality(
                val quality: SettingsPlaybackQualityUi,
            ) : RetryRequest
        }

        private fun SettingsUiState.withGroups(
            theme: SettingsThemeUi = this.theme,
            autoSkipFailedPlayback: Boolean = this.autoSkipFailedPlayback,
            dynamicCoverColors: Boolean = this.dynamicCoverColors,
            showLyricsSupplementalText: Boolean = this.showLyricsSupplementalText,
            lyricsTextSize: SettingsLyricsTextSizeUi = this.lyricsTextSize,
            playbackQuality: SettingsPlaybackQualityUi = this.playbackQuality,
            savingTheme: SettingsThemeUi? = this.savingTheme,
            savingAutoSkipFailedPlayback: Boolean? = this.savingAutoSkipFailedPlayback,
            savingDynamicCoverColors: Boolean? = this.savingDynamicCoverColors,
            savingLyricsSupplementalText: Boolean? = this.savingLyricsSupplementalText,
            savingLyricsTextSize: SettingsLyricsTextSizeUi? = this.savingLyricsTextSize,
            savingPlaybackQuality: SettingsPlaybackQualityUi? = this.savingPlaybackQuality,
            problem: SettingsProblemUi? = this.problem,
            canRetry: Boolean = this.canRetry,
            overlay: SettingsOverlay? = this.overlay,
        ): SettingsUiState =
            copy(
                theme = theme,
                autoSkipFailedPlayback = autoSkipFailedPlayback,
                dynamicCoverColors = dynamicCoverColors,
                showLyricsSupplementalText = showLyricsSupplementalText,
                lyricsTextSize = lyricsTextSize,
                playbackQuality = playbackQuality,
                savingTheme = savingTheme,
                savingAutoSkipFailedPlayback = savingAutoSkipFailedPlayback,
                savingDynamicCoverColors = savingDynamicCoverColors,
                savingLyricsSupplementalText = savingLyricsSupplementalText,
                savingLyricsTextSize = savingLyricsTextSize,
                savingPlaybackQuality = savingPlaybackQuality,
                problem = problem,
                canRetry = canRetry,
                overlay = overlay,
                groups =
                    settingsGroups(
                        theme = theme,
                        autoSkipFailedPlayback = autoSkipFailedPlayback,
                        dynamicCoverColors = dynamicCoverColors,
                        showLyricsSupplementalText = showLyricsSupplementalText,
                        lyricsTextSize = lyricsTextSize,
                        playbackQuality = playbackQuality,
                        savingTheme = savingTheme,
                        savingAutoSkipFailedPlayback = savingAutoSkipFailedPlayback,
                        savingDynamicCoverColors = savingDynamicCoverColors,
                        savingLyricsSupplementalText = savingLyricsSupplementalText,
                        savingLyricsTextSize = savingLyricsTextSize,
                        savingPlaybackQuality = savingPlaybackQuality,
                    ),
            )

        private fun AppThemePreference.toUi(): SettingsThemeUi =
            when (this) {
                AppThemePreference.System -> SettingsThemeUi.System
                AppThemePreference.Light -> SettingsThemeUi.Light
                AppThemePreference.Dark -> SettingsThemeUi.Dark
                AppThemePreference.Amoled -> SettingsThemeUi.Amoled
            }

        private fun SettingsThemeUi.toDomain(): AppThemePreference =
            when (this) {
                SettingsThemeUi.System -> AppThemePreference.System
                SettingsThemeUi.Light -> AppThemePreference.Light
                SettingsThemeUi.Dark -> AppThemePreference.Dark
                SettingsThemeUi.Amoled -> AppThemePreference.Amoled
            }

        private fun LyricsTextSizePreference.toUi(): SettingsLyricsTextSizeUi =
            when (this) {
                LyricsTextSizePreference.Standard -> SettingsLyricsTextSizeUi.Standard
                LyricsTextSizePreference.Large -> SettingsLyricsTextSizeUi.Large
                LyricsTextSizePreference.Largest -> SettingsLyricsTextSizeUi.Largest
            }

        private fun SettingsLyricsTextSizeUi.toDomain(): LyricsTextSizePreference =
            when (this) {
                SettingsLyricsTextSizeUi.Standard -> LyricsTextSizePreference.Standard
                SettingsLyricsTextSizeUi.Large -> LyricsTextSizePreference.Large
                SettingsLyricsTextSizeUi.Largest -> LyricsTextSizePreference.Largest
            }

        private fun PlaybackQualityPreference.toUi(): SettingsPlaybackQualityUi =
            when (this) {
                PlaybackQualityPreference.Standard -> SettingsPlaybackQualityUi.Standard
                PlaybackQualityPreference.High -> SettingsPlaybackQualityUi.High
                PlaybackQualityPreference.Lossless -> SettingsPlaybackQualityUi.Lossless
                PlaybackQualityPreference.HiRes -> SettingsPlaybackQualityUi.HiRes
                PlaybackQualityPreference.ViperAtmos -> SettingsPlaybackQualityUi.ViperAtmos
                PlaybackQualityPreference.ViperClear -> SettingsPlaybackQualityUi.ViperClear
                PlaybackQualityPreference.ViperTape -> SettingsPlaybackQualityUi.ViperTape
            }

        private fun SettingsPlaybackQualityUi.toDomain(): PlaybackQualityPreference =
            when (this) {
                SettingsPlaybackQualityUi.Standard -> PlaybackQualityPreference.Standard
                SettingsPlaybackQualityUi.High -> PlaybackQualityPreference.High
                SettingsPlaybackQualityUi.Lossless -> PlaybackQualityPreference.Lossless
                SettingsPlaybackQualityUi.HiRes -> PlaybackQualityPreference.HiRes
                SettingsPlaybackQualityUi.ViperAtmos -> PlaybackQualityPreference.ViperAtmos
                SettingsPlaybackQualityUi.ViperClear -> PlaybackQualityPreference.ViperClear
                SettingsPlaybackQualityUi.ViperTape -> PlaybackQualityPreference.ViperTape
            }

        private fun AppSettingsProblem.toUi(): SettingsProblemUi =
            when (this) {
                AppSettingsProblem.Read -> SettingsProblemUi.Read
                AppSettingsProblem.Write -> SettingsProblemUi.Write
            }
    }
