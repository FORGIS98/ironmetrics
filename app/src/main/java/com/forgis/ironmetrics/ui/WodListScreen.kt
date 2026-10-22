package com.forgis.ironmetrics.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.forgis.ironmetrics.data.WodEntity
import com.forgis.ironmetrics.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WodListScreen(
    state: UiState<List<WodEntity>>,
    onSave: (id: Long, name: String, timeSeconds: Int, notes: String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // null = diálogo cerrado; WodEntity() con id 0 = nuevo WOD
    var editing by remember { mutableStateOf<WodEntity?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("WODs") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = WodEntity(name = "") }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo WOD")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is UiState.Error -> Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
                is UiState.Success -> if (state.data.isEmpty()) {
                    Text(
                        "Sin WODs. Pulsa + para crear uno.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.data, key = { it.id }) { wod ->
                            WodRow(wod, onClick = { editing = wod }, onDelete = { onDelete(wod.id) })
                        }
                    }
                }
            }
        }
    }

    editing?.let { wod ->
        WodDialog(
            wod = wod,
            onDismiss = { editing = null },
            onConfirm = { name, seconds, notes ->
                onSave(wod.id, name, seconds, notes)
                editing = null
            }
        )
    }
}

@Composable
private fun WodRow(wod: WodEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(wod.name, style = MaterialTheme.typography.titleMedium)
                Text(TimeUtils.format(wod.timeSeconds), style = MaterialTheme.typography.titleLarge)
                if (wod.notes.isNotBlank()) {
                    Text(
                        wod.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Eliminar") },
                onClick = { menuExpanded = false; onDelete() }
            )
        }
    }
}

@Composable
private fun WodDialog(
    wod: WodEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, timeSeconds: Int, notes: String) -> Unit
) {
    val isNew = wod.id == 0L
    var name by remember { mutableStateOf(wod.name) }
    var time by remember { mutableStateOf(TimeUtils.format(wod.timeSeconds)) }
    var notes by remember { mutableStateOf(wod.notes) }
    val seconds = TimeUtils.parse(time)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Nuevo WOD" else "Editar WOD") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Tiempo (HH:MM:SS)") },
                    isError = seconds == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / comentarios") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, seconds!!, notes) },
                enabled = name.isNotBlank() && seconds != null
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
