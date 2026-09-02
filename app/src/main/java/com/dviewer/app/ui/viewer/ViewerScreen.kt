package com.dviewer.app.ui.viewer

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import androidx.pdf.viewer.fragment.PdfViewerFragment

@Composable
fun ViewerScreen(documentUri: Uri) {
    AndroidFragment<PdfViewerFragment>(modifier = Modifier.fillMaxSize()) { fragment ->
        fragment.documentUri = documentUri
    }
}
