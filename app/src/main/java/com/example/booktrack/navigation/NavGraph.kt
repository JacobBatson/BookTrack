package com.example.booktrack.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.booktrack.ui.BookViewModel
import com.example.booktrack.ui.screens.CalendarScreen
import com.example.booktrack.ui.screens.DetailScreen
import com.example.booktrack.ui.screens.LibraryScreen
import com.example.booktrack.ui.screens.ReadingSessionScreen
import com.example.booktrack.ui.screens.SearchScreen
import com.example.booktrack.ui.screens.SessionSummaryScreen

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Search : Screen("search")
    object Calendar : Screen("calendar")
    object Detail : Screen("detail")
    object ReadingSession : Screen("reading_session")
    object SessionSummary : Screen("session_summary")
}

private val bottomNavRoutes = setOf(
    Screen.Library.route,
    Screen.Search.route,
    Screen.Calendar.route
)

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: BookViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Books") },
                        selected = currentRoute == Screen.Library.route,
                        onClick = {
                            navController.navigate(Screen.Library.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Search") },
                        selected = currentRoute == Screen.Search.route,
                        onClick = {
                            navController.navigate(Screen.Search.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        label = { Text("Calendar") },
                        selected = currentRoute == Screen.Calendar.route,
                        onClick = {
                            navController.navigate(Screen.Calendar.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(
                    viewModel = viewModel,
                    onBookClick = { book ->
                        viewModel.selectBook(book)
                        navController.navigate(Screen.Detail.route)
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onBookClick = { book ->
                        viewModel.selectBook(book)
                        navController.navigate(Screen.Detail.route)
                    }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = viewModel)
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
}
