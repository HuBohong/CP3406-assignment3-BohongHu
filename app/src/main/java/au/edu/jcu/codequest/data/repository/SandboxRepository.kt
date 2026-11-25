package au.edu.jcu.codequest.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import au.edu.jcu.codequest.domain.mission.SandboxMap
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** The planet the learner designed in free-build mode, kept on the device between visits. */
interface SandboxRepository {
    val map: Flow<SandboxMap>
    suspend fun save(map: SandboxMap)
}

class DataStoreSandboxRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SandboxRepository {

    override val map: Flow<SandboxMap> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> SandboxMap.decode(prefs[MAP_KEY]) ?: SandboxMap.DEFAULT }

    override suspend fun save(map: SandboxMap) {
        dataStore.edit { it[MAP_KEY] = map.encode() }
    }

    private companion object {
        val MAP_KEY = stringPreferencesKey("sandbox_map")
    }
}
