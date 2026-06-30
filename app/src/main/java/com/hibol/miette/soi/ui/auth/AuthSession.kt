package com.hibol.miette.soi.ui.auth

// Durée de vie d'une session : si l'app reste en arrière-plan plus longtemps, on re-demande l'auth.
private const val SESSION_TIMEOUT_MS = 5 * 60 * 1_000L

object AuthSession {

    private var lastAuthTime: Long = 0L

    // isExpired est false quand lastAuthTime == 0 (pas encore authentifié dans ce processus).
    // L'auth initiale est gérée par SplashScreen ; ce verrou ne concerne que les retours depuis
    // l'arrière-plan après une session déjà établie.
    val isExpired: Boolean
        get() = lastAuthTime > 0L && System.currentTimeMillis() - lastAuthTime > SESSION_TIMEOUT_MS

    fun refresh() {
        lastAuthTime = System.currentTimeMillis()
    }
}
