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
                    navController.navigate("${VIEWER_ROUTE.substringBefore("{")}${Uri.encode(uri.toString())}")
                },
            )
        }
        composable(VIEWER_ROUTE) { backStackEntry ->
            ViewerScreen(documentUri = documentUriFromRouteArg(backStackEntry.arguments?.getString("uri")))
        }
    }
}

/**
 * Converts the raw `uri` nav-graph argument into a [Uri].
 *
 * Navigation-Compose already percent-decodes path arguments before they reach
 * [android.os.Bundle.getString], so this must NOT apply an additional [Uri.decode] call -
 * doing so would double-decode any real percent-escaped `content://` Uri (e.g. turning the
 * `%3A`/`%2F` in an ExternalStorageProvider Uri into literal `:`/`/`, corrupting the document
 * id).
 */
fun documentUriFromRouteArg(arg: String?): Uri = Uri.parse(arg.orEmpty())
