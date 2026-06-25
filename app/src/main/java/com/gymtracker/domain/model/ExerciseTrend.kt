package com.gymtracker.domain.model

enum class TrendState { PROGRESSING, STAGNANT, FATIGUE }

data class ExerciseTrend(
    val exercise: Exercise,
    val latestMetric: Float,
    val avgLast5Metric: Float,
    val pctChange: Float,
    val trend: TrendState,
    val allTimeMetric: Float,
    val isStrength: Boolean
)