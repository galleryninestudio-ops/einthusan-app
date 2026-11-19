# Einthusan for Android

A native Android client for browsing, saving, streaming, and downloading films
from Einthusan. The interface uses Kotlin and Jetpack Compose. Video playback
uses Media3 rather than a WebView.

## What the app does

- Browse Tamil, Hindi, Telugu, Malayalam, Kannada, Bengali, Marathi, and Punjabi films.
- Filter by recent releases, staff picks, regional hits, alphabet, and decade.
- Search within the last selected language.
- Open film details, save films to a watchlist, and continue from a saved position.
- Stream in a landscape player with seek, brightness and volume gestures,
  screen lock, picture fit, playback speed, and picture-in-picture. Pinch with
  two fingers to zoom from 1× to 4×, and move both fingers to pan while zoomed.
  Pinch back to 1× or select a picture-fit mode to return to the normal size.
- Sign in with an existing Einthusan account. Premium accounts can choose
  Standard or Premium HD in the same player settings panel as picture fit.
- Download films through a foreground service, pause or resume transfers, and
  play completed files without fetching another streaming link.

The home header opens Search, Library, and Account. The system back gesture or
button returns to the previous screen; there is no bottom navigation bar.

## Video quality

The website's `ultrahd` catalogue flag means that a higher-quality option is
listed. It does **not** verify a 4K stream. Cards show **HD**, and the player
reports the actual decoded dimensions separately from the selected stream mode.

Sultan's Premium stream was verified on-device at **1920 × 1080**. Einthusan's
[Premium page](https://einthusan.tv/buy/?lang=hindi) describes its offering as
“UltraHD Movies (1080p)”. A 4K label in the player requires decoded dimensions
at least 3840 pixels wide or 2160 pixels high; no title is labelled 4K merely
because of its catalogue flag.

Premium requests use `/premium/movie/watch/` with `html5=true&uhd=true`. The
HTML5 flag prevents the site's mobile redirect from dropping the quality
selection. The app checks both configured CDN edges for the requested Premium
MP4, since the same file can exist on one edge and return 404 on another.
An unavailable Premium stream leaves the current stream playing and does not
change the account's Premium status. Switching quality preserves position and
whether playback was paused.

## Code structure

```text
app/src/main/java/com/bench/einthusan/
  MainActivity.kt             Activity lifecycle, image loading, and deep links
  data/
    Repo.kt                   Catalogue requests and expiring stream URL cache
    Library.kt                Watchlist, resume points, and continue watching
    MovieJson.kt              Shared, backwards-compatible movie serialization
    Settings.kt               Player preferences and preload budget
    MediaCache.kt             Media3 disk cache and stream preloading
  net/
    Net.kt                    HTTP client, persistent cookies, AJAX requests
    Auth.kt                   Sign-in and Premium detection
    SecureStore.kt            Android Keystore-backed email storage
    Cdn.kt                    CDN selection and media availability checks
  parse/
    Models.kt                 Catalogue and stream models
    Parser.kt                 Jsoup HTML parsing
  download/
    DlTask.kt                 Persisted task and live progress models
    DlStore.kt                Download registry and observable byte counters
    DownloadService.kt        One foreground queue consumer
    DlEngine.kt               Transfer lifecycle and progress sampling
    DownloadRanges.kt         Byte range planning and response validation
    RangeTransfer.kt          Resumable HTTP range transfer
    DownloadFiles.kt          MediaStore publication and legacy file lookup
  ui/
    Nav.kt, AppState.kt        Screen stack and shared application state
    *Screen.kt                Home, browse, search, details, library, account
    Components.kt             Shared cards, badges, buttons, and animations
    BenchIcons.kt             Shared vector icons
    Formatters.kt             Duration formatting
    player/
      PlayerScreen.kt         ExoPlayer setup, stream loading, and lifecycle
      PlayerChrome.kt         Controls and brightness/volume/seek gestures
      PlayerSettingsSheet.kt  Quality, picture fit, and speed panel
      PlayerTimeline.kt       Scrubbing and buffered-position drawing
      VideoResolution.kt      Labels derived from decoded dimensions
      VideoTransform.kt       Bounded pinch zoom and two-finger pan
      VideoGestures.kt        Multitouch separated from one-finger swipes
      PinchIntent.kt          Hold and movement thresholds for accidental touches
      Pip.kt                  Picture-in-picture lifecycle and MediaSession
      PipActionReceiver.kt    Private destinations for explicit PiP actions
      PipPlaybackPolicy.kt    Visible/stopped/closed playback rules
    theme/Theme.kt            Colours, fonts, and language presentation
  util/SuspendResult.kt       Request errors without swallowing cancellation

app/src/test/                 JVM regression tests
app/src/main/res/             Fonts, artwork, icons, and versioned Android themes
```

## Local storage and database

The library and download registry use small JSON records in Android
SharedPreferences. There is no custom SQL or Room database. Media3 owns a
separate SQLite database for its media cache index.

| Store | Contents |
| --- | --- |
| `library.xml` | Saved films, resume positions, up to 20 continue-watching entries, last language, last known Premium status |
| `settings.xml` | Picture fit, playback speed, preload budget (24 MiB by default) |
| `downloads.xml` | `tasks_v2` JSON registry: task identity, URL, filename, status, total size, chunk count, errors, and completed media URI |
| `net.xml` | App-private session cookies, tab ID, and last selected CDN |
| `secure_store.xml` | Saved email encrypted with an Android Keystore AES-GCM key |
| App external files directory, `downloads/<task-id>/` | Temporary `.part` files, with an internal-files fallback when external app storage is unavailable |
| App cache directory, `stream/` | Up to 512 MiB of cached media, evicted by least recent use |
| Media3 database | Cache index managed by `StandaloneDatabaseProvider` |
| `Movies/Einthusan/` | Completed downloads published through MediaStore on Android 10+, or direct files on Android 8–9 |

Watchlists and resume progress stay on this device. They are not synchronized
through the Einthusan account. Normal app updates preserve them; clearing app
data or uninstalling removes the local history.

The saved-film and resume serializers share the existing JSON keys. The cleanup
keeps those preference names, the download registry key, and completed-file URIs
compatible with existing installations. Chunk resume positions come from the
actual `.part` file lengths; they are not a second database of byte counters.

Passwords are submitted for sign-in but are not saved. Old unused encrypted
password entries are removed when authentication initializes. Session cookies
persist in app-private preferences. Signing out clears cookies, the saved email,
Premium state, and the in-memory stream URL cache. Uninstalling the app removes
its private stores; published media files can remain in shared storage.

## Downloads and playback

A download is split into up to six ranges. Each connection resumes from its
part file's current length. The engine validates HTTP 206 and `Content-Range`,
rejects incomplete or oversized responses, and resets oversized partial files.
Only completed ranges are joined and published. Failed publication removes the
pending MediaStore entry. Removing a task does not allow a finishing transfer
to recreate its registry row.

The foreground service drains one queue at a time even if it receives several
start requests. Progress updates are atomic. Transfer meters and notification
tickers are cancelled when a transfer ends or fails.

Streaming uses a 512 MiB disk cache keyed by media path so renewed signed URLs
can reuse cached bytes. The details screen can preload up to 24 MiB by default.
The video view uses a TextureView so pinch transforms resize the actual video
as well as its container. Zoom is local to the current playback and is disabled
while the player is locked or the settings panel is open. A pinch must last at
least 120 ms and change spacing by at least 8% beyond the touch-slop threshold.
Brief accidental second touches do not zoom, seek, toggle controls, or become a
single-finger brightness/volume swipe when one finger lifts. Three-finger
contacts are ignored until all fingers lift.
The in-memory URL cache also checks signature expiry before reuse. MP4 playback
and HLS fallback are supported by the corresponding Media3 modules.

## Picture-in-picture controls

PiP Play and Pause use explicit commands and an Android Media3 MediaSession.
The control icon follows `playWhenReady`, including buffering, rather than
mistaking temporary buffering for a user pause. A private manifest receiver
routes PiP actions without relying on a dynamically registered receiver.

Closing PiP on Android 13+ saves progress, pauses and stops the player, and
finishes the playback activity through the system close action. On every
supported Android version, `onStop` pauses playback even if the system still
reports PiP mode during dismissal. A hidden or closing activity cannot start
late-loaded audio or resume through a media-session command. PiP uses normal
picture framing; returning fullscreen restores the current pinch zoom.

## Build and checks

Requirements for the checked-in configuration:

| Component | Version |
| --- | --- |
| Java | 21 |
| Gradle wrapper | 9.8.2 |
| Android Gradle Plugin | 9.5.1 |
| Kotlin | 2.4.21 |
| Compile / target SDK | 37 |
| Minimum Android API | 26 (Android 8) |
| Compose BOM | 2026.09.00 |
| Media3 | 1.11.1 |

Dependency versions live in `gradle/libs.versions.toml`.
Set `sdk.dir` in your own `local.properties`. This workspace's
`gradle.properties` points `android.aapt2FromMavenOverride` at the Termux aapt2
binary; change or remove that override when building elsewhere.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:assembleRelease
```

Outputs:

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Signed release: `app/build/outputs/apk/release/app-release.apk` (with local signing configuration)
- Unsigned release: `app/build/outputs/apk/release/app-release-unsigned.apk` (without local signing configuration)
- Tests: `app/build/reports/tests/testDebugUnitTest/index.html`
- Lint: `app/build/reports/lint-results-debug.html`

Release builds enable code and resource shrinking. This workspace now has a
dedicated RSA-3072 release key in the private `signing/` directory. Gradle reads
`signing/release.properties`; the key and passwords are excluded from source
control. Keep a secure backup of that directory for future updates. Builds made
without the local signing configuration produce an unsigned release APK.

The release certificate is separate from the debug certificate. A plain release
APK cannot directly replace a debug installation. This device now uses the
release certificate, so subsequent releases signed with the same key update it
normally. Do not uninstall a debug copy to switch certificates if you need its
local data; plan a supported signing-key migration first.

Signing establishes APK integrity and update identity, not Google approval.
A harmful-app classification needs investigation and, if incorrect, a
[Play Protect review](https://developers.google.com/android/play-protect/warning-dev-guidance#appeals).

Tests cover legacy movie and download JSON, byte-range boundaries and invalid
responses, real HTTP resume behavior against a local test server, catalogue and
watch parsing, cancellation propagation, decoded-resolution labels, and bounded zoom transforms.
Live account entitlements, CDN availability, and Android service behavior also
need device testing; passing JVM tests does not verify every film.

### Install through Shizuku in Termux

```sh
cat app/build/outputs/apk/debug/app-debug.apk | rish -c 'cat > /data/local/tmp/einthusan.apk'
rish -c 'pm install -r /data/local/tmp/einthusan.apk'
```

Installing with `-r` updates the existing package and keeps its app data when the
signing certificate matches. A signed release using another key cannot replace
a debug installation directly.

## Deep links

```text
einthusan://watch?id=9032&lang=hindi
einthusan://watch?id=9032&lang=hindi&play=1&title=Sultan
```

`play=1` opens the player; otherwise the link opens film details. Both cold and
warm links push a screen above Home or the current stack. Links are consumed
after routing so opening the same link again can work after navigating back.

## Current limits

- Availability, account access, and quality depend on Einthusan's live service.
- Separate subtitle tracks are not configured. Some films have burned-in subtitles.
- Signed download URLs can expire during a long pause; resuming does not currently
  refresh a download URL automatically.
- Android 8–9 publishing needs the legacy shared-storage permission. Android
  background execution limits still apply to the foreground download service.
- The app is an unofficial client; no affiliation with Einthusan is claimed.
