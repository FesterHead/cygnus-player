# Contributing to Cygnus Player

Thank you for your interest in contributing to **Cygnus Player**!

Cygnus Player is a minimalist, high-performance local audio player for Android, specifically engineered to handle massive, duplicate-heavy M3U/M3U8 playlists of **MP3 files** with sub-millisecond responsiveness.

We welcome pull requests that align with the project's core philosophy, architecture, and performance standards. Please take a moment to review these guidelines before submitting code changes.

---

## 🌌 Core Project Philosophy & Scope

Before proposing features or submitting pull requests, please keep the following design constraints in mind:

1. **Strictly MP3 Playback**: Cygnus Player is optimized exclusively for local MP3 playback. Feature additions for other container formats or streaming services are out of scope.
2. **Minimalist UX**: No "Next" or "Previous" buttons, and no interactive seek bar. Playback is an immutable journey once started. Controls are limited to a single high-contrast Play/Pause toggle.
3. **Linear Memory Footprint ($O(1)$)**: To support libraries of 78,000+ tracks and queue sizes exceeding 47,000 items, queue structures must rely on low-overhead primitive arrays (`LongArray`/`IntArray`) and Room database sliding cursors.
4. **Gapless Transitions**: Playback engine transitions must be zero-latency via `androidx.media3` (`ExoPlayer` & `MediaSessionService`).
5. **High-Contrast Accessibility**: The UI strictly uses the **Monokai Pro (Filter Spectrum)** color palette. Visual indicators prioritize high-contrast Purple (`#AB9DF2`), Blue/Cyan (`#78DCE8`), and Orange (`#FC9867`) to ensure readability for red-green color-blind users.

---

## 🌿 Branching Strategy & Workflow

* **Development Branch (`develop`)**: All feature development, refactoring, and experimental work target the `develop` branch.
* **Release Branch (`main`)**: Pull Requests submitted against `main` trigger the automated **Android Release Build** GitHub Actions workflow.
* **No Direct Automated Commits**: AI agents and automated scripts are prohibited from running `git commit` or `git push` directly. All changes must be manually reviewed and committed by human maintainers.

### Versioning Protocol

If your pull request introduces a new version release:

1. Update `VERSION_NAME` and `VERSION_CODE` in `version.properties`.
2. Keep `app/src/main/java/com/festerhead/cygnusplayer/VersionInfo.kt` synchronized with `version.properties`.
3. Update `CHANGELOG.md` following [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) and [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Keep unreleased changes under `## [Unreleased]`.

---

## 🏗 Architectural & Coding Guidelines

### 1. M3U Sequence Mapping

Traditional media queues struggle with duplicate entries of the same audio file. Cygnus Player assigns every playlist entry a unique, sequential ID (`sequence_id` or `queue_id`). Duplicate tracks within an M3U file must always be instantiated as distinct nodes in the Room database.

### 2. Relative Path Resolution & Storage

* All M3U relative paths are resolved relative to the `.m3u` / `.m3u8` file location using the Android **MediaStore API** and **Storage Access Framework (SAF)**.
* Absolute file paths (e.g., paths starting with `/` or containing Windows drive letters) are explicitly unsupported to ensure library portability.

### 3. Playback Randomness Engine

* **Supported Modes**: `SEQUENTIAL`, `TRACK_RANDOM`, and `RANDOM_FOLDER_SEQUENTIAL` (with a 24-folder history buffer).
* **Immutable Identity**: A playlist's shuffle mode is locked upon creation ("minting"). Changing modes requires re-adding the playlist.
* **Forward-Only Principle**: Manual reshuffles must never wrap around to previously played tracks in the current album or folder context. Played tracks are structurally truncated from active queue arrays.

### 4. Smart ReplayGain

* Use `ALBUM_GAIN` for `SEQUENTIAL` and `RANDOM_FOLDER_SEQUENTIAL` modes.
* Use `TRACK_GAIN` for `TRACK_RANDOM` mode.

### 5. Documentation & Code Quality

* **KDoc**: Every new class, interface, method, and public function must include comprehensive KDoc documentation explaining purpose, parameters (`@param`), and return values (`@return`).
* **Static Analysis**: All code must pass IDE inspections without deprecation warnings, unused imports, or style violations.

---

## 🧪 Testing Standards

Cygnus Player enforces a **Zero-Manual-Discovery** testing philosophy. Regressions must be caught by automated tests before code is merged.

1. **Logic-First Unit Testing**: Core logic (`M3uParser`, `ShuffleEngine`, ReplayGain multipliers) must have JVM unit test coverage (`app/src/test`). Use real `.m3u8` test resource files.
2. **Database Integrity**: Test all Room DAO operations using in-memory SQLite databases.
3. **Compose Previews & Tests**: Every Jetpack Compose UI component must provide a `@Preview` and a corresponding `ComposeContentTestRule` test in `app/src/androidTest`.
4. **Media3 Service Tests**: Verify playback state transitions and audio focus handling via Robolectric or Media3 test helpers.

To run the local test suite:

```powershell
# Wake up and unlock connected emulator
adb shell input keyevent 224; adb shell wm dismiss-keyguard

# Run full test suite
./gradlew test connectedDebugAndroidTest
```

---

## 📋 Pull Request Submission Checklist

Before submitting a Pull Request:

* [ ] Project builds cleanly via `./gradlew :app:assembleDebug`.
* [ ] All unit tests pass via `./gradlew test`.
* [ ] Code follows Kotlin style conventions with comprehensive KDoc comments.
* [ ] No raw absolute file paths or unhandled exceptions introduced.
* [ ] `CHANGELOG.md` is updated under `## [Unreleased]`.
* [ ] Pull request description clearly outlines the rationale, scope, and testing verification performed.
