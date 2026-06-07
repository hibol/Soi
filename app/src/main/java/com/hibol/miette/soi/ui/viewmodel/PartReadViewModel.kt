package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Part
import com.hibol.miette.soi.data.entity.PartTrait
import com.hibol.miette.soi.data.repository.PartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PartReadViewModel(
    private val partRepository: PartRepository
) : ViewModel() {

    val part = MutableStateFlow<Part?>(null)
    val traits = MutableStateFlow<List<PartTrait>>(emptyList())
    val isDeleted = MutableStateFlow(false)

    fun load(partId: Long) {
        viewModelScope.launch {
            partRepository.getById(partId).collect { part.value = it }
        }
        viewModelScope.launch {
            partRepository.getTraitsForPart(partId).collect { traits.value = it }
        }
    }

    fun delete(partId: Long) {
        viewModelScope.launch {
            partRepository.delete(partId)
            isDeleted.value = true
        }
    }

    class Factory(
        private val partRepository: PartRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PartReadViewModel(partRepository) as T
    }
}
