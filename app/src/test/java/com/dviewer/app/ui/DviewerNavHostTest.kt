package com.dviewer.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DviewerNavHostTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun startDestinationShowsOpenPdfButton() {
        composeTestRule.setContent {
            val navController = TestNavHostController(composeTestRule.activity).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            DviewerNavHost(navController = navController)
        }

        composeTestRule.onNodeWithText("Open PDF").assertExists()
    }

    @Test
    fun navigatingToViewerRouteDecodesTheUriArgument() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(composeTestRule.activity).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            DviewerNavHost(navController = navController)
        }

        composeTestRule.runOnUiThread {
            navController.navigate("viewer/${android.net.Uri.encode("content://example/doc.pdf")}")
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Viewer: content://example/doc.pdf").assertExists()
    }
}
