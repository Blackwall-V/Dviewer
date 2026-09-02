package com.dviewer.app.ui

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DocumentUriFromRouteArgTest {
    @Test
    fun blankArgumentBecomesEmptyUri() {
        assertEquals(Uri.parse(""), documentUriFromRouteArg(null))
    }

    @Test
    fun percentEscapedDocIdIsNotDoubleDecoded() {
        // A real SAF content Uri's docId is itself percent-escaped (DocumentsContract encodes
        // the docId's own `:`/`/` before it ever becomes part of the Uri). Navigation-Compose
        // decodes the route's path segment exactly once, so by the time this function sees the
        // argument it already contains literal `%3A`/`%2F`. This function must pass that value
        // through unchanged rather than running it through Uri.decode a second time, which would
        // mangle those escapes into literal `:`/`/` and corrupt the document id. A fixture with
        // no `%` characters can't catch this: Uri.decode is a no-op on such a string either way.
        val onceDecoded =
            "content://com.android.externalstorage.documents/document/primary%3ADownload%2Ffile.pdf"

        val result = documentUriFromRouteArg(onceDecoded)

        assertEquals(onceDecoded, result.toString())
        assertTrue(result.toString().contains("%3A"))
        assertTrue(result.toString().contains("%2F"))
    }
}
