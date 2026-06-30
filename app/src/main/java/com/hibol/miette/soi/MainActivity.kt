package com.hibol.miette.soi

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.hibol.miette.soi.ui.navigation.NavGraph
import com.hibol.miette.soi.ui.screens.LockScreen
import com.hibol.miette.soi.ui.theme.SoiTheme
import com.hibol.miette.soi.ui.viewmodel.LockViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoiTheme {
                val lockViewModel: LockViewModel = viewModel()
                val isLocked by lockViewModel.isLocked.collectAsState()

                // Observe ON_START pour détecter les retours depuis l'arrière-plan.
                // DisposableEffect utilise le lifecycle de l'Activity (LocalLifecycleOwner).
                // La rotation déclenche aussi ON_START, mais AuthSession.isExpired est false
                // dans ce cas (quelques millisecondes se sont écoulées).
                DisposableEffect(Unit) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_START) lockViewModel.onForeground()
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }

                val navController = rememberNavController()
                NavGraph(navController = navController)

                if (isLocked) {
                    LockScreen(onAuthenticated = lockViewModel::onAuthenticated)
                }
            }
        }
    }
}
