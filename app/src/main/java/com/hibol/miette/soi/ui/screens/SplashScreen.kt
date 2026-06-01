package com.hibol.miette.soi.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
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

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state) {
        when (state) {
            is SplashState.Loading -> return@LaunchedEffect

            is SplashState.NoProfile -> {
                navController.navigate(Routes.SETUP) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }

            is SplashState.HasProfile -> {
                val biometricManager = BiometricManager.from(context)
                val canAuthenticate = biometricManager.canAuthenticate(
                    BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                )

                if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
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
                                // Auth annulée — on reste sur le splash
                                // L'utilisateur peut réessayer
                            }

                            override fun onAuthenticationFailed() {
                                // Tentative échouée — le prompt reste ouvert
                            }
                        }
                    )

                    prompt.authenticate(
                        BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Soi")
                            .setSubtitle("Déverrouiller")
                            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                            .build()
                    )
                } else {
                    // Pas de biométrie disponible → accès direct
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
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}