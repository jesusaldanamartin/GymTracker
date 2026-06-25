package com.gymtracker.data.local

import androidx.compose.ui.graphics.Color
import com.gymtracker.domain.model.Exercise

object ExerciseDefaults {

    val MUSCLE_COLORS = mapOf(
        "Pecho"   to Color(0xFFFD2D87),
        "Hombros" to Color(0xFFFFBE0B),
        "Triceps" to Color(0xFF8338EC),
        "Espalda" to Color(0xFF4ECDC4),
        "Biceps"  to Color(0xFF3A86FF),
        "Piernas" to Color(0xFFFF006E),
        "Gluteos" to Color(0xFFFB5607),
        "Core"    to Color(0xFF06D6A0),
        "Cardio"  to Color(0xFFE63946)
    )

    val ROUTINE_COLORS = mapOf(
        "Push"   to Color(0xFFFD2D87),
        "Pull"   to Color(0xFF4ECDC4),
        "Legs"   to Color(0xFFFF9500),
        "Full"   to Color(0xFF06D6A0),
        "Cardio" to Color(0xFFE63946),
    )

    val VARIANT_SUGGESTIONS_BY_EXERCISE: Map<Int, List<String>> = mapOf(
        1  to listOf("Agarre ancho", "Agarre cerrado", "Pausa abajo"),
        31 to listOf("Cable bajo", "Cable alto", "Cable medio", "Unilateral"),
        5  to listOf("Barra", "Mancuernas", "Máquina", "De pie"),
        6  to listOf("Cable", "Muñequeras", "Mancuernas"),
        7  to listOf("Unilateral"),
        8  to listOf("Cuerda", "Barra V", "Barra Plana"),
        33 to listOf("Mancuerna", "Cuerda", "Barra Plana"),
        10 to listOf("Agarre prono", "Agarre supino", "Agarre neutro", "Agarre ancho", "Agarre cerrado"),
        11 to listOf("Agarre espalda alta", "Agarre dorsal"),
        12 to listOf("Agarre prono ancho", "Mag Ancho", "Mag Mediano", "Mag Mediano Peq.", "Mag Pequeño"),
        13 to listOf("Pronado", "Supinado", "Neutro"),
        35 to listOf("Cuerda", "Mancuerna", "Barra Plana"),
        14 to listOf("Cable", "Banco Inclinado", "Estricto"),
        15 to listOf("Mancuernas", "Cuerda", "Estricto"),
        36 to listOf("Barra EZ", "Barra recta", "Máquina"),
        18 to listOf("Pausa abajo"),
        23 to listOf("Pausa abajo", "Pie elevado"),
        28 to listOf("Ritmo constante", "Intervalos", "HIIT", "Inclinación 0%", "Inclinación 5%", "Inclinación 10%"),
        29 to listOf("Ritmo constante", "Intervalos", "HIIT", "Resistencia baja", "Resistencia alta"),
        30 to listOf("Ritmo constante", "Intervalos", "Sprints", "Cadencia baja", "Cadencia alta"),
        43 to listOf("Ritmo constante", "Intervalos", "Alta velocidad", "Baja velocidad"),
    )

    val VARIANT_SUGGESTIONS_BY_MUSCLE = mapOf(
        "Pecho"   to listOf("Banco plano", "Banco 30°", "Banco 45°", "Declive", "Agarre ancho", "Agarre cerrado"),
        "Hombros" to listOf("Agarre neutro", "Agarre pronado", "Unilateral", "Cable bajo", "Cable alto"),
        "Triceps" to listOf("Agarre cerrado", "Agarre neutro", "Agarre inverso", "Unilateral", "Sobre cabeza"),
        "Espalda" to listOf("Agarre supino", "Agarre prono", "Agarre neutro", "Agarre ancho", "Agarre cerrado"),
        "Biceps"  to listOf("Agarre supino", "Agarre neutro", "Agarre prono", "Inclinado", "Predicador"),
        "Piernas" to listOf("Stance ancho", "Stance estrecho", "Pies altos", "Pies bajos", "Unilateral"),
        "Gluteos" to listOf("Pie elevado", "Banda", "Unilateral", "Pausa arriba"),
        "Core"    to listOf("Rodillas dobladas", "Piernas rectas", "Con peso", "Sin peso"),
        "Cardio"  to listOf("Ritmo constante", "Intervalos", "HIIT"),
    )

    fun variantSuggestionsFor(exercise: Exercise): List<String> =
        VARIANT_SUGGESTIONS_BY_EXERCISE[exercise.id]
            ?: VARIANT_SUGGESTIONS_BY_MUSCLE[exercise.muscle]
            ?: emptyList()
}