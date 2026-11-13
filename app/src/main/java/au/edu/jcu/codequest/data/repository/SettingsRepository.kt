package au.edu.jcu.codequest.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import au.edu.jcu.codequest.domain.model.Difficulty
import au.edu.jcu.codequest.domain.model.ThemeMode
import au.edu.jcu.codequest.domain.model.UserSettings
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** The learner's preferences, including the consent choice made on first launch. */
interface SettingsRepository {
    val settings: Flow<UserSettings>

    /** Stores the first-run choice and marks onboarding as finished. */
    suspend fun completeOnboarding(detailedHistory: Boolean)
    suspend fun setDetailedHistory(enabled: Boolean)
    suspend fun setSoundOn(enabled: Boolean)
    suspend fun setDifficulty(difficulty: Difficulty)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setUnlockAllLessons(enabled: Boolean)
}

class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private val defaults = UserSettings()

    override val settings: Flow<UserSettings> = dataStore.data
        // A corrupted or unreadable file falls back to the defaults instead of crashing.
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            UserSettings(
                onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: defaults.onboardingDone,
                detailedHistory = prefs[Keys.DETAILED_HISTORY] ?: defaults.detailedHistory,
                soundOn = prefs[Keys.SOUND_ON] ?: defaults.soundOn,
                difficulty = Difficulty.entries.firstOrNull { it.name == prefs[Keys.DIFFICULTY] }
                    ?: defaults.difficulty,
                themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME_MODE] }
                    ?: defaults.themeMode,
                unlockAllLessons = prefs[Keys.UNLOCK_ALL] ?: defaults.unlockAllLessons,
            )
        }

    override suspend fun completeOnboarding(detailedHistory: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DETAILED_HISTORY] = detailedHistory
            prefs[Keys.ONBOARDING_DONE] = true
        }
    }

    override suspend fun setDetailedHistory(enabled: Boolean) {
        dataStore.edit { it[Keys.DETAILED_HISTORY] = enabled }
    }

    override suspend fun setSoundOn(enabled: Boolean) {
        dataStore.edit { it[Keys.SOUND_ON] = enabled }
    }

    override suspend fun setDifficulty(difficulty: Difficulty) {
        dataStore.edit { it[Keys.DIFFICULTY] = difficulty.name }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    override suspend fun setUnlockAllLessons(enabled: Boolean) {
        dataStore.edit { it[Keys.UNLOCK_ALL] = enabled }
    }

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DETAILED_HISTORY = booleanPreferencesKey("detailed_history")
        val SOUND_ON = booleanPreferencesKey("sound_on")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val UNLOCK_ALL = booleanPreferencesKey("unlock_all_lessons")
    }
}
