# Release signing and Play Protect review

## Signed artifact

- Package: `com.bench.einthusan`
- Version: `1.0.1` (code `2`)
- APK: `app/build/outputs/apk/release/app-release.apk`
- APK SHA-256: `00a739cc3aa2cfa440d11eccf199418a8116d618dd6ef2a6765f25a53e7d28d2`
- Release certificate SHA-256: `4c32e80833a56ccf93e8b770a682b05d64cefd36748c34b2634fad8454b55a27`
- Signer: `CN=Einthusan App Release`, RSA-3072, SHA256withRSA
- APK v2 and v3 signatures verified with Android SDK apksigner.
- APK ZIP integrity verified.
- This is a dedicated, self-signed Android release certificate, not the Android debug certificate or evidence of Google approval.

## Signing material

The private `signing/` directory contains the PKCS12 keystore, password
configuration, public certificate, and `private-release-key-backup.zip`.
The folder is excluded from source control; the key, configuration, and backup
have restricted filesystem permissions. Keep the backup private and secure.
Future updates need the same release key. `./gradlew :app:assembleRelease`
now signs automatically when this local configuration is present.

Debug installations use a different certificate and cannot be replaced by this
release with an ordinary update. This device is now using the dedicated release
certificate; release updates use the same key and preserve existing app data.

## Declared permissions

```text
package: com.bench.einthusan
uses-permission: name='android.permission.INTERNET'
uses-permission: name='android.permission.ACCESS_NETWORK_STATE'
uses-permission: name='android.permission.WAKE_LOCK'
uses-permission: name='android.permission.FOREGROUND_SERVICE'
uses-permission: name='android.permission.FOREGROUND_SERVICE_DATA_SYNC'
uses-permission: name='android.permission.POST_NOTIFICATIONS'
uses-permission: name='android.permission.WRITE_EXTERNAL_STORAGE' maxSdkVersion='28'
permission: com.bench.einthusan.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='com.bench.einthusan.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
```

The reviewed app source does not request SMS, contacts, notification-listener,
or accessibility-service access. No dynamic-code loading, shell execution,
or package-install request APIs were found in the app's own source. This is a
limited source/manifest review, not a malware-scan result or proof of safety.

## Play Protect clarification

The supplied screenshot says **App scan recommended** because Play Protect has
not seen this app before. It is not a harmful-app classification. Use **Scan app**
to request Google's security scan. Signing verifies integrity and update identity;
it is not evidence of Google approval and does not guarantee prompts disappear.
No external APK upload or Play Protect appeal was submitted here.

## PiP fix validation (1.0.1)

- Explicit Play, Pause and Close actions target an app-private receiver.
- A Media3 MediaSession routes system transport controls to the active player.
- Stopping/closing the activity pauses playback even during buffering or stale PiP flags.
- Close saves progress, stops the player and finishes the activity on Android 13+.
- Hidden screens cannot restart late-loaded audio through stream loading or session controls.
- All 31 JVM regression tests passed; lint has zero errors; signed release build passed.
- Device session monitoring observed PLAYING → PAUSED → PLAYING → PAUSED → session destroyed;
  the user confirmed PiP controls and closing behavior work.

[Google Play Protect guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)
