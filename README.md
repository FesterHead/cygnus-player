# Cygnus Player

Cygnus Player is a minimalist, high-performance local audio player for Android, specifically engineered to handle massive, duplicate-heavy M3U/M3U8 playlists of **MP3 files** with sub-millisecond responsiveness.

Designed for collectors with large libraries, Cygnus Player prioritizes a low memory footprint ($O(1)$ relative to queue size) and absolute sequence integrity.

## 🌌 Why "Cygnus Player"?

The name is a direct tribute to the legendary progressive rock band **Rush** and their epic multi-album masterpiece, *Cygnus X-1*. 

Specifically, it honors what is "the ultimate transition" in rock history: the frantic, heavy sci-fi cliffhanger ending of "Cygnus X-1 Book I: The Voyage" (the finale of *A Farewell to Kings*) bridging seamlessly into the triumphant, complex multi-movement opening of "Book II: Hemispheres" on the subsequent *Hemispheres* album.

Modern commercial streaming apps and feature-bloated players treat music like a data-harvesting commodity. To borrow a line from Rush in *Superconductor*: **"That's entertainment!"** But it isn't an optimal listening experience for a serious archive. Traditional metadata-tag or basic folder-shuffling players completely destroy complex narrative arcs—randomly inserting a compressed compilation track or a raw bootleg right in the middle of a gapless, multi-album experience. 

**Cygnus Player** is engineered to anchor precise M3U sequences down to the millisecond, preserving historical sonic continuity while still providing the mathematical flexibility to drop the entire library into total chaos mode when desired.

## 🚀 Key Features

- **Sequence-Critical Playback**: Unlike traditional players, Cygnus treats every entry in an M3U playlist as a unique node. Duplicate tracks in a sequence retain independent selection probabilities and metadata context.
- **Massive Scale Support**: Optimized Room database schema and primitive-array shuffle mappings handle 45,000+ item queues and 78,000+ track libraries with zero lag.
- **Advanced Shuffle Modes**:
    - `SEQUENTIAL`: Standard linear playback.
    - `TRACK_RANDOM`: True shuffle across the entire sequence.
    - `RANDOM_FOLDER_SEQUENTIAL`: Shuffles directory groups, playing all tracks within a folder sequentially before moving to the next random folder (with a 24-folder history buffer).
- **Minted Identity**: Playlists are assigned a shuffle strategy at the moment of creation, ensuring deterministic behavior for specialized collections (e.g., a dedicated "Chaos" vs. "Album" playlist).
- **No-Skip Philosophy**: Designed for the "full experience" listener—no forward/back controls. Playback is an immutable journey once started.
- **Dynamic ReplayGain**: "Smart" real-time volume normalization. Automatically applies `ALBUM_GAIN` for sequential flows and `TRACK_GAIN` for randomized tracks.
- **Modern Android Core**: Built for Android 17 (API 37) using Jetpack Compose, Media3 (ExoPlayer), and Jetpack Glance.

## 📁 Storage & Scoped Storage Compliance

Cygnus Player is fully compatible with modern Android **Scoped Storage** requirements. To ensure high-performance relative path resolution for massive libraries, please follow these steps:

1.  **Select Music Root**: On first launch, use the prompt to select your main music directory (e.g., `Internal Storage > Music`). This grants Cygnus persistent, recursive access to your entire library.
2.  **Relative Path Resolution**: The app uses the **MediaStore API** to map M3U relative paths (e.g., `Rush/2112.mp3`) to system-registered content URIs. This avoids restricted direct filesystem access and ensures absolute sequence integrity.
3.  **Permissions**: Ensure the `READ_MEDIA_AUDIO` permission is granted to allow the system to index your music files for the MediaStore.

## 🤖 AI-Assisted Development

This project is developed and managed using **Gemini 3.1 Flash Lite** via a Google AI Studio subscription. The architecture, implementation, and repository maintenance are guided by specialized AI agents to ensure high-performance, minimalist engineering standards.

## 🛠 Tech Stack

- **Target Platform**: Android 17 (API Level 37)
- **UI Framework**: Jetpack Compose
- **Playback Engine**: `androidx.media3:media3-exoplayer` & `MediaSessionService`
- **Database**: `androidx.room` with SQLite indexing on `sequence_id` and `file_path`
- **Widgets**: `androidx.glance`
- **Language**: Kotlin with Coroutines and Flow

## 🏗 Architecture Highlights

### 1. Unique Sequence Mapping
Traditional media queues struggle with duplicate file paths. Cygnus Player maps every playlist entry to a unique `sequence_id`. This allows the engine to distinguish between multiple occurrences of the same file, preserving the exact intent of the M3U creator.

### 2. Persistent Pointer Arrays
To ensure "Resuming" never triggers a new shuffle, Cygnus stores the generated playback sequence (`LongArray`) in the database. When you switch between your specialized playlists, you return to the exact track and sequence state you left.

### 3. O(1) Memory Footprint
To support enormous playlists, the `ShuffleEngine` operates on primitive `LongArray` mappings. Heavy domain models and metadata are never loaded for the entire queue at once. Instead, Cygnus utilizes a sliding cursor window to lazily populate UI and media session context.

### 3. Folder-Aware Engine
The database tracks folder relationships via a dedicated `FolderEntity`. This enables native support for folder-based automation and randomized folder sequencing without expensive file system traversals during playback.

## 🎨 Branding & Iconography

### The "Singularity" Icon
Cygnus Player utilizes a custom-designed **Adaptive Icon** that reflects the cosmic and musical themes of the project:

- **Design Principle**: A minimalist geometric representation of **Cygnus X-1**, the first black hole discovered in our galaxy.
- **Visual Elements**:
    - **The Singularity**: A central circle (Monokai Purple) representing the core of the music and the point of no return for the listener's focus.
    - **The Event Horizon**: Four symmetrical circular arcs (Monokai Blue/Cyan) implying rotation, gravitational pull, and the dynamic energy of the audio sequence.
- **Rationale**: The design uses common geometric property to ensure absolute **Copyright Safety**. It avoids literal depictions or franchise-specific imagery while maintaining a high-performance, scientific aesthetic.
- **Accessibility**: Optimized for the **Monokai Pro (Filter Spectrum)** palette, ensuring high visibility for red-green color-blind users.

## 📈 Status & Roadmap

- [x] **Core Database**: Entities for massive library management.
    - [x] `TrackEntity`: Metadata and ReplayGain storage (with non-nullable defaults).
    - [x] `FolderEntity`: Directory-aware grouping.
    - [x] `QueueEntity`: Unique sequence mapping (duplicates support).
    - [x] `PlaylistState`: Per-M3U persistence with strongly-typed `ShuffleMode`.
- [x] **Data Logic**: High-performance M3U parser and file picker integration.
    - [x] M3U/M3U8 Parser: Efficient line-by-line relative path resolution.
    - [x] Metadata Extractor: Background extraction of ReplayGain and media tags (with `"<not found>"` fallback).
    - [x] File Picker: System integration for playlist selection.
    - [x] Playlist History: Manageable UI for recently opened M3Us (supports removal).
- [x] **Core Logic**: High-performance sequence and shuffle management.
    - [x] `ShuffleEngine`: $O(1)$ memory-efficient primitive array mappings with **Forward-Only** logic.
    - [x] Folder-Sequential Logic: History-aware directory shuffling (24-folder buffer).
    - [x] ReplayGain Controller: "Smart" gain switching logic (Album vs. Track).
- [x] **Playback**: Media3 Service integration with ReplayGain and Audio Focus.
    - [x] `MediaSessionService`: Foreground service with Android 17 security bounds.
    - [x] ExoPlayer Core: Gapless transition and volume normalization.
    - [x] Lazy Queue Controller: Sliding window logic for $O(1)$ memory playback.
    - [x] System Integration: Audio Focus and `BECOMING_NOISY` handling.
    - [x] Playback Control Integration: Hooking UI Play/Pause actions to the service.
    - [x] Scoped Storage Compliance: Folder-based access and MediaStore URI resolution.
- [x] **UI Baseline**: Minimalist playback screen (Index/Total display) and Home Screen Widget. No navigation controls.
    - [x] Theme: Monokai Pro palette with high-contrast accessibility (Red-Green).
    - [x] App Icon: "Singularity" adaptive icon (Cygnus X-1 theme).
    - [x] Main Screen: Branding updated to "Cygnus Player" and Marquee text implemented.
    - [x] Glance Widget: Minimalist 4x1 high-contrast remote views with dynamic artwork.
- [x] **Advanced Features**: Relative path sanitization improvements, Scrobbler integration testing, and **Minimalist Android Auto support** (via `MediaLibraryService` for safe, voice-controlled library access).
    - [x] Android Auto & AAOS Support: Minimalist browsing and dashboard control.
    - [x] Position Persistence: Per-playlist millisecond-accurate resumption.
    - [x] Smart Bluetooth: Automated playback resumption upon device reconnection.


## 🧪 Running Tests

To maintain "Zero-Manual-Discovery" of bugs, Cygnus Player uses a dual-layered testing strategy. For more details, see [TESTING_STRATEGY.md](TESTING_STRATEGY.md).

### 1. Unit Tests (Logic-First)
Run these for sub-second validation of parsers, mapping logic, and shuffle algorithms on the JVM.
```powershell
./gradlew test
```

### 2. Instrumented Tests (Device Validation)
Run these to verify Room database integrity and physical media extraction on a connected device or emulator.
```powershell
./gradlew connectedDebugAndroidTest
```

> [!TIP]
> You can run both suites sequentially using:
> ```powershell
> ./gradlew test connectedDebugAndroidTest
> ```
>
> If Gradle skips tests due to UP-TO-DATE checks, you can force execution using:
> ```powershell
> ./gradlew clean test # Deletes build artifacts and runs everything fresh
> ./gradlew test --rerun-tasks # Runs all test tasks regardless of cache
> ```

## 🚀 Deployment

To build the APK and install the app on your connected device or emulator from the CLI:

```powershell
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.festerhead.cygnusplayer/.MainActivity
```

> [!NOTE]
> If `adb` is not recognized, you may need to use the full path to the executable (e.g., `C:\Users\<User>\AppData\Local\Android\Sdk\platform-tools\adb.exe`) or add the `platform-tools` directory to your system's `PATH`.

---

## 📜 Credits & Licensing

- **"Impact Moderato"** by Kevin MacLeod ([incompetech.com](https://incompetech.com)). Licensed under [Creative Commons: By Attribution 4.0 License](http://creativecommons.org/licenses/by/4.0/). Used for automated metadata extraction testing.

---

Cygnus Player adheres to [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) and [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Check `CHANGELOG.md` for the latest updates.
