package com.example.booktrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.booktrack.navigation.NavGraph
import com.example.booktrack.ui.BookViewModel
import com.example.booktrack.ui.theme.BookTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BookTrackTheme {
                val navController = rememberNavController()
                val bookViewModel: BookViewModel = viewModel()
                NavGraph(navController = navController, viewModel = bookViewModel)
            }
        }
    }
}
