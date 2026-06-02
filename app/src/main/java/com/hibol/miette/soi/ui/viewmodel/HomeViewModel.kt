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
import java.time.YearMonth

class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val entryRepository: EntryRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    private val _profileName = MutableStateFlow("")
    val profileName: StateFlow<String> = _profileName

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val _profileId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            profileRepository.getProfile().collectLatest { profile ->
                profile?.let {
                    _profileName.value = it.name
                    _profileId.value = it.id
                    entryRepository.getAllByProfile(it.id).collectLatest { entries ->
                        _entries.value = entries
                    }
                }
            }
        }
    }

    fun setMonth(month: YearMonth) {
        _currentMonth.value = month
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