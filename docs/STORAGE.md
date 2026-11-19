# Storage and account data

[← App overview](../README.md)

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

Watchlists and resume progress are stored locally. The app does not synchronize
them through the Einthusan account. Android device backup is separate from app-level sync; the manifest currently allows Android backup. Normal app updates preserve them; clearing app
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
