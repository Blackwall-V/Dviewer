package com.dviewer.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

    /**
     * Verifies the viewer route is correctly wired into the nav graph (registered, with a `uri`
     * argument) WITHOUT actually navigating into it.
     *
     * We deliberately don't drive `navController.navigate(...)` to the viewer route here: doing
     * so composes [com.dviewer.app.ui.viewer.ViewerScreen], which embeds a real
     * `PdfViewerFragment` via `AndroidFragment`. That was tried during this fix pass and it
     * surfaces a genuine Robolectric/PdfViewerFragment incompatibility (an `InflateException` /
     * `UnsupportedOperationException` while inflating the fragment's own layout) regardless of
     * which FragmentActivity subclass or theme hosts it - this is exactly the kind of
     * platform-behavior integration the project's compose-ui-testing-patterns skill says to keep
     * out of Robolectric tests, since it can only be genuinely verified on a device/emulator
     * (see the C3 fix and its report for the runtime theme fix, which is unverified on-device in
     * this environment). We keep this test at the structural, nav-graph level instead, and rely
     * on [DocumentUriFromRouteArgTest] to cover the pure argument-decoding logic (C2).
     */
    @Test
    fun viewerRouteIsRegisteredWithAUriArgument() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(composeTestRule.activity).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            DviewerNavHost(navController = navController)
        }

        val viewerDestination = navController.graph.findNode(VIEWER_ROUTE)

        assertNotNull("expected the viewer route to be registered in the nav graph", viewerDestination)
        assertTrue(
            "expected the viewer route's pattern to carry a 'uri' path argument",
            viewerDestination!!.route.orEmpty().contains("{uri}"),
        )
        composeTestRule.onNodeWithText("Open PDF").assertExists()
    }
}
