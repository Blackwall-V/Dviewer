package com.dviewer.app.ui.viewer

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ViewerScreen(documentUri: Uri) {
    Text("Viewer: $documentUri")
}
