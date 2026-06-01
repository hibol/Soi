package com.hibol.miette.soi.ui.navigation

import android.R.attr.defaultValue
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.ui.screens.HomeScreen
import com.hibol.miette.soi.ui.screens.NewDreamScreen
import com.hibol.miette.soi.ui.screens.NewEventScreen
import com.hibol.miette.soi.ui.screens.NewSessionScreen
import com.hibol.miette.soi.ui.screens.SetupScreen
import com.hibol.miette.soi.ui.screens.SplashScreen
import com.hibol.miette.soi.ui.viewmodel.SetupViewModel
import com.hibol.miette.soi.ui.viewmodel.SplashViewModel

object Routes {
    const val SETUP = "setup"
    const val SPLASH = "splash"
    const val HOME = "home"
    const val ENTRY_DETAIL = "entry/{entryId}"
    const val NEW_DREAM = "new_dream?date={date}"
    const val NEW_SESSION = "new_session?date={date}"
    const val NEW_EVENT = "new_event?date={date}"
    const val PARTS = "parts"
    const val PART_DETAIL = "part/{partId}"
    const val SETTINGS = "settings"

    fun newDream(date: Long? = null) = if (date != null) "new_dream?date=$date" else "new_dream"
    fun newSession(date: Long? = null) = if (date != null) "new_session?date=$date" else "new_session"
    fun newEvent(date: Long? = null) = if (date != null) "new_event?date=$date" else "new_event"
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SPLASH
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as SoiApplication

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.SPLASH) {
            val viewModel: SplashViewModel = viewModel(
                factory = SplashViewModel.Factory(app.container.profileRepository)
            )
            SplashScreen(navController = navController, viewModel = viewModel)
        }
        composable(Routes.SETUP) {
            val viewModel: SetupViewModel = viewModel(
                factory = SetupViewModel.Factory(app.container.profileRepository)
            )
            SetupScreen(navController = navController, viewModel = viewModel)
        }
        composable(Routes.HOME) {
            HomeScreen(navController)
        }
        composable(Routes.NEW_DREAM,
            arguments = listOf(navArgument("date") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getLong("date").takeIf { it != -1L }
            NewDreamScreen(navController = navController, initialDate = date)
        }
        composable(Routes.NEW_SESSION,
            arguments = listOf(navArgument("date") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getLong("date").takeIf { it != -1L }
            NewSessionScreen(navController = navController, initialDate = date)
        }
        composable(Routes.NEW_EVENT,
            arguments = listOf(navArgument("date") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getLong("date").takeIf { it != -1L }
            NewEventScreen(navController = navController, initialDate = date)
        }
        composable(Routes.PARTS) {
            // PartsScreen(navController)  — à venir
        }
        composable(Routes.SETTINGS) {
            // SettingsScreen(navController)  — à venir
        }
    }
}