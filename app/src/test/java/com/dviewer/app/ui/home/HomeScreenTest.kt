package com.dviewer.app.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tappingOpenPdfInvokesCallback() {
        var clicked = false
        composeTestRule.setContent {
            HomeScreen(
                errorMessage = null,
                onErrorDismissed = {},
                onOpenPdfClick = { clicked = true },
            )
        }

        composeTestRule.onNodeWithText("Open PDF").performClick()

        assertTrue(clicked)
    }

    @Test
    fun errorMessageIsShown() {
        composeTestRule.setContent {
            HomeScreen(
                errorMessage = "Couldn't open that file.",
                onErrorDismissed = {},
                onOpenPdfClick = {},
            )
        }

        composeTestRule.onNodeWithText("Couldn't open that file.").assertExists()
    }
}
