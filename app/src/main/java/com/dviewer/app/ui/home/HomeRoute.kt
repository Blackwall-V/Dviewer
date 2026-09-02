package com.dviewer.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun HomeRoute(onDocumentReady: (Uri) -> Unit) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // No readability pre-check here: PdfViewerFragment surfaces its own error UI for
    // unopenable/malformed documents, and a synchronous contentResolver.openFileDescriptor
    // call here could block the main thread for seconds against a cloud DocumentsProvider.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        when (val result = pickResultFor(uri) { true }) {
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
