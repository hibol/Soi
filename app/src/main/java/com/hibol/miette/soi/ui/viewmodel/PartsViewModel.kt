package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Part
import com.hibol.miette.soi.data.repository.PartRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PartsViewModel(
    private val partRepository: PartRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _parts = MutableStateFlow<List<Part>>(emptyList())
    val parts: StateFlow<List<Part>> = _parts

    init {
        viewModelScope.launch {
            val profile = profileRepository.getProfile().filterNotNull().first()
            partRepository.getAllByProfile(profile.id).collect { _parts.value = it }
        }
    }

    fun deletePart(id: Long) {
        viewModelScope.launch { partRepository.delete(id) }
    }

    class Factory(
        private val partRepository: PartRepository,
        private val profileRepository: ProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PartsViewModel(partRepository, profileRepository) as T
    }
}
