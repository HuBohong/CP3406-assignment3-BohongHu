package au.edu.jcu.codequest.di

import au.edu.jcu.codequest.data.repository.CurriculumRepository
import au.edu.jcu.codequest.data.repository.DataStoreSettingsRepository
import au.edu.jcu.codequest.data.repository.NetworkTriviaRepository
import au.edu.jcu.codequest.data.repository.OfflineCurriculumRepository
import au.edu.jcu.codequest.data.repository.OfflineProgressRepository
import au.edu.jcu.codequest.data.repository.ProgressRepository
import au.edu.jcu.codequest.data.repository.SettingsRepository
import au.edu.jcu.codequest.data.repository.TriviaRepository
import au.edu.jcu.codequest.util.SoundPlayer
import au.edu.jcu.codequest.util.ToneSoundPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Tells Hilt which implementation to use for each interface the ViewModels depend on. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCurriculumRepository(impl: OfflineCurriculumRepository): CurriculumRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: OfflineProgressRepository): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindTriviaRepository(impl: NetworkTriviaRepository): TriviaRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindSoundPlayer(impl: ToneSoundPlayer): SoundPlayer
}
