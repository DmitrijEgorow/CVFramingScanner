package ru.samolet.indoorinspection.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.samolet.indoorinspection.dataStore


interface CacheRepository {

    val cachedData: Flow<String?>
    suspend fun saveData(data: String)

    class LocalCacheRepository(private val context: Context) : CacheRepository {
        private val dataStore = context.dataStore
        override val cachedData: Flow<String?> = dataStore.data.map { preferences ->
            preferences[CACHED_DATA_KEY]
        }

        override suspend fun saveData(data: String) {
            dataStore.edit { preferences ->
                preferences[CACHED_DATA_KEY] = data
            }
        }

        companion object {
            private val CACHED_DATA_KEY = stringPreferencesKey("data_key")
        }
    }
}