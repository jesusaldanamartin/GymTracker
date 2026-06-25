package com.gymtracker.domain.model

import androidx.compose.ui.graphics.Color

data class Exercise(
    val id: Int,
    val name: String,
    val muscle: String,
    val routine: String,
    val emoji: String,
    val color: Color,
    val isStrengthFocus: Boolean = false,
    val isCustom: Boolean = false,
    val isCardio: Boolean = false
)