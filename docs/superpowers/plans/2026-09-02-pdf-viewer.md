# Dviewer PDF Viewer (Phase 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a minimal Android app that lets a user pick a PDF via the system file picker and view it (paging, zoom, search, text selection) using Google's official `androidx.pdf` library.

**Architecture:** Single-Activity Jetpack Compose app. `MainActivity` (a `FragmentActivity`) hosts a two-route Compose nav graph: `home` (a picker button) and `viewer/{uri}` (embeds `PdfViewerFragment` via the `AndroidFragment` composable). No persistence, no ViewModel, no database — a picked Uri flows straight through nav arguments into the fragment.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose (BOM 2024.09.00), AGP 9.1.1, Gradle 9.3.1, `androidx.navigation:navigation-compose:2.8.9`, `androidx.fragment:fragment-compose:1.8.6`, `androidx.pdf:pdf-viewer-fragment:1.0.0-beta01`, Robolectric 4.16.1 for JVM-based Compose UI tests.

**Spec:** `docs/superpowers/specs/2026-09-02-pdf-viewer-design.md`

## Global Constraints

- **minSdk 30**, compileSdk/targetSdk **36** (installed SDK platform is `android-36.1`; use AGP 9.1's `compileSdk { version = release(36) { minorApiLevel = 1 } }` DSL).
- `applicationId` / `namespace`: `com.dviewer.app`.
- Dependency versions are pinned exactly as listed under Tech Stack above — these were chosen because AGP 9.1.1 / Kotlin 2.2.10 / composeBom 2024.09.00 / navigationCompose 2.8.9 / Robolectric 4.16.1 are already proven to build together in another Android project on this same machine (`~/Projects/Pinpoint`).
- **Real API surface, verified by inspecting the actual downloaded AARs (not just docs), because a doc fetch for `androidx.pdf` returned a garbled artifact coordinate and a stale SdkExtensions requirement:**
  - `androidx.pdf:pdf-viewer-fragment:1.0.0-beta01`'s own `AndroidManifest.xml` declares `minSdkVersion="28"` — no runtime `SdkExtensions` gating is exposed on the public API. `PdfViewerFragment` (`androidx.pdf.viewer.fragment.PdfViewerFragment`) extends `androidx.fragment.app.Fragment` and exposes a plain settable property `var documentUri: Uri`, plus a no-arg public constructor. No manifest entries or theme requirements beyond hosting it from a `FragmentActivity`.
  - `androidx.fragment:fragment-compose:1.8.6`'s `AndroidFragment` composable signature (confirmed via `javap`) is: `AndroidFragment<T : Fragment>(modifier: Modifier = Modifier, fragmentState: FragmentState = rememberFragmentState(), arguments: Bundle = Bundle.EMPTY, onUpdate: (T) -> Unit = {})`.
- **No JDK, Gradle, or adb on this machine's PATH.** Android Studio is installed as a flatpak (`com.google.AndroidStudio`) that bundles its own JBR at `/app/extra/jbr` and sets `JAVA_HOME` for it. Run every Gradle command as:
  ```bash
  flatpak run --command=bash com.google.AndroidStudio -c "cd <project-dir> && ./gradlew <task> --console=plain"
  ```
  The flatpak sandbox only has filesystem access under `$HOME` (`/home/v`) — **a project path under `/tmp` is invisible to it.** Keep the project under `/home/v/Projects/Dviewer`.
- **Known sandbox quirk:** piping flatpak-run Gradle output through `| tail` (or similar) can hang indefinitely even after the build has actually finished and written its output. Prefer redirecting to a log file, and confirm success by checking for the expected output artifact (e.g. `app/build/outputs/apk/debug/app-debug.apk`, or a test-results XML under `app/build/test-results/`) rather than waiting on the process to exit. A first Gradle invocation (cold daemon/dependency download) can take several minutes.
- `local.properties` (gitignored) must contain `sdk.dir=/home/v/Android/Sdk` for Gradle to find the Android SDK.
- No launcher icon asset is created — the manifest omits `android:icon`, using the system default. Icon design is out of scope for this phase.
- Robolectric tests pin `@Config(sdk = [30])` (matching minSdk) rather than the newest SDK, since Robolectric shadow support for very recent API levels can lag the platform release.

---

### Task 1: Gradle project scaffolding + minimal buildable app

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` (bootstrapped, not hand-written)
- Create: `local.properties` (gitignored)
- Create: `.gitignore`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/java/com/dviewer/app/ui/theme/Color.kt`
- Create: `app/src/main/java/com/dviewer/app/ui/theme/Type.kt`
- Create: `app/src/main/java/com/dviewer/app/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/dviewer/app/MainActivity.kt`

**Interfaces:**
- Produces: `com.dviewer.app.ui.theme.DviewerTheme` (a `@Composable fun DviewerTheme(content: @Composable () -> Unit)`), used by Task 4's `MainActivity` rewrite.

- [ ] **Step 1: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Dviewer"
include(":app")
```

- [ ] **Step 2: Create root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

- [ ] **Step 3: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 4: Create `gradle/libs.versions.toml`**

```toml
[versions]
agp = "9.1.1"
kotlin = "2.2.10"
coreKtx = "1.13.1"
activityCompose = "1.10.1"
lifecycleRuntimeKtx = "2.8.7"
composeBom = "2024.09.00"
navigationCompose = "2.8.9"
fragmentCompose = "1.8.6"
pdfViewerFragment = "1.0.0-beta01"
junit = "4.13.2"
robolectric = "4.16.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-navigation-testing = { group = "androidx.navigation", name = "navigation-testing", version.ref = "navigationCompose" }
androidx-fragment-compose = { group = "androidx.fragment", name = "fragment-compose", version.ref = "fragmentCompose" }
androidx-pdf-viewer-fragment = { group = "androidx.pdf", name = "pdf-viewer-fragment", version.ref = "pdfViewerFragment" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 5: Bootstrap the Gradle wrapper**

No standalone `gradle` or `java` binary exists on this machine's PATH, so `gradle wrapper` can't be run directly. Copy the wrapper from the sibling project already using the same Gradle version (fast, proven to work in this sandbox):

```bash
cp /home/v/Projects/Pinpoint/gradlew /home/v/Projects/Pinpoint/gradlew.bat /home/v/Projects/Dviewer/
mkdir -p /home/v/Projects/Dviewer/gradle/wrapper
cp /home/v/Projects/Pinpoint/gradle/wrapper/gradle-wrapper.jar /home/v/Projects/Dviewer/gradle/wrapper/
cp /home/v/Projects/Pinpoint/gradle/wrapper/gradle-wrapper.properties /home/v/Projects/Dviewer/gradle/wrapper/
chmod +x /home/v/Projects/Dviewer/gradlew
```

If `~/Projects/Pinpoint` no longer exists, fall back to generating it from the already-downloaded Gradle distribution:
```bash
/home/v/.gradle/wrapper/dists/gradle-9.3.1-bin/*/gradle-9.3.1/bin/gradle wrapper --gradle-version 9.3.1
```
(run via the same `flatpak run --command=bash com.google.AndroidStudio -c "..."` wrapper as all other Gradle invocations; this can take several minutes).

- [ ] **Step 6: Create `local.properties`**

```properties
sdk.dir=/home/v/Android/Sdk
```

- [ ] **Step 7: Create `.gitignore`**

```gitignore
*.iml
.gradle/
/local.properties
.idea/
.DS_Store
/build
app/build
/captures
.externalNativeBuild
.cxx
*.apk
```

- [ ] **Step 8: Create `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dviewer.app"
    compileSdk { version = release(36) { minorApiLevel = 1 } }

    defaultConfig {
        applicationId = "com.dviewer.app"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
```

- [ ] **Step 9: Create `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Material.Light.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 10: Create `app/src/main/res/values/strings.xml`**

```xml
<resources>
    <string name="app_name">Dviewer</string>
</resources>
```

- [ ] **Step 11: Create `app/src/main/java/com/dviewer/app/ui/theme/Color.kt`**

```kotlin
package com.dviewer.app.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
```

- [ ] **Step 12: Create `app/src/main/java/com/dviewer/app/ui/theme/Type.kt`**

```kotlin
package com.dviewer.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)
```

- [ ] **Step 13: Create `app/src/main/java/com/dviewer/app/ui/theme/Theme.kt`**

```kotlin
package com.dviewer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

@Composable
fun DviewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
```

- [ ] **Step 14: Create `app/src/main/java/com/dviewer/app/MainActivity.kt`**

```kotlin
package com.dviewer.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.dviewer.app.ui.theme.DviewerTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DviewerTheme {
                Surface(modifier = Modifier) {
                    Text("Dviewer")
                }
            }
        }
    }
}
```

- [ ] **Step 15: Build and verify**

```bash
flatpak run --command=bash com.google.AndroidStudio -c "cd /home/v/Projects/Dviewer && ./gradlew assembleDebug --console=plain > /tmp/dviewer-build.log 2>&1"
```
Then check: `ls -la /home/v/Projects/Dviewer/app/build/outputs/apk/debug/app-debug.apk` exists, and `grep -i "BUILD SUCCESSFUL" /tmp/dviewer-build.log`. If the wrapping shell command doesn't return promptly, check these two things directly instead of waiting on the process (see Global Constraints sandbox quirk).

- [ ] **Step 16: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/ gradlew gradlew.bat .gitignore app/
git commit -m "Scaffold Android project with minimal Compose app"
```

---

### Task 2: HomeScreen composable

**Files:**
- Create: `app/src/main/java/com/dviewer/app/ui/home/HomeScreen.kt`
- Test: `app/src/test/java/com/dviewer/app/ui/home/HomeScreenTest.kt`

**Interfaces:**
- Consumes: nothing beyond Compose/Material3.
- Produces: `@Composable fun HomeScreen(errorMessage: String?, onErrorDismissed: () -> Unit, onOpenPdfClick: () -> Unit)`, used by Task 3's `HomeRoute`.

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
flatpak run --command=bash com.google.AndroidStudio -c "cd /home/v/Projects/Dviewer && ./gradlew testDebugUnitTest --tests com.dviewer.app.ui.home.HomeScreenTest --console=plain > /tmp/dviewer-test.log 2>&1"
```
Expected: FAIL — `HomeScreen` is unresolved (doesn't exist yet). Confirm via `grep -i "unresolved reference\|FAILED" /tmp/dviewer-test.log`.

- [ ] **Step 3: Write `HomeScreen.kt`**

```kotlin
package com.dviewer.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen(
    errorMessage: String?,
    onErrorDismissed: () -> Unit,
    onOpenPdfClick: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorDismissed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Button(onClick = onOpenPdfClick) {
                Text("Open PDF")
            }
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
flatpak run --command=bash com.google.AndroidStudio -c "cd /home/v/Projects/Dviewer && ./gradlew testDebugUnitTest --tests com.dviewer.app.ui.home.HomeScreenTest --console=plain > /tmp/dviewer-test.log 2>&1"
```
Expected: PASS. Confirm via `grep -i "BUILD SUCCESSFUL" /tmp/dviewer-test.log`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dviewer/app/ui/home/HomeScreen.kt app/src/test/java/com/dviewer/app/ui/home/HomeScreenTest.kt
git commit -m "Add HomeScreen composable with Open PDF button"
```

---

### Task 3: Pick-result logic and HomeRoute wiring

**Files:**
- Create: `app/src/main/java/com/dviewer/app/ui/home/PickResult.kt`
- Test: `app/src/test/java/com/dviewer/app/ui/home/PickResultTest.kt`
- Create: `app/src/main/java/com/dviewer/app/ui/home/HomeRoute.kt`

**Interfaces:**
- Consumes: `HomeScreen(errorMessage, onErrorDismissed, onOpenPdfClick)` from Task 2.
- Produces: `sealed interface PickResult<out T>` with `Ready<T>(value: T)`, `Error(message: String)`, `Cancelled`; `fun <T> pickResultFor(picked: T?, isReadable: (T) -> Boolean): PickResult<T>`; `@Composable fun HomeRoute(onDocumentReady: (Uri) -> Unit)`, used by Task 4's `DviewerNavHost`.

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
flatpak run --command=bash com.google.AndroidStudio -c "cd /home/v/Projects/Dviewer && ./gradlew testDebugUnitTest --tests com.dviewer.app.ui.home.PickResultTest --console=plain > /tmp/dviewer-test.log 2>&1"
```
Expected: FAIL — `PickResult`/`pickResultFor` unresolved.

- [ ] **Step 3: Write `PickResult.kt`**

```kotlin
package com.dviewer.app.ui.home

sealed interface PickResult<out T> {
    data class Ready<T>(val value: T) : PickResult<T>
    data class Error(val message: String) : PickResult<Nothing>
    data object Cancelled : PickResult<Nothing>
}

fun <T> pickResultFor(picked: T?, isReadable: (T) -> Boolean): PickResult<T> = when {
    picked == null -> PickResult.Cancelled
    isReadable(picked) -> PickResult.Ready(picked)
    else -> PickResult.Error("Couldn't open that file.")
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
flatpak run --command=bash com.google.AndroidStudio -c "cd /home/v/Projects/Dviewer && ./gradlew testDebugUnitTest --tests com.dviewer.app.ui.home.PickResultTest --console=plain > /tmp/dviewer-test.log 2>&1"
```
Expected: PASS.

- [ ] **Step 5: Write `HomeRoute.kt`** (thin Android/Compose wiring; covered by the manual smoke test in Task 5, not a unit test — see spec's Testing section)

```kotlin
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
```

- [ ] **Step 6: Verify the project still compiles**

```bash
flatpak run --command=bash com.google.AndroidStudio -c "cd /home/v/Projects/Dviewer && ./gradlew assembleDebug --console=plain > /tmp/dviewer-build.log 2>&1"
```
Expected: `grep -i "BUILD SUCCESSFUL" /tmp/dviewer-build.log`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/dviewer/app/ui/home/PickResult.kt app/src/test/java/com/dviewer/app/ui/home/PickResultTest.kt app/src/main/java/com/dviewer/app/ui/home/HomeRoute.kt
git commit -m "Add pick-result logic and HomeRoute wiring for the SAF picker"
```

---

### Task 4: Navigation graph (Home <-> Viewer)

**Files:**
- Modify: `gradle/libs.versions.toml` (add navigation-compose/testing entries — already present from Task 1, so this task only wires them into `app/build.gradle.kts`)
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/dviewer/app/ui/viewer/ViewerScreen.kt` (placeholder; Task 5 replaces the body)
- Create: `app/src/main/java/com/dviewer/app/ui/DviewerNavHost.kt`
- Modify: `app/src/main/java/com/dviewer/app/MainActivity.kt`
- Test: `app/src/test/java/com/dviewer/app/ui/DviewerNavHostTest.kt`

**Interfaces:**
- Consumes: `HomeRoute(onDocumentReady)` from Task 3; `DviewerTheme` from Task 1.
- Produces: `@Composable fun DviewerNavHost(navController: NavHostController = rememberNavController())`; `@Composable fun ViewerScreen(documentUri: Uri)`, replaced (same signature) by Task 5.

- [ ] **Step 1: Add navigation dependencies to `app/build.gradle.kts`**

Add inside the `dependencies { ... }` block (after `implementation(libs.androidx.material3)`):

```kotlin
    implementation(libs.androidx.navigation.compose)
```

Add inside the test dependencies (after `testImplementation(libs.androidx.ui.test.junit4)`):

```kotlin
    testImplementation(libs.androidx.navigation.testing)
```

- [ ] **Step 2: Create placeholder `ViewerScreen.kt`**

```kotlin
package com.dviewer.app.ui.viewer

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ViewerScreen(documentUri: Uri) {
    Text("Viewer: $documentUri")
}
```

- [ ] **Step 3: Write the failing test**

```kotlin
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
```

- [ ] **Step 4: Run the test to verify it fails**

```bash
flatpak run --command=bash com.google.AndroidStudio -c "cd /home/v/Projects/Dviewer && ./gradlew testDebugUnitTest --tests com.dviewer.app.ui.DviewerNavHostTest --console=plain > /tmp/dviewer-test.log 2>&1"
```
Expected: FAIL — `DviewerNavHost` unresolved.

- [ ] **Step 5: Create `DviewerNavHost.kt`**

```kotlin
package com.dviewer.app.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dviewer.app.ui.home.HomeRoute
import com.dviewer.app.ui.viewer.ViewerScreen

const val HOME_ROUTE = "home"
const val VIEWER_ROUTE = "viewer/{uri}"

@Composable
fun DviewerNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            HomeRoute(
                onDocumentReady = { uri ->
                    navController.navigate("viewer/${Uri.encode(uri.toString())}")
                },
            )
        }
        composable(VIEWER_ROUTE) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("uri").orEmpty()
            ViewerScreen(documentUri = Uri.parse(Uri.decode(encodedUri)))
        }
    }
}
```

- [ ] **Step 6: Rewrite `MainActivity.kt`**

```kotlin
package com.dviewer.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.dviewer.app.ui.DviewerNavHost
import com.dviewer.app.ui.theme.DviewerTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DviewerTheme {
                DviewerNavHost()
            }
        }
    }
}
```

- [ ] **Step 7: Run the test to verify it passes**

```bash
flatpak run --command=bash com.google.AndroidStudio -c "cd /home/v/Projects/Dviewer && ./gradlew testDebugUnitTest --tests com.dviewer.app.ui.DviewerNavHostTest --console=plain > /tmp/dviewer-test.log 2>&1"
```
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/dviewer/app/ui/viewer/ViewerScreen.kt app/src/main/java/com/dviewer/app/ui/DviewerNavHost.kt app/src/main/java/com/dviewer/app/MainActivity.kt app/src/test/java/com/dviewer/app/ui/DviewerNavHostTest.kt
git commit -m "Wire Home and Viewer routes into a navigation graph"
```

---

### Task 5: Real PDF rendering via androidx.pdf

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/dviewer/app/ui/viewer/ViewerScreen.kt` (replace placeholder body)

**Interfaces:**
- Consumes: `ViewerScreen(documentUri: Uri)` call site from Task 4's `DviewerNavHost` (signature unchanged, so no other file needs to change).

- [ ] **Step 1: Add dependencies to `app/build.gradle.kts`**

Add inside `dependencies { ... }` (after `implementation(libs.androidx.navigation.compose)`):

```kotlin
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.pdf.viewer.fragment)
```

- [ ] **Step 2: Replace `ViewerScreen.kt`'s body with the real fragment embed**

```kotlin
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
```

- [ ] **Step 3: Build and verify**

```bash
flatpak run --command=bash com.google.AndroidStudio -c "cd /home/v/Projects/Dviewer && ./gradlew assembleDebug --console=plain > /tmp/dviewer-build.log 2>&1"
```
Expected: `grep -i "BUILD SUCCESSFUL" /tmp/dviewer-build.log`; `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 4: Manual smoke test** (per spec — `PdfViewerFragment`'s rendering is Google's library, not ours to unit test)

Install the debug APK on a device or emulator running API 30+:
```bash
flatpak run --command=bash com.google.AndroidStudio -c "$ANDROID_HOME/platform-tools/adb install -r /home/v/Projects/Dviewer/app/build/outputs/apk/debug/app-debug.apk"
```
(or install via Android Studio's Run button). Then, on-device: launch Dviewer, tap "Open PDF", pick a real PDF file, and confirm:
- The document renders and pages.
- Pinch-to-zoom and scroll work.
- The in-document search (toolbox) can find text.

If no device/emulator is available in this environment, this step is performed by whoever has access to one — do not skip it silently; flag it as pending in the task result.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/dviewer/app/ui/viewer/ViewerScreen.kt
git commit -m "Render picked PDFs with androidx.pdf's PdfViewerFragment"
```
