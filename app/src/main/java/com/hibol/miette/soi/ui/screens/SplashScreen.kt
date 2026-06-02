package com.hibol.miette.soi.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
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

    // Un Animatable par lettre — chaque lettre passe de alpha 0 à 1 avec un délai décalé
    val letterAlphas = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        letterAlphas.forEachIndexed { i, anim ->
            launch {
                delay(300L * (i + 1))
                anim.animateTo(1f, animationSpec = tween(durationMillis = 600))
            }
        }
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

                val biometricManager = BiometricManager.from(context)

                // BIOMETRIC_STRONG | DEVICE_CREDENTIAL n'est supporté qu'à partir d'API 30.
                // On teste d'abord STRONG seul (empreinte = Class 3), puis WEAK (visage Class 2).
                val authenticators = when {
                    biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS ->
                        BIOMETRIC_STRONG
                    biometricManager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS ->
                        BIOMETRIC_WEAK
                    else -> null
                }

                if (authenticators != null) {
                    val executor = ContextCompat.getMainExecutor(context)
                    val prompt = BiometricPrompt(
                        context as FragmentActivity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(
                                result: BiometricPrompt.AuthenticationResult
                            ) {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }

                            override fun onAuthenticationError(
                                errorCode: Int,
                                errString: CharSequence
                            ) {
                                // Auth annulée ou trop d'essais — on reste bloqué sur le splash
                            }

                            override fun onAuthenticationFailed() {
                                // Tentative échouée — le prompt reste ouvert, Android gère le retry
                            }
                        }
                    )

                    prompt.authenticate(
                        BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Soi")
                            .setSubtitle("Déverrouiller")
                            .setAllowedAuthenticators(authenticators)
                            // setNegativeButtonText est obligatoire quand on n'utilise pas DEVICE_CREDENTIAL
                            .setNegativeButtonText("Annuler")
                            .build()
                    )
                } else {
                    // Aucune biométrie disponible sur cet appareil → accès direct
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
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
        }
    }
}