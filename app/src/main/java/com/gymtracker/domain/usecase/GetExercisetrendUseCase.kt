package com.gymtracker.domain.usecase

import com.gymtracker.domain.model.Exercise
import com.gymtracker.domain.model.ExerciseTrend
import com.gymtracker.domain.model.Session
import com.gymtracker.domain.model.TrendState
import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.domain.model.bestE1RM
import com.gymtracker.domain.model.bestHypertrophyScore
import com.gymtracker.domain.model.estimatedOneRM
import javax.inject.Inject

class GetExerciseTrendUseCase @Inject constructor() {

    operator fun invoke(
        exercise: Exercise,
        sessions: List<Session>,
        variant: String = ""
    ): ExerciseTrend? {

        fun historyFor(): List<Pair<String, WorkoutSet>> =
            sessions
                .flatMap { s -> s.sets.filter { it.exerciseId == exercise.id }.map { s.date to it } }
                .sortedBy { it.first }

        fun metricPerSession(history: List<Pair<String, WorkoutSet>>): List<Pair<String, Float>> {
            val filtered = if (variant.isBlank()) history
            else history.filter { it.second.variant == variant }
            return sessions
                .filter { s -> s.sets.any { it.exerciseId == exercise.id } }
                .sortedBy { it.date }
                .map { s ->
                    val sets = filtered.filter { it.first == s.date }.map { it.second }
                    val metric = when {
                        exercise.isCardio        -> sets.sumOf { it.reps }.toFloat()
                        exercise.isStrengthFocus -> bestE1RM(sets)
                        else                     -> bestHypertrophyScore(sets)
                    }
                    s.date to metric
                }
                .filter { it.second > 0f }
        }

        val history    = historyFor()
        val metricData = metricPerSession(history)
        if (metricData.isEmpty()) return null

        val allTime = metricData.maxOf { it.second }
        val last3   = metricData.takeLast(3)
        val latest  = last3.last().second
        val prev2   = last3.dropLast(1)
        val avg3    = if (prev2.isEmpty()) latest
        else prev2.map { it.second }.average().toFloat()

        val trend = when {
            last3.size >= 2 && latest > avg3 + 0.5f -> TrendState.PROGRESSING
            last3.size >= 3 && last3.zipWithNext().all { (a, b) -> b.second < a.second } -> TrendState.FATIGUE
            avg3 != 0f && ((latest - avg3) / avg3) * 100f < -3f -> TrendState.FATIGUE
            else -> TrendState.STAGNANT
        }

        val last5 = metricData.dropLast(1).takeLast(5)
        val avg5  = if (last5.isEmpty()) latest
        else last5.map { it.second }.average().toFloat()
        val pct5  = if (avg5 == 0f) 0f else ((latest - avg5) / avg5) * 100f

        return ExerciseTrend(
            exercise      = exercise,
            latestMetric  = latest,
            avgLast5Metric = avg3,
            pctChange     = pct5,
            trend         = trend,
            allTimeMetric = allTime,
            isStrength    = exercise.isStrengthFocus
        )
    }
}