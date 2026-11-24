package au.edu.jcu.codequest.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import au.edu.jcu.codequest.data.local.AppDatabase
import au.edu.jcu.codequest.data.local.CurriculumDao
import au.edu.jcu.codequest.data.local.MissionDao
import au.edu.jcu.codequest.data.local.ProgressDao
import au.edu.jcu.codequest.data.local.TriviaDao
import au.edu.jcu.codequest.data.remote.TriviaApi
import au.edu.jcu.codequest.util.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Marks the dispatcher used for blocking I/O so tests can swap in a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** App-wide singletons: database, network client, preferences and clock. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val SETTINGS_FILE = "settings"
    private const val NETWORK_TIMEOUT_SECONDS = 10L

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideCurriculumDao(database: AppDatabase): CurriculumDao = database.curriculumDao()

    @Provides
    fun provideProgressDao(database: AppDatabase): ProgressDao = database.progressDao()

    @Provides
    fun provideTriviaDao(database: AppDatabase): TriviaDao = database.triviaDao()

    @Provides
    fun provideMissionDao(database: AppDatabase): MissionDao = database.missionDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideTriviaApi(client: OkHttpClient): TriviaApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(TriviaApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TriviaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(SETTINGS_FILE) }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    fun provideTimeProvider(): TimeProvider = TimeProvider { System.currentTimeMillis() }
}
