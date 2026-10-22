package com.forgis.ironmetrics.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
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
fun IronMetricsApp(viewModel: ExerciseViewModel, wodViewModel: WodViewModel) {
    val context = LocalContext.current
    val exercisesState by viewModel.exercises.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedId.collectAsStateWithLifecycle()
    val wodsState by wodViewModel.wods.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(AppTab.Strength) }
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

    LaunchedEffect(Unit) {
        wodViewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    BackHandler(enabled = selectedId != null) { viewModel.selectExercise(null) }

    Scaffold(
        bottomBar = {
            if (selectedId == null) {
                NavigationBar {
                    AppTab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            icon = { Icon(entry.icon, contentDescription = entry.label) },
                            label = { Text(entry.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
    Box(Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
        val id = selectedId
        if (tab == AppTab.Wods) {
            WodListScreen(
                state = wodsState,
                onSave = wodViewModel::saveWod,
                onDelete = wodViewModel::deleteWod
            )
        } else if (id == null) {
            ExerciseListScreen(
                state = exercisesState,
                onExerciseClick = viewModel::selectExercise,
                onAddExercise = viewModel::addExercise,
                onDeleteExercise = viewModel::deleteExercise,
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
}

private enum class AppTab(val label: String, val icon: ImageVector) {
    Strength("Fuerza", Icons.Default.Star),
    Wods("WODs", Icons.Default.Home)
}
