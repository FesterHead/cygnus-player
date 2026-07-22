# Cygnus Player: Copilot Guardrails & Rules

You are reviewing code for **Cygnus Player**, a minimalist, high-performance local audio player for Android.

## 🚫 CRITICAL CONSTRAINTS (DO NOT SUGGEST THESE)

1. **NO Non-MP3 Support:** The player is strictly MP3-only. Do not suggest adding extractors, parsing logic, or UI elements for FLAC, WAV, AAC, or OGG.
2. **NO Navigation Controls:** The UI philosophy is minimalist. Do not suggest adding "Next", "Previous", or "Seek" controls to the UI.
3. **NO Absolute File Paths:** Do not suggest using `java.io.File` with absolute paths. All file access must be mapped relatively from M3U files to MediaStore URIs via Scoped Storage.
4. **NO Heavy Object Queues:** Do not suggest loading domain models for the entire playlist into memory. Playback queue mappings must be calculated using low-overhead primitive data types (`IntArray` or `LongArray`).

## ✅ ARCHITECTURE RULES (ENFORCE THESE)

1. **Duplicate Sequence IDs:** M3U playlists can contain the same file path multiple times. Always ensure PRs rely on `sequence_id` (not `file_path`) to distinguish tracks in the queue.
2. **Forward-Only Shuffling:** When shuffling, any tracks appearing before the current playback anchor in a folder/album context must be permanently discarded from the new queue array.
3. **Smart ReplayGain:** Verify that volume multipliers dynamically apply `ALBUM_GAIN` for sequential modes and `TRACK_GAIN` for randomized modes.
4. **Jetpack Compose Only:** Enforce that all UI additions use Jetpack Compose and adhere to the high-contrast Monokai Pro color palette (prioritize Purple, Blue/Cyan, and Orange for color-blind accessibility).
5. **Foreground Service:** Ensure any modifications to `MediaSessionService` cleanly declare the `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK` to satisfy Android 14+ bounds.
