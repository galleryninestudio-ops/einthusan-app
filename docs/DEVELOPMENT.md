# Development

[← App overview](../README.md)

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

Release builds enable code and resource shrinking. Maintainer builds use a dedicated RSA-3072 release key in the private `signing/` directory. Gradle reads
`signing/release.properties`; the key and passwords are excluded from source
control. Keep a secure backup of that directory for future updates. Builds made
without the local signing configuration produce an unsigned release APK.

The release certificate is separate from the debug certificate. A plain release
APK cannot directly replace a debug installation. Subsequent releases signed with the same key can update an existing release installation. Do not uninstall a debug copy to switch certificates if you need its
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

## Deep links

```text
einthusan://watch?id=9032&lang=hindi
einthusan://watch?id=9032&lang=hindi&play=1&title=Sultan
```

`play=1` opens the player; otherwise the link opens film details. Both cold and
warm links push a screen above Home or the current stack. Links are consumed
after routing so opening the same link again can work after navigating back.

## Related documentation

- [Storage and account data](STORAGE.md)
- [Playback and downloads](PLAYBACK.md)
- [Release verification](RELEASE_VERIFICATION.md)
