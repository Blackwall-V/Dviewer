# Dviewer — Phase 1: PDF Viewer (Android)

## Status
Approved for implementation planning.

## Context
Dviewer is a new Android document viewer app. The long-term goal is to view
PDF and Office documents (docx/xlsx/pptx), but PDF and Office formats need
fundamentally different rendering approaches on Android (native PDF support
vs. no native Office support at all). This spec covers **Phase 1 only**: a
complete, shippable PDF viewer. Office format support is deferred to
follow-on specs, to be designed once this phase is working.

## Goals
- Open and view a PDF file picked by the user via the system file picker.
- Paging, pinch-zoom/scroll, in-document search, and text selection.
- No unnecessary scope: no recents list, no "Open with" intent-filter
  registration, no persistent library/database. Single-shot: pick a file,
  view it.

## Non-goals (this phase)
- Office document formats (docx/xlsx/pptx) — future phases.
- Registering as a PDF handler for other apps ("Open with Dviewer").
- Any document storage, history, bookmarks, or annotations persistence
  beyond what `androidx.pdf` provides in-session.
- Support for Android versions below API 30.

## Decisions

### PDF rendering: `androidx.pdf` (Jetpack PDF viewer library)
Use Google's official, actively-maintained `PdfViewerFragment`
(`androidx.pdf`), backed by framework `PdfRenderer` /
`PdfRendererPreV`. It provides paging, zoom/scroll, search, text
selection, and stylus annotation support out of the box — this avoids
writing and maintaining custom PDF rendering/gesture code.

**Trade-off accepted:** full feature support requires **minSdk 30**
(Android 11+), since `PdfRendererPreV` backports Android V PDF APIs only to
API 30–34, and native support starts at API 35. This excludes devices on
Android 10 and below. Confirmed acceptable — Android 11+ covers the large
majority of active devices, and the alternative (a third-party
pdfium-based library) would trade away built-in search and annotation
support to reach API 21.

### File input: Storage Access Framework only
Use `ACTION_OPEN_DOCUMENT` via `registerForActivityResult`. No storage
permissions are requested. The returned `content://` Uri is used directly
(read-only, single-session); no persistable Uri permission is taken since
there is no recents feature to support re-opening later.

### UI: single-Activity Jetpack Compose app
- `MainActivity` (must be `FragmentActivity`/`AppCompatActivity` to host a
  Fragment) sets Compose content with a small nav graph (`androidx
  .navigation:navigation-compose`): `home` and `viewer/{uri}` routes.
- **Home screen**: one "Open PDF" button. On pick, navigates to the viewer
  route with the encoded Uri as an argument.
- **Viewer screen**: embeds `PdfViewerFragment` via the `AndroidFragment`
  composable (`androidx.fragment:fragment-compose`), passing the Uri as a
  fragment argument. All rendering, paging, zoom, and search UI is
  provided by the fragment itself.

### Error handling
- Picker cancelled → no-op, stay on Home.
- Uri open failure (`SecurityException`, `FileNotFoundException`) →
  caught on Home screen, shown as a `Snackbar`, user stays on Home.
- Malformed/password-protected PDFs → handled by `PdfViewerFragment`'s
  own built-in error and password-prompt UI; Dviewer does not duplicate
  this.

## Architecture

```
app/
  src/main/java/<pkg>/
    MainActivity.kt              # FragmentActivity, sets Compose content + NavHost
    ui/
      DviewerNavHost.kt          # NavHost: "home", "viewer/{uri}"
      HomeScreen.kt              # Open-PDF button, SAF launcher, error Snackbar
      ViewerScreen.kt            # AndroidFragment<PdfViewerFragment> wrapper
  AndroidManifest.xml
build.gradle.kts (app + project)
settings.gradle.kts
gradle/libs.versions.toml
```

Default package: `com.dviewer.app` (easy to change later; app name
"Dviewer" matches the repo name).

## Data flow
1. User taps "Open PDF" on Home.
2. `ACTION_OPEN_DOCUMENT` picker launched; user selects a PDF.
3. Result Uri received in the activity-result callback.
4. On success, navigate to `viewer/{uri}` with the Uri passed as a nav
   argument.
5. `ViewerScreen` reads the Uri argument and embeds `PdfViewerFragment`,
   which loads and renders the document.
6. Back navigation returns to Home; the Uri/fragment state is discarded
   (no persistence).

## Testing
- **Compose UI test**: Home screen renders the "Open PDF" button; tapping
  it triggers the picker launcher (verified via a fake `ActivityResultRegistry`).
- **Manual smoke test**: pick a real PDF on a device/emulator running API
  30+; verify page swipe, pinch-zoom, and in-document search work.
- No unit tests planned for `PdfViewerFragment` itself — it is Google's
  library and out of scope to test directly; our code has no business
  logic beyond picker-result handling and navigation.

## Tooling
Installed the `chrisbanes-skills` plugin (Kotlin/Jetpack Compose skill
pack by Chris Banes) for implementation guidance on Compose state
handling, performance, and UI testing patterns during this and future
phases.

## Future phases (not designed yet)
- Phase 2+: docx/xlsx/pptx support via an offline WebView embedding
  bundled open-source JS renderers (docx-preview, SheetJS), fully local
  with no file leaving the device. To be brainstormed and spec'd
  separately once Phase 1 ships.
