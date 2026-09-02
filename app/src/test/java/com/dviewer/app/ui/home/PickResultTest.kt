package com.dviewer.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class PickResultTest {
    @Test
    fun nullPickedValueIsCancelled() {
        val result = pickResultFor<String>(null) { true }
        assertEquals(PickResult.Cancelled, result)
    }

    @Test
    fun readableValueIsReady() {
        val result = pickResultFor("doc.pdf") { true }
        assertEquals(PickResult.Ready("doc.pdf"), result)
    }

    @Test
    fun unreadableValueIsError() {
        val result = pickResultFor("doc.pdf") { false }
        assertEquals(PickResult.Error("Couldn't open that file."), result)
    }
}
