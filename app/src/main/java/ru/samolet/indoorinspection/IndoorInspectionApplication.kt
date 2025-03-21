package ru.samolet.indoorinspection

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import java.util.Random
import ru.samolet.indoorinspection.data.CacheRepository
import ru.samolet.indoorinspection.data.FeatureService

class IndoorInspectionApplication: Application() {
    val rnd = Random(8)
    lateinit var cacheRepository: CacheRepository
        private set
    lateinit var featureService: FeatureService
        private set

    override fun onCreate() {
        super.onCreate()
        cacheRepository = CacheRepository.LocalCacheRepository(this)
        featureService = FeatureService()
    }
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")