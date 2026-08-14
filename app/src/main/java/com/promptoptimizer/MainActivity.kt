package com.promptoptimizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.promptoptimizer.ui.nav.NavGraph
import com.promptoptimizer.ui.theme.PromptOptimizerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PromptOptimizerTheme {
                val viewModel: com.promptoptimizer.ui.viewmodel.MainViewModel = viewModel()
                NavGraph(viewModel)
            }
        }
    }
}
