# Architectural Strategy: Android Auto Integration

## Purpose
To provide a safety-compliant, minimalist "hands-free" interface for car head units while strictly adhering to our minimalist UI philosophy.

## Strategy: Minimalist `MediaLibraryService`
Instead of a full-featured library browser, we will implement a "Thin-Interface" `MediaLibraryService`:

1.  **Root Node**: Return a single virtual root node labeled "Playlists".
2.  **Children**: Map the current playlist history into the library hierarchy so the car head unit can navigate between your active M3U sessions.
3.  **No-Skip Compliance**: The Android Auto interface will honor the standard `MediaSession` transport controls. We will implement them to match our minimalist philosophy:
    *   **Play/Pause**: Supported.
    *   **Skip Next/Previous**: Explicitly **Disabled** or mapped to "Play Next Folder/Playlist" to respect the "Forward-Only" shuffle principle.
4.  **UI Minimalization**: We will provide basic track information (`title`, `artist`) to the car display, ensuring that the driver sees exactly what is on the phone screen without adding extra metadata or browser bloat.

## Implementation Requirements
- **Manifest**: Must include `<intent-filter>` for `androidx.media3.session.MediaLibraryService`.
- **ExoPlayer Integration**: The service must share the same `ExoPlayer` instance used by our `MediaSessionService` to ensure playback state consistency between the car head unit and the phone.
