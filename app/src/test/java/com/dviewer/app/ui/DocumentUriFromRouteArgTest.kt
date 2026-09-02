package com.dviewer.app.ui

import android.net.Uri
import org.junit.Assert.assertEquals
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
    fun alreadyDecodedSafUriIsNotDoubleDecoded() {
        // Navigation-Compose decodes the route's path segment before handing it to us, so by
        // the time this function sees it, a real SAF content Uri already contains literal `:`
        // and `/` characters (decoded from the `%3A`/`%2F` that were in the encoded route
        // segment). This function must pass such a value through unchanged rather than running
        // it through Uri.decode a second time, which would mangle those characters further.
        val alreadyDecoded = "content://com.android.externalstorage.documents/document/primary:Download/file.pdf"

        val result = documentUriFromRouteArg(alreadyDecoded)

        assertEquals(alreadyDecoded, result.toString())
        assertEquals("primary:Download/file.pdf", result.encodedPath?.substringAfter("/document/"))
    }
}
