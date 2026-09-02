package com.dviewer.app.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dviewer.app.ui.home.HomeRoute
import com.dviewer.app.ui.viewer.ViewerScreen

const val HOME_ROUTE = "home"
const val VIEWER_ROUTE = "viewer/{uri}"

@Composable
fun DviewerNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            HomeRoute(
                onDocumentReady = { uri ->
                    navController.navigate("viewer/${Uri.encode(uri.toString())}")
                },
            )
        }
        composable(VIEWER_ROUTE) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("uri").orEmpty()
            ViewerScreen(documentUri = Uri.parse(Uri.decode(encodedUri)))
        }
    }
}
