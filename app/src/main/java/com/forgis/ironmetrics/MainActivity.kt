package com.forgis.ironmetrics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.forgis.ironmetrics.data.AppDatabase
import com.forgis.ironmetrics.data.BackupManager
import com.forgis.ironmetrics.data.ExerciseRepository
import com.forgis.ironmetrics.ui.ExerciseViewModel
import com.forgis.ironmetrics.ui.IronMetricsApp
import com.forgis.ironmetrics.ui.theme.IronmetricsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ExerciseViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        ExerciseViewModel.Factory(ExerciseRepository(db), BackupManager(db))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IronmetricsTheme {
                IronMetricsApp(viewModel)
            }
        }
    }
}
