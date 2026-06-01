package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed class SplashState {
    object Loading : SplashState()
    object NoProfile : SplashState()
    object HasProfile : SplashState()
}

class SplashViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val state: StateFlow<SplashState> =
        profileRepository.hasProfile()
            .map { hasProfile ->
                if (hasProfile) SplashState.HasProfile else SplashState.NoProfile
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SplashState.Loading
            )

    class Factory(private val profileRepository: ProfileRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SplashViewModel(profileRepository) as T
        }
    }
}