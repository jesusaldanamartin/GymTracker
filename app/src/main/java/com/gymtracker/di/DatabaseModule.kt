package com.gymtracker.di

import android.content.Context
import androidx.room.Room
import com.gymtracker.data.local.db.ExerciseDao
import com.gymtracker.data.local.db.GymDatabase
import com.gymtracker.data.local.db.SessionDao
import com.gymtracker.data.local.db.WorkoutSetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GymDatabase =
        Room.databaseBuilder(
            context,
            GymDatabase::class.java,
            "gym_tracker.db"
        ).build()

    @Provides
    fun provideSessionDao(db: GymDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideWorkoutSetDao(db: GymDatabase): WorkoutSetDao = db.workoutSetDao()

    @Provides
    fun provideExerciseDao(db: GymDatabase): ExerciseDao = db.exerciseDao()
}