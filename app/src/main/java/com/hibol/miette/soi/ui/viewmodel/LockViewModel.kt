package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.hibol.miette.soi.ui.auth.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LockViewModel : ViewModel() {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Appelé à chaque ON_START de l'activité (retour depuis l'arrière-plan ou rotation).
    // La rotation est inoffensive : AuthSession.isExpired sera false (quelques ms se sont écoulées).
    fun onForeground() {
        if (AuthSession.isExpired) _isLocked.value = true
    }

    fun onAuthenticated() {
        AuthSession.refresh()
        _isLocked.value = false
    }
}
