package com.hibol.miette.soi.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.hibol.miette.soi.ui.auth.AuthSession
import com.hibol.miette.soi.ui.auth.launchBiometric
import com.hibol.miette.soi.ui.navigation.Routes
import com.hibol.miette.soi.ui.viewmodel.SplashState
import com.hibol.miette.soi.ui.viewmodel.SplashViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var biometricLaunched by remember { mutableStateOf(false) }
    var showRetry by remember { mutableStateOf(false) }

    val letterAlphas = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        letterAlphas.forEachIndexed { i, anim ->
            launch {
                delay(300L * (i + 1))
                anim.animateTo(1f, animationSpec = tween(durationMillis = 600))
            }
        }
    }

    fun triggerAuth() {
        showRetry = false
        launchBiometric(
            activity = context as FragmentActivity,
            onSuccess = {
                AuthSession.refresh()
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            },
            onError = { showRetry = true }
        )
    }

    LaunchedEffect(state) {
        when (state) {
            is SplashState.Loading -> return@LaunchedEffect

            is SplashState.NoProfile -> {
                navController.navigate(Routes.SETUP) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }

            is SplashState.HasProfile -> {
                if (biometricLaunched) return@LaunchedEffect
                biometricLaunched = true
                triggerAuth()
            }
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(modifier = Modifier.align(Alignment.TopCenter).padding(top = 140.dp)) {
                listOf("S", "o", "i").forEachIndexed { index, letter ->
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = letterAlphas[index].value
                        )
                    )
                }
            }

            if (showRetry) {
                Button(
                    onClick = { triggerAuth() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                ) {
                    Text("Déverrouiller")
                }
            }
        }
    }
}
