# Dviewer

An Android document viewer. Phase 1 is a minimal PDF viewer: pick a PDF via
the system file picker and view it — paging, pinch-zoom, in-document
search, and text selection, all provided by Google's official
[`androidx.pdf`](https://developer.android.com/jetpack/androidx/releases/pdf)
library. Office formats (docx/xlsx/pptx) are a planned future phase, not
yet started.

## Status

Phase 1 (PDF viewing) is implemented and unit-tested, but **has not been
verified on a real device or emulator** — no device was available in the
environment this was built in. Before relying on it, install a debug build
and confirm a real PDF actually opens, pages, zooms, and searches correctly.

## Stack

- Kotlin 2.2.10, Jetpack Compose (BOM 2024.09.00), single-Activity app with
  a two-route Compose navigation graph (`home` / `viewer/{uri}`).
- PDF rendering: `androidx.pdf:pdf-viewer-fragment:1.0.0-beta01`
  (`PdfViewerFragment`), embedded into Compose via the `AndroidFragment`
  composable from `androidx.fragment:fragment-compose`.
- File input: Storage Access Framework (`ACTION_OPEN_DOCUMENT`) only — no
  storage permissions, no recents list, no "Open with" registration.
- `minSdk 30` (Android 11+), `compileSdk`/`targetSdk 36`.

## Building

Requires the Android SDK and a JDK. Create `local.properties` at the repo
root pointing at your SDK:

```properties
sdk.dir=/path/to/Android/Sdk
```

Then:

```bash
./gradlew assembleDebug   # build
./gradlew testDebugUnitTest   # run unit tests
```

## Known constraints

- **Theme requirement:** `PdfViewerFragment`'s own layouts use Material3
  theme attributes internally. The app's manifest theme
  (`Theme.Dviewer`, in `app/src/main/res/values/themes.xml`) must stay
  parented to a Material3 theme (currently `Theme.Material3.DayNight.NoActionBar`)
  and `MainActivity` must stay an `AppCompatActivity`/`FragmentActivity` —
  reverting to a plain platform theme will crash when the viewer screen
  inflates the fragment.
- **`androidx.pdf` is still in beta** (`1.0.0-beta01`). Its API is
  reasonably stable at this point but not final — re-check the release
  notes before bumping the version, and re-verify on-device after any bump.
- The picked document's read permission is not persisted, so re-opening the
  app after process death shows the fragment's own "can't open" error
  rather than the last-viewed PDF. This is intentional for this phase (no
  recents/history feature) — see the design spec.

## Docs

- [`docs/superpowers/specs/2026-09-02-pdf-viewer-design.md`](docs/superpowers/specs/2026-09-02-pdf-viewer-design.md) — the design spec Phase 1 was built from.
- [`docs/superpowers/plans/2026-09-02-pdf-viewer.md`](docs/superpowers/plans/2026-09-02-pdf-viewer.md) — the implementation plan, task by task.

## License

[MIT](LICENSE)
