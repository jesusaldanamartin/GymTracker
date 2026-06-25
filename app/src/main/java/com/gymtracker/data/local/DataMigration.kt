package com.gymtracker.data.local

import android.content.Context
import android.util.Log
import com.gymtracker.data.local.db.GymDatabase
import com.gymtracker.data.local.db.SessionEntity
import com.gymtracker.data.local.db.mapper.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DataMigration {

    private const val PREFS_MIGRATION  = "gym_migration"
    private const val KEY_DONE         = "migration_v1_done"

    suspend fun migrateIfNeeded(context: Context, db: GymDatabase) {
        val prefs = context.getSharedPreferences(PREFS_MIGRATION, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return

        withContext(Dispatchers.IO) {
            try {
                Log.d("DataMigration", "Iniciando migración de SharedPreferences a Room...")

                // 1. Migrar sesiones y sets
                val sessions = Storage.load(context)
                Log.d("DataMigration", "Sesiones encontradas: ${sessions.size}")

                sessions.forEach { session ->
                    db.sessionDao().insertSession(SessionEntity(session.date))
                    val setEntities = session.sets.map { it.toEntity(session.date) }
                    db.workoutSetDao().insertSets(setEntities)
                }

                // 2. Migrar ejercicios custom
                val customExercises = Storage.loadCustomExercises(context)
                Log.d("DataMigration", "Ejercicios custom encontrados: ${customExercises.size}")
                if (customExercises.isNotEmpty()) {
                    db.exerciseDao().insertExercises(customExercises.map { it.toEntity() })
                }

                // 3. Migrar ejercicios importados
                val importedExercises = Storage.loadImportedExercises(context)
                Log.d("DataMigration", "Ejercicios importados encontrados: ${importedExercises.size}")
                if (importedExercises.isNotEmpty()) {
                    db.exerciseDao().insertExercises(importedExercises.map { it.toEntity() })
                }

                // 4. Marcar migración como completada
                prefs.edit().putBoolean(KEY_DONE, true).apply()
                Log.d("DataMigration", "Migración completada correctamente.")

            } catch (e: Exception) {
                Log.e("DataMigration", "Error durante la migración: ${e.message}", e)
                // No marcamos como done para que reintente en el siguiente arranque
            }
        }
    }

    fun isMigrationDone(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_MIGRATION, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DONE, false)
    }
}