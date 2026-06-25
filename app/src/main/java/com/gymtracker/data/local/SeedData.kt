package com.gymtracker.data.local

import androidx.compose.ui.graphics.Color
import com.gymtracker.domain.model.Exercise

object SeedData {

    val EXERCISES = listOf(
        Exercise(1,  "Press Banca",               "Pecho",   "Push",   "💪", Color(0xFFFF6B6B), true),
        Exercise(2,  "Press Inclinado",            "Pecho",   "Push",   "💪", Color(0xFFFF6B6B), true),
        Exercise(42, "Press Inclinado Manc.",      "Pecho",   "Push",   "💪", Color(0xFFFF6B6B), true),
        Exercise(31, "Aperturas Cable",            "Pecho",   "Push",   "💪", Color(0xFFFF6B6B)),
        Exercise(3,  "Peck Deck",                  "Pecho",   "Push",   "💪", Color(0xFFFF6B6B)),
        Exercise(4,  "Fondos",                     "Pecho",   "Push",   "💪", Color(0xFFFF6B6B)),
        Exercise(5,  "Press Militar",              "Hombros", "Push",   "🏋️", Color(0xFFFFBE0B), true),
        Exercise(6,  "Elevaciones Lat.",           "Hombros", "Push",   "🏋️", Color(0xFFFFBE0B)),
        Exercise(7,  "Reversed Peck Deck",         "Hombros", "Pull",   "🏋️", Color(0xFFFFBE0B)),
        Exercise(17, "Facepull",                   "Hombros", "Pull",   "🎯", Color(0xFFFFBE0B)),
        Exercise(8,  "Extensiones Triceps",        "Triceps", "Push",   "💪", Color(0xFF8338EC)),
        Exercise(9,  "Extensiones Katana",         "Triceps", "Push",   "💪", Color(0xFF8338EC)),
        Exercise(32, "Extensiones Unilateral",     "Triceps", "Push",   "💪", Color(0xFF8338EC)),
        Exercise(33, "Extensiones sobre cabeza",   "Triceps", "Push",   "💪", Color(0xFF8338EC)),
        Exercise(10, "Dominadas",                  "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
        Exercise(11, "Remo en T",                  "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
        Exercise(12, "Jalón al Pecho",             "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
        Exercise(13, "Remo Máquina Unilateral",    "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
        Exercise(34, "Remo Polea Unilateral",      "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
        Exercise(35, "Pull Over",                  "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
        Exercise(14, "Curl Bíceps Unilateral",     "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),
        Exercise(15, "Curl Martillo",              "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),
        Exercise(16, "Curl Bayesian",              "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),
        Exercise(36, "Curl Predicador",            "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),
        Exercise(18, "Sentadilla Libre",           "Piernas", "Legs",   "🦵", Color(0xFFFF006E), true),
        Exercise(23, "Sentadilla MultiPower",      "Piernas", "Legs",   "🦵", Color(0xFFFF006E), true),
        Exercise(37, "Peso Muerto",                "Piernas", "Legs",   "🦵", Color(0xFFFF006E), true),
        Exercise(38, "Peso Muerto Sumo",           "Piernas", "Legs",   "🦵", Color(0xFFFF006E), true),
        Exercise(39, "Peso Muerto Rumano",         "Piernas", "Legs",   "🦵", Color(0xFFFF006E), true),
        Exercise(19, "Prensa de Piernas",          "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
        Exercise(20, "Extensiones Cuad.",          "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
        Exercise(21, "Curl Femoral",               "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
        Exercise(22, "Hip Thrust",                 "Gluteos", "Legs",   "🍑", Color(0xFFFB5607)),
        Exercise(24, "Gemelos de Pie",             "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
        Exercise(40, "Aducciones",                 "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
        Exercise(41, "Abducciones",                "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
        Exercise(25, "Elevaciones de piernas",     "Core",    "Full",   "🎯", Color(0xFF06D6A0)),
        Exercise(26, "Crunch Polea",               "Core",    "Full",   "🎯", Color(0xFF06D6A0)),
        Exercise(27, "Rueda Abdominal",            "Core",    "Full",   "🎯", Color(0xFF06D6A0)),
        Exercise(28, "Cinta de Correr",            "Cardio",  "Cardio", "❤️", Color(0xFFE63946), isCardio = true),
        Exercise(29, "Bicicleta Est.",             "Cardio",  "Cardio", "❤️", Color(0xFFE63946), isCardio = true),
        Exercise(30, "Remo Ergómetro",             "Cardio",  "Cardio", "❤️", Color(0xFFE63946), isCardio = true),
        Exercise(43, "Máquina de Escalera",        "Cardio",  "Cardio", "❤️", Color(0xFFE63946), isCardio = true),
    )

    val MUSCLES  = listOf("Todos","Pecho","Hombros","Triceps","Espalda","Biceps","Piernas","Gluteos","Core","Cardio")
    val ROUTINES = listOf("Todas","Push","Pull","Legs","Full","Cardio")
}