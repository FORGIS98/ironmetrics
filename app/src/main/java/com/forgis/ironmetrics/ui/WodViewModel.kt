package com.forgis.ironmetrics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forgis.ironmetrics.data.WodEntity
import com.forgis.ironmetrics.data.WodRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WodViewModel(private val repository: WodRepository) : ViewModel() {

    val wods: StateFlow<UiState<List<WodEntity>>> = repository.wods
        .map<List<WodEntity>, UiState<List<WodEntity>>> { UiState.Success(it) }
        .onStart { emit(UiState.Loading) }
        .catch { emit(UiState.Error(it.message ?: "Error loading WODs")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun saveWod(id: Long, name: String, timeSeconds: Int, notes: String) = launchSafely("Error saving WOD") {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Name cannot be empty" }
        repository.saveWod(WodEntity(id = id, name = trimmed, timeSeconds = timeSeconds, notes = notes.trim()))
    }

    fun deleteWod(id: Long) = launchSafely("Error deleting WOD") {
        repository.deleteWod(id)
        _messages.emit("WOD eliminado")
    }

    private fun launchSafely(errorPrefix: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _messages.emit("$errorPrefix: ${e.message ?: "unknown error"}")
            }
        }
    }

    class Factory(private val repository: WodRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WodViewModel(repository) as T
    }
}
