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

### Added

- Project-level `.markdownlint.json` and `.vscode/settings.json` configuration disabling `MD025` (multiple top-level headings) and line length checks, allowing screenshot `<img>` tags, and setting `MD024` sibling-only heading rules.

### Changed

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
