package com.forgis.ironmetrics.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun IronMetricsApp(viewModel: ExerciseViewModel) {
    val context = LocalContext.current
    val exercisesState by viewModel.exercises.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedId.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExport
        pendingExport = null
        if (uri != null && json != null) {
            viewModel.writeText(context.contentResolver, uri, json)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importFromUri(context.contentResolver, uri)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExerciseEvent.Message -> snackbar.showSnackbar(event.text)
                is ExerciseEvent.ExportReady -> {
                    pendingExport = event.json
                    exportLauncher.launch("ironmetrics_backup.json")
                }
            }
        }
    }

    BackHandler(enabled = selectedId != null) { viewModel.selectExercise(null) }

    Box {
        val id = selectedId
        if (id == null) {
            ExerciseListScreen(
                state = exercisesState,
                onExerciseClick = viewModel::selectExercise,
                onAddExercise = viewModel::addExercise,
                onExport = viewModel::exportBackup,
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
            )
        } else {
            val exercise = (exercisesState as? UiState.Success)?.data?.firstOrNull { it.id == id }
            ExerciseDetailScreen(
                exercise = exercise,
                onBack = { viewModel.selectExercise(null) },
                onAddSet = viewModel::addSet
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
