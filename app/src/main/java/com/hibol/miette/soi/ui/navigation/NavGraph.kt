package com.hibol.miette.soi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.ui.screens.EntryDetailScreen
import com.hibol.miette.soi.ui.screens.HomeScreen
import com.hibol.miette.soi.ui.screens.NewEntryScreen
import com.hibol.miette.soi.ui.screens.PartDetailScreen
import com.hibol.miette.soi.ui.screens.PartReadScreen
import com.hibol.miette.soi.ui.screens.PartsListScreen
import com.hibol.miette.soi.ui.screens.SetupScreen
import com.hibol.miette.soi.ui.screens.SplashScreen
import com.hibol.miette.soi.ui.viewmodel.SetupViewModel
import com.hibol.miette.soi.ui.viewmodel.SplashViewModel

object Routes {
    const val SETUP = "setup"
    const val SPLASH = "splash"
    const val HOME = "home"
    const val ENTRY_DETAIL = "entry_detail/{entryId}"
    fun entryDetail(entryId: Long) = "entry_detail/$entryId"
    const val NEW_ENTRY = "new_entry/{type}?date={date}&entryId={entryId}"
    const val PARTS = "parts"
    const val PART_READ = "part_read/{partId}"
    const val PART_DETAIL = "part/{partId}?role={role}"
    const val SETTINGS = "settings"

    fun partRead(partId: Long) = "part_read/$partId"

    fun partDetail(partId: Long = 0L, role: String? = null): String {
        val base = "part/$partId"
        return if (role != null) "$base?role=$role" else base
    }

    fun newEntry(type: EntryType, date: Long? = null, entryId: Long? = null): String {
        var route = "new_entry/${type.value}"
        val params = mutableListOf<String>()
        if (date != null) params.add("date=$date")
        if (entryId != null) params.add("entryId=$entryId")
        if (params.isNotEmpty()) route += "?${params.joinToString("&")}"
        return route
    }
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
        composable(
            route = "new_entry/{type}?date={date}&entryId={entryId}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("date") { type = NavType.LongType; defaultValue = -1L },
                navArgument("entryId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val type = EntryType.entries.first {
                it.value == backStackEntry.arguments?.getString("type")
            }
            val date = backStackEntry.arguments?.getLong("date").takeIf { it != -1L }
            val entryId = backStackEntry.arguments?.getLong("entryId").takeIf { it != -1L }
            NewEntryScreen(
                navController = navController,
                type = type,
                initialDate = date,
                entryId = entryId
            )
        }
        composable(
            route = Routes.ENTRY_DETAIL,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: return@composable
            EntryDetailScreen(navController = navController, entryId = entryId)
        }
        composable(Routes.PARTS) {
            PartsListScreen(navController)
        }
        composable(
            route = Routes.PART_READ,
            arguments = listOf(navArgument("partId") { type = NavType.LongType })
        ) { backStackEntry ->
            val partId = backStackEntry.arguments?.getLong("partId") ?: return@composable
            PartReadScreen(navController = navController, partId = partId)
        }
        composable(
            route = Routes.PART_DETAIL,
            arguments = listOf(
                navArgument("partId") { type = NavType.LongType },
                navArgument("role") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val partId = backStackEntry.arguments?.getLong("partId").takeIf { it != 0L }
            val role = backStackEntry.arguments?.getString("role")
            PartDetailScreen(navController = navController, partId = partId, initialRole = role)
        }
        composable(Routes.SETTINGS) {
            // SettingsScreen(navController)  — à venir
        }
    }
}