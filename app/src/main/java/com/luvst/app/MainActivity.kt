package com.luvst.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.luvst.app.ui.screens.home.HomeScreen
import com.luvst.app.ui.screens.inbox.InboxScreen
import com.luvst.app.ui.screens.login.LoginScreen
import com.luvst.app.ui.screens.luvst.LuvstScreen
import com.luvst.app.ui.screens.splash.SplashScreen
import com.luvst.app.ui.theme.LuvstTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuvstTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onNavigateToLogin = { navController.navigate("login") { popUpTo("splash") { inclusive = true } } },
                                onNavigateToHome = { navController.navigate("home") { popUpTo("splash") { inclusive = true } } }
                            )
                        }
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { navController.navigate("home") { popUpTo("login") { inclusive = true } } }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                onNavigateToLuvst = { navController.navigate("luvst") },
                                onNavigateToInbox = { navController.navigate("inbox") }
                            )
                        }
                        composable("luvst") {
                            LuvstScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("inbox") {
                            InboxScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
