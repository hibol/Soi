package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val entryRepository: EntryRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    private val _profileId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            profileRepository.getProfile().collectLatest { profile ->
                profile?.let {
                    _profileId.value = it.id
                    entryRepository.getAllByProfile(it.id).collectLatest { entries ->
                        _entries.value = entries
                    }
                }
            }
        }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val entryRepository: EntryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(profileRepository, entryRepository) as T
        }
    }
}