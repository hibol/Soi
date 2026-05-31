package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SetupViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profileCreated = MutableStateFlow(false)
    val profileCreated: StateFlow<Boolean> = _profileCreated

    fun createProfile(name: String) {
        viewModelScope.launch {
            profileRepository.createProfile(name.trim())
            _profileCreated.value = true
        }
    }

    class Factory(private val profileRepository: ProfileRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SetupViewModel(profileRepository) as T
        }
    }
}