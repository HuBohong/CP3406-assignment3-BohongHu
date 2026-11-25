package au.edu.jcu.codequest.di

import au.edu.jcu.codequest.data.repository.BlockCollectionRepository
import au.edu.jcu.codequest.data.repository.BuiltInLearnCardRepository
import au.edu.jcu.codequest.data.repository.CurriculumRepository
import au.edu.jcu.codequest.data.repository.DataStoreSandboxRepository
import au.edu.jcu.codequest.data.repository.DataStoreSettingsRepository
import au.edu.jcu.codequest.data.repository.LearnCardRepository
import au.edu.jcu.codequest.data.repository.MissionRepository
import au.edu.jcu.codequest.data.repository.NetworkTriviaRepository
import au.edu.jcu.codequest.data.repository.OfflineCurriculumRepository
import au.edu.jcu.codequest.data.repository.OfflineMissionRepository
import au.edu.jcu.codequest.data.repository.OfflineProgressRepository
import au.edu.jcu.codequest.data.repository.ProgressBlockCollectionRepository
import au.edu.jcu.codequest.data.repository.ProgressRepository
import au.edu.jcu.codequest.data.repository.SandboxRepository
import au.edu.jcu.codequest.data.repository.SettingsRepository
import au.edu.jcu.codequest.data.repository.TriviaRepository
import au.edu.jcu.codequest.util.DelayFrameTimer
import au.edu.jcu.codequest.util.FrameTimer
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
    abstract fun bindLearnCardRepository(impl: BuiltInLearnCardRepository): LearnCardRepository

    @Binds
    @Singleton
    abstract fun bindMissionRepository(impl: OfflineMissionRepository): MissionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindBlockCollectionRepository(impl: ProgressBlockCollectionRepository): BlockCollectionRepository

    @Binds
    @Singleton
    abstract fun bindSandboxRepository(impl: DataStoreSandboxRepository): SandboxRepository

    @Binds
    abstract fun bindSoundPlayer(impl: ToneSoundPlayer): SoundPlayer

    @Binds
    abstract fun bindFrameTimer(impl: DelayFrameTimer): FrameTimer
}
