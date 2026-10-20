package com.forgis.ironmetrics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.forgis.ironmetrics.data.ExerciseEntity
import com.forgis.ironmetrics.util.BrzyckiUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exercise: ExerciseEntity?,
    onBack: () -> Unit,
    onAddSet: (weight: Double, reps: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = { AddSetForm(onAddSet) }
    ) { padding ->
        if (exercise == null) return@Scaffold

        val bestOneRm = maxOf(exercise.realOneRm ?: 0.0, exercise.theoreticalOneRm)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RmCard("RM Real", exercise.realOneRm?.let { formatKg(it) } ?: "—", Modifier.weight(1f))
                    RmCard("RM Teórico Máx.", formatKg(exercise.theoreticalOneRm), Modifier.weight(1f))
                }
            }
            if (bestOneRm <= 0.0) {
                item {
                    Text(
                        "Registra una serie para ver tu tabla de porcentajes.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item {
                    Text("Porcentajes sobre ${formatKg(bestOneRm)}", style = MaterialTheme.typography.titleSmall)
                }
                items(BrzyckiUtils.percentageTable(bestOneRm), key = { it.first }) { (percent, weight) ->
                    Column {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text("$percent%", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Text(formatKg(weight))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RmCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun AddSetForm(onAddSet: (Double, Int) -> Unit) {
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    val weightValue = weight.replace(',', '.').toDoubleOrNull()
    val repsValue = reps.toIntOrNull()
    val valid = weightValue != null && weightValue > 0 && repsValue != null && repsValue >= 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .imePadding()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Peso (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = reps,
            onValueChange = { reps = it.filter(Char::isDigit) },
            label = { Text("Reps") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(0.7f)
        )
        Button(
            enabled = valid,
            onClick = {
                onAddSet(weightValue!!, repsValue!!)
                weight = ""
                reps = ""
            }
        ) { Text("Añadir") }
    }
}
