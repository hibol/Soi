package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.repository.EmotionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val emotionRepository: EmotionRepository) : ViewModel() {

    val primaryEmotions = emotionRepository.getPrimaryEmotions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateColor(emotion: Emotion, hexColor: String) {
        viewModelScope.launch {
            emotionRepository.update(emotion.copy(color = hexColor))
        }
    }

    class Factory(private val emotionRepository: EmotionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(emotionRepository) as T
    }
}
