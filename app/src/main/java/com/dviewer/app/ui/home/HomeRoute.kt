package com.dviewer.app.ui.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.FileNotFoundException

@Composable
fun HomeRoute(onDocumentReady: (Uri) -> Unit) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        when (val result = pickResultFor(uri) { picked -> context.canOpenPdf(picked) }) {
            is PickResult.Ready -> {
                errorMessage = null
                onDocumentReady(result.value)
            }
            is PickResult.Error -> errorMessage = result.message
            PickResult.Cancelled -> Unit
        }
    }

    HomeScreen(
        errorMessage = errorMessage,
        onErrorDismissed = { errorMessage = null },
        onOpenPdfClick = { launcher.launch(arrayOf("application/pdf")) },
    )
}

private fun Context.canOpenPdf(uri: Uri): Boolean = try {
    contentResolver.openFileDescriptor(uri, "r")?.use { }
    true
} catch (e: SecurityException) {
    false
} catch (e: FileNotFoundException) {
    false
}
