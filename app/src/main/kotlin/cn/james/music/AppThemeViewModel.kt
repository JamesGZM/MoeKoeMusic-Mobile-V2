package cn.james.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.designsystem.BrandThemeColor
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.settings.AppSettingsRepository
import cn.james.music.core.model.settings.AppThemePreference
import cn.james.music.core.model.settings.BrandThemeColorPreference
import cn.james.music.core.model.settings.LyricsTextSizePreference
import cn.james.music.feature.player.PlayerLyricsTextSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AppThemeViewModel
    @Inject
    constructor(
        private val repository: AppSettingsRepository,
    ) : ViewModel() {
        val themeMode: StateFlow<ThemeMode> =
            repository.settings
                .map { snapshot -> snapshot.settings.theme.toThemeMode() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = ThemeMode.System,
                )

        val brandThemeColor: StateFlow<BrandThemeColor> =
            repository.settings
                .map { snapshot -> snapshot.settings.brandThemeColor.toDesignSystemBrandThemeColor() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = BrandThemeColor.SkyBlue,
                )

        val dynamicCoverColors: StateFlow<Boolean> =
            repository.settings
                .map { snapshot -> snapshot.settings.dynamicCoverColors }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = true,
                )

        val showLyricsSupplementalText: StateFlow<Boolean> =
            repository.settings
                .map { snapshot -> snapshot.settings.showLyricsSupplementalText }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = true,
                )

        val lyricsTextSize: StateFlow<PlayerLyricsTextSize> =
            repository.settings
                .map { snapshot -> snapshot.settings.lyricsTextSize.toPlayerLyricsTextSize() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = PlayerLyricsTextSize.Standard,
                )

        private var updateJob: Job? = null

        fun updateTheme(themeMode: ThemeMode) {
            updateJob?.cancel()
            updateJob = viewModelScope.launch { repository.setTheme(themeMode.toPreference()) }
        }

        private fun AppThemePreference.toThemeMode(): ThemeMode =
            when (this) {
                AppThemePreference.System -> ThemeMode.System
                AppThemePreference.Light -> ThemeMode.Light
                AppThemePreference.Dark -> ThemeMode.Dark
                AppThemePreference.Amoled -> ThemeMode.Amoled
            }

        private fun ThemeMode.toPreference(): AppThemePreference =
            when (this) {
                ThemeMode.System -> AppThemePreference.System
                ThemeMode.Light -> AppThemePreference.Light
                ThemeMode.Dark -> AppThemePreference.Dark
                ThemeMode.Amoled -> AppThemePreference.Amoled
            }

        private fun BrandThemeColorPreference.toDesignSystemBrandThemeColor(): BrandThemeColor =
            when (this) {
                BrandThemeColorPreference.SkyBlue -> BrandThemeColor.SkyBlue
                BrandThemeColorPreference.SakuraPink -> BrandThemeColor.SakuraPink
                BrandThemeColorPreference.StarPurple -> BrandThemeColor.StarPurple
                BrandThemeColorPreference.MintGreen -> BrandThemeColor.MintGreen
                BrandThemeColorPreference.LakeCyan -> BrandThemeColor.LakeCyan
                BrandThemeColorPreference.SunsetOrange -> BrandThemeColor.SunsetOrange
            }

        private fun LyricsTextSizePreference.toPlayerLyricsTextSize(): PlayerLyricsTextSize =
            when (this) {
                LyricsTextSizePreference.Standard -> PlayerLyricsTextSize.Standard
                LyricsTextSizePreference.Large -> PlayerLyricsTextSize.Large
                LyricsTextSizePreference.Largest -> PlayerLyricsTextSize.Largest
            }
    }
