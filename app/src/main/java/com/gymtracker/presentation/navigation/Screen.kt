package com.gymtracker.presentation.navigation

sealed class Screen(val route: String) {
    object Home           : Screen("home")
    object Train          : Screen("train")
    object Calendar       : Screen("calendar")
    object Progress       : Screen("progress")
    object Session        : Screen("session")
    object Summary        : Screen("summary")
}