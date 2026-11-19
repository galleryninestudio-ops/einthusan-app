# Playback and downloads

[← App overview](../README.md)

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
