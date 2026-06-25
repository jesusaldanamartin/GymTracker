package com.gymtracker

import android.app.Application
import android.util.Log
import com.gymtracker.data.local.DataMigration
import com.gymtracker.data.local.db.GymDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GymTrackerApp : Application() {

    @Inject
    lateinit var database: GymDatabase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        runMigrationIfNeeded()
    }

    private fun runMigrationIfNeeded() {
        if (DataMigration.isMigrationDone(this)) return
        applicationScope.launch {
            try {
                DataMigration.migrateIfNeeded(this@GymTrackerApp, database)
            } catch (e: Exception) {
                Log.e("GymTrackerApp", "Migration failed: ${e.message}", e)
            }
        }
    }
}