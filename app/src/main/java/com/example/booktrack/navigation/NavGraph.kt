package com.example.booktrack.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.booktrack.ui.BookViewModel
import com.example.booktrack.ui.screens.DetailScreen
import com.example.booktrack.ui.screens.LibraryScreen
import com.example.booktrack.ui.screens.ReadingSessionScreen
import com.example.booktrack.ui.screens.SearchScreen
import com.example.booktrack.ui.screens.SessionSummaryScreen

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Search : Screen("search")
    object Detail : Screen("detail")
    object ReadingSession : Screen("reading_session")
    object SessionSummary : Screen("session_summary")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: BookViewModel
) {
    NavHost(navController = navController, startDestination = Screen.Library.route) {
        composable(Screen.Library.route) {
            LibraryScreen(
                viewModel = viewModel,
                onBookClick = { book ->
                    viewModel.selectBook(book)
                    navController.navigate(Screen.Detail.route)
                },
                onSearchClick = { navController.navigate(Screen.Search.route) }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                viewModel = viewModel,
                onBookClick = { book ->
                    viewModel.selectBook(book)
                    navController.navigate(Screen.Detail.route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Detail.route) {
            DetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStartSession = { navController.navigate(Screen.ReadingSession.route) }
            )
        }
        composable(Screen.ReadingSession.route) {
            ReadingSessionScreen(
                viewModel = viewModel,
                onSessionEnded = {
                    navController.navigate(Screen.SessionSummary.route) {
                        popUpTo(Screen.ReadingSession.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.SessionSummary.route) {
            SessionSummaryScreen(
                viewModel = viewModel,
                onDone = {
                    navController.navigate(Screen.Detail.route) {
                        popUpTo(Screen.SessionSummary.route) { inclusive = true }
                    }
                }
            )
        }
    }
}