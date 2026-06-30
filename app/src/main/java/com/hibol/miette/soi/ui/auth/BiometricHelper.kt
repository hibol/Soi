package com.hibol.miette.soi.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// BIOMETRIC_STRONG seul (empreinte, Class 3) est préféré.
// Repli sur BIOMETRIC_WEAK (visage Class 2) si STRONG non disponible.
// DEVICE_CREDENTIAL (PIN système) volontairement exclu : il nécessite API 30+
// et dilue la sécurité propre à la biométrie.
fun launchBiometric(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: () -> Unit = {}
) {
    val manager = BiometricManager.from(activity)
    val authenticators = when {
        manager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS -> BIOMETRIC_STRONG
        manager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS -> BIOMETRIC_WEAK
        else -> null
    }

    if (authenticators == null) {
        // Aucun hardware disponible → on laisse passer (pas de régression par rapport à "sans auth").
        onSuccess()
        return
    }

    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                onSuccess()

            // onAuthenticationFailed (mauvais doigt) est géré par le dialog système — pas besoin
            // de l'intercepter ici, Android affiche lui-même le retry.
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                onError()
        }
    )

    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Soi")
            .setSubtitle("Déverrouiller")
            .setAllowedAuthenticators(authenticators)
            .setNegativeButtonText("Annuler")
            .build()
    )
}
