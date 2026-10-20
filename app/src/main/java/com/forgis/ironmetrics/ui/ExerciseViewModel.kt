package com.forgis.ironmetrics.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewModelScope
import com.forgis.ironmetrics.data.BackupManager
import com.forgis.ironmetrics.data.ExerciseEntity
import com.forgis.ironmetrics.data.ExerciseRepository
import com.forgis.ironmetrics.data.SetLogEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Eventos puntuales para la UI (snackbars, compartir JSON...). */
sealed interface ExerciseEvent {
    data class Message(val text: String) : ExerciseEvent
    data class ExportReady(val json: String) : ExerciseEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModel(
    private val repository: ExerciseRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    val exercises: StateFlow<UiState<List<ExerciseEntity>>> = repository.exercises
        .map<List<ExerciseEntity>, UiState<List<ExerciseEntity>>> { UiState.Success(it) }
        .onStart { emit(UiState.Loading) }
        .catch { emit(UiState.Error(it.message ?: "Error loading exercises")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val selectedExerciseId = MutableStateFlow<Long?>(null)
    val selectedId: StateFlow<Long?> = selectedExerciseId.asStateFlow()

    val history: StateFlow<UiState<List<SetLogEntity>>> = selectedExerciseId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.history(id)
                    .map<List<SetLogEntity>, UiState<List<SetLogEntity>>> { UiState.Success(it) }
                    .onStart { emit(UiState.Loading) }
                    .catch { emit(UiState.Error(it.message ?: "Error loading history")) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Success(emptyList()))

    private val _events = MutableSharedFlow<ExerciseEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ExerciseEvent> = _events.asSharedFlow()

    fun selectExercise(id: Long?) {
        selectedExerciseId.value = id
    }

    fun addExercise(name: String) = launchSafely("Error saving exercise") {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Name cannot be empty" }
        repository.saveExercise(ExerciseEntity(name = trimmed))
    }

    fun addSet(weight: Double, reps: Int) {
        val id = selectedExerciseId.value ?: return emit(ExerciseEvent.Message("Select an exercise first"))
        launchSafely("Error saving set") { repository.logSet(id, weight, reps) }
    }

    fun exportBackup() = launchSafely("Export failed") {
        _events.emit(ExerciseEvent.ExportReady(backupManager.exportToJson()))
    }

    fun importBackup(json: String, replace: Boolean = false) = launchSafely("Import failed") {
        backupManager.importFromJson(json, replace)
        _events.emit(ExerciseEvent.Message("Import completed"))
    }

    fun writeText(resolver: ContentResolver, uri: Uri, text: String) = launchSafely("Export failed") {
        withContext(Dispatchers.IO) {
            resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) }
                ?: error("Cannot open file")
        }
        _events.emit(ExerciseEvent.Message("Export completed"))
    }

    fun importFromUri(resolver: ContentResolver, uri: Uri, replace: Boolean = false) =
        launchSafely("Import failed") {
            val json = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Cannot open file")
            }
            backupManager.importFromJson(json, replace)
            _events.emit(ExerciseEvent.Message("Import completed"))
        }

    private fun emit(event: ExerciseEvent) {
        _events.tryEmit(event)
    }

    private fun launchSafely(errorPrefix: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _events.emit(ExerciseEvent.Message("$errorPrefix: ${e.message ?: "unknown error"}"))
            }
        }
    }

    class Factory(
        private val repository: ExerciseRepository,
        private val backupManager: BackupManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExerciseViewModel(repository, backupManager) as T
    }
}
