# 📝 Description

Please include a summary of the change, the rationale behind it, and any relevant motivation or context.

Fixes / Closes #(issue number)

## 🌌 Scope & Architecture Checklist

Please confirm that your Pull Request adheres to Cygnus Player's core constraints:

- [ ] **MP3 Playback Only**: Changes are strictly for local MP3 audio files and M3U/M3U8 playlists.
- [ ] **Minimalist UX**: Maintains single Play/Pause control; no seek bars or skip/navigation buttons added.
- [ ] **Linear Memory Footprint ($O(1)$)**: Low memory footprint relative to queue size maintained using primitive arrays (`LongArray`/`IntArray`) and Room sliding cursors.
- [ ] **Gapless & Audio Focus**: Media3 gapless playback and Android Audio Focus handling preserved.

## 🧪 Testing Checklist

- [ ] I have executed `./gradlew test` locally and all unit tests pass.
- [ ] New classes, interfaces, methods, and functions include full KDoc documentation (`@param`, `@return`).
- [ ] No raw absolute file paths or unhandled exceptions introduced.
- [ ] `CHANGELOG.md` updated under `## [Unreleased]` (if applicable).
