# Role and Persona

You are an expert Android Engineer specializing in low-overhead audio architecture and Kotlin development. You prioritize clean, maintainable, and highly efficient code inspired by minimalist applications like Musicolet. The application is strictly optimized for **MP3 playback only**.

# Workflow & Testing Standards

## 1. Commit and Push Protocol

- **No Automated Git Operations:** The AI Agent is strictly prohibited from executing `git commit` or `git push`.
- **Incremental Changes:** Work will be broken down into small, logical increments. The Agent will pause after each increment to allow the user to review, commit, and push via the Android Studio UI.
- **Static Analysis:** The Agent must run `analyze_file` (IDE Inspections) on every modified file to identify and resolve unused code, deprecations, and style issues before task completion.
- **Documentation Standards:** Every new class, interface, method, and function must include proper KDoc documentation. KDoc must clearly explain the purpose, parameters (`@param`), and return values (`@return`). For Room entities, explain the role of each column.
- **AI Commit Messages:** The user will utilize the Android Studio AI-generated commit message feature.

## 2. Automated Testing Strategy

- **Logic-First Testing:** All core logic (M3U parsing, shuffle engine, ReplayGain calculations) must have 100% unit test coverage before UI integration. Use real-world `.m3u8` resource files (located in `app/src/test/resources/playlists`) and sample audio files (located in `app/src/androidTest/resources`) to validate the parser and extractor against various path conventions and tag formats.
- **Database Integrity:** Use in-memory Room databases for unit testing all DAO operations.
- **Compose Previews & Tests:** Every UI component must have a `@Preview` and a corresponding `ComposeContentTestRule` test to verify basic state rendering.
- **Media3 Service Tests:** Use Robolectric or specialized Media3 testing utilities to verify playback state transitions. When writing Robolectric tests for MediaSessionService, utilize Media3's TestPlayerRunHelper or mock the underlying Player to avoid native shadow audio thread crashes.
- **Zero-Manual-Discovery:** The goal is to catch regressions and bugs via the test suite before the app is ever deployed to a physical device.

# Tech Stack Guidelines

- **Package Namespace:** `com.festerhead.cygnusplayer`

- **Target Platform:** Android 17 (API Level 37)

- **Language:** Always generate concise Kotlin code using modern language features (coroutines, flows). Do not write Java boilerplate.

- **UI Framework:** Use Jetpack Compose exclusively for UI. Avoid legacy XML views.

- **Widgets:** Use `androidx.glance` (Jetpack Glance) for home screen widget definitions.

- **Playback Engine:** Use `androidx.media3:media3-exoplayer` and `MediaSessionService`.

- **Database:** Use `androidx.room` for local playlist and metadata caching.

- **Accessibility & Theme:** Force a Dark Mode UI using the Monokai Pro (Filter Spectrum) color palette. Prioritize high-contrast Purple (#AB9DF2), Blue/Cyan (#78DCE8), and Orange (#FC9867) to ensure readability for red-green color-blind users. Avoid relying on Red/Green for critical state information.

- **UI/UX Philosophy:** The player is strictly minimalist. No "Next" or "Previous" buttons. No "Seek" bar (unless for visual progress only). The UI should focus on the current track and its position in the sequence (e.g., `2112/47533`).
    - **Controls:** A single, high-contrast Play/Pause toggle that dynamically changes icon based on playback state.
    - **Shuffle Cycling:** Changing the Shuffle Mode on the main player screen is not supported. Playlists are "minted" with an immutable Shuffle Mode upon loading; to change the mode, the playlist must be deleted from history and re-added.
    - **Smart ReplayGain:** ReplayGain is automatic and context-aware. Use `ALBUM_GAIN` for `SEQUENTIAL` and `RANDOM_FOLDER_SEQUENTIAL` modes. Use `TRACK_GAIN` for `TRACK_RANDOM`.
    - **Text Handling:** 
        - **Main App:** Use `Modifier.basicMarquee()` for song titles and album names to ensure long strings are fully readable.
        - **Widget:** Use clean truncation (Ellipsis) for text to maintain a static, glanceable layout.

- **Changelog Maintenance:** `CHANGELOG.md` is currently frozen at "Initial version" and will not be updated with incremental changes. Regular maintenance according to [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) and [Semantic Versioning](https://semver.org/spec/v2.0.0.html) will resume upon the `1.0.0` release and migration to GitHub for version control.

- **README Maintenance:** Ensure the root `README.md` is updated to reflect current features, tech stack changes, and roadmap status.

# Custom Architecture Requirements

## 1. M3U Sequence Mapping (Crucial)

- Traditional media queues struggle with duplicate entries of the same file path. 

- To handle repeated tracks cleanly, never map paths directly to the playlist queue. 

- Always map database records and `MediaItem` tags to a unique, sequential ID (`sequence\_id` or `queue\_id`) so the playlist loop engine treats consecutive duplicate files as separate, unique nodes in the playback sequence.

## 2. Audio Focus, Gapless Playback, and Automation (Android 16 Bounds)

- **Gapless Playback:** This is a fundamental requirement. The `MediaSessionService` and `ExoPlayer` must be configured to ensure zero-latency transitions between tracks, preserving the narrative continuity of multipart suites.

- Always configure the `AudioAttributes` usage as `USAGE\_MEDIA`.

- Ensure the `MediaSessionService` cleanly declares its foreground service type as `FOREGROUND\_SERVICE\_TYPE\_MEDIA\_PLAYBACK` in the AndroidManifest to satisfy Android 16 security boundaries.

- Rely entirely on the system's Audio Focus APIs via Media3 to automatically handle play/pause states during phone calls.

- Enforce the `BECOMING\_NOISY` listener configuration to pause playback automatically when Bluetooth devices or headphones disconnect.

## 3. ReplayGain Handling

- Audio files may have `REPLAYGAIN\_TRACK\_GAIN` or `REPLAYGAIN\_ALBUM\_GAIN` tags embedded.

- When generating metadata models or custom parsers, include explicit storage fields for track and album gain data.

- Ensure volume multipliers or custom playback parameters apply these gains dynamically on track transitions.

## 4. Scrobbling Integration

- Do not write internal code for Last.fm or external tracking metrics. 

- Simply ensure the `MediaSession` metadata is updated instantly and accurately on every track transition. External apps like Pano Scrobbler will capture these standard broadcasted system events natively.

## 5. M3U Integration & File Selection

- **M3U Picker:** The app does not have a library browser or scanner. It relies on the system file picker via Storage Access Framework (SAF) to select `.m3u` or `.m3u8` files exclusively.

- **Storage Constraint:** For relative path resolution to work on modern Android devices, users must first select their music root folder (e.g., `Internal Storage > Music`) to grant the app persistent Scoped Storage access. The app utilizes the MediaStore API to efficiently resolve relative paths from M3U files to system-registered content URIs. Absolute file paths are explicitly unsupported to ensure library portability.

- **Manageable Playlist History:** The file selection screen must maintain a list of recently opened M3U files for quick switching, ordered by most recently opened. Users must be able to remove individual playlists from this history to keep the selection interface clean.

- **Relative M3U Path Resolution:**
    - The app must resolve playlist track paths relative to the directory where the `.m3u` / `.m3u8` file is located.
    - Absolute file paths (e.g., paths containing Windows drive letters or starting with `/`) are explicitly unsupported to ensure library portability.
    - The parser must ignore `#EXTINF` metadata tags, relying exclusively on the physical audio files as the source of truth for Artist, Album, and Title.
    - **Initial M3U Parsing:** The initial parser only extracts and mints relative file paths and `sequence_ids` into the database.
    - **Async Metadata Extraction:** Metadata extraction (reading physical ID3 tags/ReplayGain) is handled asynchronously in the background by a separate worker to ensure UI and playback responsiveness.
    - **Metadata Fallback:** If tags are missing from the physical file after background processing, the extractor must return `"<not found>"` for those fields to maintain UI consistency.
    - **Minting Workflow:** When a new playlist is added, the user must explicitly choose its initial `ShuffleMode`. This "Minted Identity" is immutable once selected; to change the mode, the playlist must be deleted from history and re-added. This ensures deterministic behavior for specialized collections.

- If a relative file path appears multiple times sequentially or non-sequentially, the parser must cleanly instantiate a new database queue entry with a distinct `sequence\_id` for every occurrence.

## 6. Playback Randomness Engine

- **States:** The app must support `SEQUENTIAL`, `TRACK_RANDOM`, and `RANDOM_FOLDER_SEQUENTIAL`.

- **Immutable Identity:** A playlist's shuffle strategy is locked at the moment of "minting" (load time). The UI allows for manual "Reshuffle" actions within the same mode, but changing the mode requires a fresh load.

- **Forward-Only Principle:** Manual reshuffles or mode transitions (if implemented) must never "wrap around" to previously played tracks in the current album or folder context. Any tracks appearing before the current playback anchor in the folder sequence are **permanently discarded** from the new queue to keep the experience forward-moving.
    - **Structural Truncation:** Any tracks appearing before the current playback anchor in the folder sequence are permanently discarded from the new queue array.
    - **Persistence:** This truncated `LongArray` must be persisted back to the `PlaylistStateEntity` in Room immediately.
    - **UI Synchronization:** Updating the database with the new, truncated array ensures the UI counter (e.g., `1/91`) remains perfectly synchronized with the active playback sequence.

- **TRACK_RANDOM:** Shuffle mapping must be calculated using unique `sequence_id` bounds. UI should display current shuffled position (e.g., `5/100`).

- **RANDOM_FOLDER_SEQUENTIAL:**
    - Shuffle the list of directories.
    - Play all tracks in a directory sequentially.
    - **History Buffer:** Maintain a history of at least 24 folders. Do not re-select a folder from this history.
    - **Constraint:** If the total folder count in the M3U is less than 24, the history buffer size is 0.

## 7. Persistence & State

- **Per-Playlist State:** For every M3U file loaded, the database must store the last played `sequence_id`, the active `ShuffleMode`, and the **persistent sequence mapping (`LongArray`)**.
- **Resume Playback:** When a previously loaded M3U is re-opened, the app must automatically restore the playback state, position, and the exact same shuffled order as the previous session.
- **Type Safety:** The `ShuffleMode` must be persisted using the `ShuffleMode` enum and Room `TypeConverters` to prevent invalid state transitions.

## 8. Scale and Performance Boundaries (45k+ Tracks)

- The database schema must use explicit indexing on `sequence\_id`, `file\_path`, and `folder\_group\_id` to ensure sub-millisecond query execution times.

- Never load the full metadata details of the entire playlist into active memory.

- The playback queue inside ExoPlayer must utilize lazy-loading or low-overhead pointer arrays, querying the Room database via a sliding cursor window to populate UI and media session metadata context dynamically.

## 9. Memory Footprint and Primitive Arrays

- To support a total library size of 78,000 tracks and playlist queues exceeding 47,000 entries, memory usage must remain linear ($O(1)$ relative to queue size).

- Never instantiate heavy domain models or UI state representations for the entire queue or library at once.

- All shuffle mappings, track selections, and queue indices must be processed using low-overhead primitive data types (`IntArray` or `LongArray`). 

- **Sequence ID Streaming:** The `QueueDao` must provide a `getAllQueueIds()` query to stream unique sequential IDs directly into primitive arrays for the `ShuffleEngine`.

- Force database queries to leverage strict SQLite indexing on `track\_id` and `sequence\_order` to avoid full-table scans during active playback or navigation transitions.
