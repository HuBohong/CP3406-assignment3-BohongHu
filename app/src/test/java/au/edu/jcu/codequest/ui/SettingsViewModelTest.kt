package au.edu.jcu.codequest.ui

import au.edu.jcu.codequest.domain.model.Difficulty
import au.edu.jcu.codequest.domain.model.ThemeMode
import au.edu.jcu.codequest.domain.model.UserSettings
import au.edu.jcu.codequest.testutil.FakeProgressRepository
import au.edu.jcu.codequest.testutil.FakeSettingsRepository
import au.edu.jcu.codequest.testutil.MainDispatcherRule
import au.edu.jcu.codequest.ui.settings.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository(UserSettings(onboardingDone = true, detailedHistory = true))
    private val progress = FakeProgressRepository()
    // Created on first use, after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy { SettingsViewModel(settings, progress) }

    @Test
    fun turningHistoryOff_alsoDeletesTheStoredAnswers() {
        viewModel.setDetailedHistory(false)

        assertFalse(settings.state.value.detailedHistory)
        assertEquals(1, progress.clearAttemptsCalls)
    }

    @Test
    fun turningHistoryOn_deletesNothing() {
        viewModel.setDetailedHistory(true)

        assertTrue(settings.state.value.detailedHistory)
        assertEquals(0, progress.clearAttemptsCalls)
    }

    @Test
    fun preferences_areWrittenThrough() {
        viewModel.setSoundOn(false)
        viewModel.setDifficulty(Difficulty.HARD)
        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setUnlockAllLessons(true)

        val saved = settings.state.value
        assertFalse(saved.soundOn)
        assertEquals(Difficulty.HARD, saved.difficulty)
        assertEquals(ThemeMode.DARK, saved.themeMode)
        assertTrue(saved.unlockAllLessons)
    }

    @Test
    fun deleteProgress_clearsLearningDataButKeepsPreferences() {
        viewModel.setThemeMode(ThemeMode.DARK)

        viewModel.deleteProgress()

        assertEquals(1, progress.clearAllCalls)
        assertEquals(ThemeMode.DARK, settings.state.value.themeMode)
    }
}
