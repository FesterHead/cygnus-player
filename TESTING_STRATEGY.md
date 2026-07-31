# Cygnus Player Testing Strategy

To achieve **Zero-Manual-Discovery** of bugs, Cygnus Player employs a rigorous multi-layered testing strategy. No feature is considered complete without its corresponding suite of automated tests.

## 0. Static Analysis (Primary Gate)

- **Target:** All modified source files.
- **Tool:** `analyze_file` (IDE Inspections).
- **Approach:** Run analysis on every modified file to catch unused parameters, deprecated API usage, and lint warnings before proceeding to functional testing.

## 1. Core Logic & Shuffle Engine (100% Coverage)

- **Target:** M3U Parsers, Shuffle Mapping Logic, ReplayGain Multipliers, and Queue Sequence IDs for **MP3 files**.
- **Frameworks:** JUnit 4 (Unit), JUnit 5 (Unit), MockK, Kotlin Coroutines Test.
- **Approach:**
  - **Logic-First Unit Testing:** Core components like `M3uParser` and `ShuffleEngine` (mapping logic) are tested via JVM unit tests for sub-second validation.
  - **Forward-Only Verification:** Explicitly verify that re-shuffles or folder transitions discard previously played tracks and never "wrap around" to the start of an album.
  - Use real-world `.m3u8` resource files (located in `app/src/test/resources/playlists`) to verify path resolution for both Windows and Unix style sources.
  - Stress-test the `ShuffleEngine` with 50,000+ entries (using `massive_library.m3u8`) to ensure $O(1)$ memory performance and sequence integrity.

## 2. Database & Persistence

- **Target:** Room DAOs, Migration Logic, and SQLite Indexing.
- **Frameworks:** AndroidX Test, Room In-Memory Databases.
- **Approach:**
  - Every DAO method must have a test case verifying CRUD operations.
  - Specifically verify that duplicate `file_path` entries result in unique `sequence_id` records as per the architecture.
  - **Logic Consistency:** Verify that `TrackEntity` correctly defaults missing metadata to `"<not found>"` and that `PlaylistStateEntity` correctly serializes the `ShuffleMode` enum.

## 3. UI & Jetpack Compose

- **Target:** All Composables, UI State transitions, and Accessibility.
- **Frameworks:** Compose Test Rule (Instrumented), Screenshot Testing (optional/future), Glance Testing for widgets.
- **Approach:**
  - Use `@Preview` for every component to ensure Monokai Pro color compliance.
  - `ComposeContentTestRule` is used in **instrumented tests** (`app/src/androidTest`) to verify that UI nodes respond correctly to state changes (e.g., history list rendering and Snackbar visibility).
  - Test for high-contrast accessibility labels.

## 4. Media Playback (Media3)

- **Target:** `MediaSessionService`, ExoPlayer state transitions, Audio Focus handling.
- **Frameworks:** Robolectric, Media3 Test Utilities.
- **Approach:**
  - Simulate `BECOMING_NOISY` and phone call interruptions to verify automatic pausing.
  - Verify that `MediaMetadata` is updated accurately on every track transition for Scrobbler compatibility.

## 5. Continuous Validation

- **Command:** `./gradlew test` (Local Unit Tests) and `./gradlew connectedAndroidTest` (Instrumented Tests).
- **Policy:** Tests must run successfully before any commit is pushed by the user.
