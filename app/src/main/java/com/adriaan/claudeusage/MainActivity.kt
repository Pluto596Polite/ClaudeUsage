package com.adriaan.claudeusage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.adriaan.claudeusage.data.repository.UsageRepository
import com.adriaan.claudeusage.ui.screen.DashboardScreen
import com.adriaan.claudeusage.ui.screen.LoginScreen
import com.adriaan.claudeusage.ui.theme.ClaudeUsageTheme
import com.adriaan.claudeusage.viewmodel.MainViewModel
import com.adriaan.claudeusage.viewmodel.NavEvent

class MainActivity : ComponentActivity() {

    private val app get() = application as ClaudeUsageApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClaudeUsageTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(repository = app.repository)
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(repository: UsageRepository) {
    val navController = rememberNavController()
    val isLoggedIn by repository.isLoggedIn.collectAsState(initial = false)

    val vm: MainViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(repository) as T
        }
    )

    // Handle navigation events emitted by ViewModel
    LaunchedEffect(vm) {
        vm.navEvents.collect { event ->
            when (event) {
                is NavEvent.NavigateToDashboard -> navController.navigate("dashboard") {
                    popUpTo("login") { inclusive = true }
                }
                is NavEvent.NavigateToLogin -> navController.navigate("login") {
                    popUpTo("dashboard") { inclusive = true }
                }
            }
        }
    }

    // Auto-route on cold start based on stored session
    LaunchedEffect(isLoggedIn) {
        val current = navController.currentDestination?.route
        if (isLoggedIn && current == "login") {
            navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
        } else if (!isLoggedIn && current == "dashboard") {
            navController.navigate("login") { popUpTo("dashboard") { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = { cookies -> vm.onLoginSuccess(cookies) })
        }
        composable("dashboard") {
            DashboardScreen(viewModel = vm, onLogout = { vm.logout() })
        }
    }
}
