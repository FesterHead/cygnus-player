# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Release policy:

- Keep in-progress changes under `## [Unreleased]` while work is still in progress on non-`main` branches.
- Mint the numbered release heading before opening a pull request to `main`.
- The first numbered release for this project will be `1.0.0`.
- Version numbers follow SemVer 2.0.0: MAJOR for incompatible changes, MINOR for backward-compatible functionality, and PATCH for backward-compatible bug fixes.

## [Unreleased]

## [1.0.5] - 2026-08-23

### Added

- Documentation for Android Auto Developer Mode, "Unknown sources" sideloading setup, and local Desktop Head Unit (DHU) emulator testing in `README.md` and `TESTING_STRATEGY.md`.
- Robolectric unit tests in `CygnusPlaybackServiceUnitTest` for Android Auto service intent filters and `MediaLibraryCallback.onGetLibraryRoot`.

### Changed

- Refactored PowerShell aliases (`ctest`, `crun`, `cdebug`, `cauto`) to v1.5, updating `ctest` to run the full test suite (`./gradlew test connectedDebugAndroidTest`), implementing environment sanitization (unsetting `GRADLE_HOME`/`JAVA_HOME`), and adding process management for the Desktop Head Unit (DHU).
- Refactored `SharedPreferences` usage in `PlaylistPickerViewModel` and `CygnusPlaybackService` to use modern idiomatic Kotlin KTX `edit { ... }` blocks, resolving static analysis warnings.
- Hardened ReplayGain parsing in `Media3MetadataExtractor` to handle variations in TXXX tag description whitespace and `dB` unit formatting.
- Refined playlist file picker in `PlaylistPickerScreen` to specifically filter for M3U and M3U8 MIME types.
- Enforced ultimate minimalism in Android Auto by disabling library browsing and hiding next/previous navigation controls via `ForwardingPlayer`, aligning the head unit experience with the "Immutable Journey" philosophy.
- Implemented dynamic Media Session ID generation to force Android Auto cache refreshes and prevent "phantom" playlist suggestions.
- Restored legacy `MediaBrowserService` intent filter to ensure app visibility in Android Auto while maintaining internal code lockdown.

### Fixed

- Added path sanitization in `PlaylistPickerViewModel` to prevent corrupted object `toString()` metadata from leaking into the UI.
- Resolved Android Auto app recognition issue ([#7](https://github.com/FesterHead/cygnus-player/issues/7)) by declaring `com.google.android.gms.car.notification.SmallIcon` metadata, adding `androidx.media3.session.MediaSessionService` intent filter action to `CygnusPlaybackService`, and cleaning up invalid manifest properties.
- Fixed string interpolation bug in `CygnusPlaybackService` library item generation that caused playback failures in Android Auto.
- Removed hardcoded local Windows JDK path (`org.gradle.java.home`) from `gradle.properties` to fix GitHub Actions CI build failures on Linux runners.

## [1.0.4] - 2026-08-11

### Added

- Added `.markdownlintignore` configuration ignoring AI artifact files (`implementation_plan.md` and `walkthrough.md`).

### Changed

- Refined PowerShell aliases (`ctest`, `crun`, `cdebug`) in `README.md` to use the `ANDROID_SERIAL` environment variable within a `try/finally` block, ensuring reliable device targeting and environment sanitization.
- Added specialized `cauto` PowerShell alias for streamlined Android Auto Desktop Head Unit (DHU) testing, handling port forwarding and debug installation in one step.
- Updated PowerShell aliases to include an `adb uninstall` step and support dual-target switching between physical phone and emulator.
- Relaxed `DatabaseStressTest` query thresholds (Single: 10ms, Folder: 50ms) to ensure stability on physical hardware under background OS load.

### Fixed

- Display configured Music Root Folder on Settings screen with URL decoding and marquee formatting ([#3](https://github.com/FesterHead/cygnus-player/issues/3)).
- Resolved home screen widget metadata update delay on initial add during active playback via `REQUEST_WIDGET_UPDATE` broadcast ([#4](https://github.com/FesterHead/cygnus-player/issues/4)).
- Display playing triangle indicator on playlist screen for active playlist and position active playlist at top of history list ([#5](https://github.com/FesterHead/cygnus-player/issues/5)).

## [1.0.3] - 2026-08-01

### Added

- Standardized `.github/PULL_REQUEST_TEMPLATE.md` checklist for contributor pull requests.
- Structured GitHub Issue Forms (`bug_report.yml`, `feature_request.yml`, and `config.yml`) in `.github/ISSUE_TEMPLATE/`.
- Official `CONTRIBUTING.md` guidelines detailing contribution workflow, scope boundaries, architectural constraints, and testing standards.
- Project-level `.markdownlint.json` and `.vscode/settings.json` configuration disabling `MD025` (multiple top-level headings) and line length checks, allowing screenshot `<img>` tags, and setting `MD024` sibling-only heading rules.

### Changed

- Hardened `.github/workflows/pr-build.yml` for fork PRs by isolating signing keystore steps behind secret presence checks.
- Updated `README.md` Project Status & Disclaimer section to indicate that Pull Requests aligning with project goals may be accepted, linking to `CONTRIBUTING.md`.
- Refined Markdown formatting, list indentation, line spacing, and heading syntax across `README.md`, `TESTING_STRATEGY.md`, `LICENSE`, `AGENTS.md`, and KDoc comments in `VersionInfo.kt`.

## [1.0.2] - 2026-07-25

### Added

- Official `PRIVACY.md` policy documenting on-device storage access and zero data collection.
- Project Status & Disclaimer section in `README.md`.

## [1.0.1] - 2026-07-25

### Added

- Minimalist Settings screen with version info and project links.
- "Reset Music Root Folder" utility for easier storage reconfiguration.
- Basic database diagnostics (Track and Playlist counts).

### Fixed

- Resolved Android 17.1 (API 37) binder deadlock by deferring startup permission requests.
- Improved UI test stability with robust `waitUntil` synchronization.
- Stabilized database stress tests for virtualized emulator environments.

## [1.0.0] - 2026-07-23

- Initial version
