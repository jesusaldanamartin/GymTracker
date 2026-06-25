package com.gymtracker

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymtracker.presentation.screens.GymViewModel
import com.gymtracker.presentation.screens.calendar.CalendarScreen
import com.gymtracker.presentation.screens.home.HomeScreen
import com.gymtracker.presentation.screens.progress.ProgressScreen
import com.gymtracker.presentation.screens.train.ExercisesScreen
import com.gymtracker.presentation.screens.train.SessionScreen
import com.gymtracker.presentation.screens.train.SummaryScreen
import com.gymtracker.presentation.theme.*
import dagger.hilt.android.AndroidEntryPoint
import android.os.Bundle
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GymApp() }
    }
}

sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    object Home     : Tab("home",     "Inicio",        Icons.Default.Home)
    object Train    : Tab("train",    "Entrenamiento", Icons.Default.FitnessCenter)
    object Calendar : Tab("calendar", "Historial",     Icons.Default.CalendarMonth)
    object Progress : Tab("progress", "Progreso",      Icons.AutoMirrored.Filled.TrendingUp)
}

val TABS = listOf(Tab.Home, Tab.Train, Tab.Calendar, Tab.Progress)

@Composable
fun GymApp() {
    val context = LocalContext.current
    val vm: GymViewModel = hiltViewModel()

    LaunchedEffect(Unit) { vm.loadAll(context) }

    MaterialTheme(colorScheme = darkColorScheme(
        primary        = Accent,    onPrimary    = Black,
        background     = Surface0,  onBackground = TextPrim,
        surface        = Surface1,  onSurface    = TextPrim,
        surfaceVariant = Surface2,  outline      = Border,
    )) {
        val nav      = rememberNavController()
        val navEntry by nav.currentBackStackEntryAsState()
        val current  = navEntry?.destination?.route
        val innerRoutes = setOf(Tab.Train.route, Tab.Calendar.route, Tab.Progress.route)

        Scaffold(containerColor = Surface0, bottomBar = {
            AnimatedVisibility(
                visible = current in innerRoutes,
                enter   = slideInVertically(initialOffsetY = { it }),
                exit    = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(containerColor = Surface1, tonalElevation = 0.dp) {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick  = {
                                if (tab == Tab.Home)
                                    nav.navigate(Tab.Home.route) {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                else nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon   = { Icon(tab.icon, contentDescription = tab.label) },
                            label  = { Text(tab.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Black,   selectedTextColor   = Accent,
                                unselectedIconColor = TextSec, unselectedTextColor = TextSec,
                                indicatorColor      = Accent
                            )
                        )
                    }
                }
            }
        }) { padding ->
            NavHost(
                navController    = nav,
                startDestination = Tab.Home.route,
                modifier         = Modifier.padding(padding)
            ) {
                composable(Tab.Home.route) {
                    HomeScreen(vm,
                        { nav.navigate(Tab.Train.route) },
                        { nav.navigate(Tab.Calendar.route) },
                        { nav.navigate(Tab.Progress.route) }
                    )
                }
                composable(Tab.Train.route)    { ExercisesScreen(vm) { nav.navigate("session") } }
                composable(Tab.Calendar.route) { CalendarScreen(vm) }
                composable(Tab.Progress.route) { ProgressScreen(vm) }
                composable("session") {
                    SessionScreen(vm,
                        onBack = { nav.popBackStack() },
                        onSave = {
                            vm.saveSession(context)
                            nav.navigate("summary") { popUpTo(Tab.Train.route) }
                        }
                    )
                }
                composable("summary") {
                    SummaryScreen(vm) {
                        nav.navigate(Tab.Home.route) { popUpTo(0) { inclusive = true } }
                    }
                }
            }
        }
    }
}