# Changelog

All notable changes to PebbleDo are documented here.

---

## [1.1.1] — 2026-08-23

### Fixed

- **Launch no longer flashes the default (light) theme.** The native shell now persists the background color pushed by the web app's theme system and applies it to the window and WebView before the first frame — so a cold start with, say, Dark or Ocean selected goes straight into that theme with no brief light flash.

---

## [1.1.0] — 2026-08-23

Shell synced with upstream WebShell 1.0.2, plus a round of functional fixes across the bridge, back-button handling, and exports.

### Fixed

- **Haptic feedback now actually works.** Haptics were wired to `navigator.vibrate`, which Android WebView doesn't implement; vibrations route through the native bridge instead.
- **Hardware/gesture back button behaves properly.** It now closes the topmost dialog first, then exits multi-select, then returns from the Archive view — and only leaves the app when nothing is left. Previously any back press exited immediately.
- **Release builds keep their JS bridge.** ProGuard keep rules targeted the template package (`com.yourapp.*`) instead of `com.s17labs.pebbledo.*`, so minified builds could strip the native bridge methods.
- **Multiline tasks export correctly.** Internal line breaks no longer corrupt plain-text lists ("one task per line") or Markdown checklists; the copied-item count reflects tasks, not lines.
- **Export feedback is honest.** Copy failures no longer report success, and exporting an empty scope shows a toast instead of silently doing nothing.
- **No dark flash on light themes.** The window/WebView background matches the active theme and updates live when switching themes.
- **Onboarding welcome screen keeps its two-line layout** after language changes.
- **Slovak translations cleaned up** — Czech-isms and typos fixed (`čokoľvek`, `minimálny`, `Vitajte`, `viacnásobný`, `vymaž`) and plural forms corrected (1 úlohu / 2–4 úlohy / 5+ úloh).
- Persisted state is validated on load; a corrupted storage entry can no longer crash rendering.
- About dialog shows the real version number from the Android package instead of a hardcoded string.

### Changed

- Shell hardened per upstream WebShell 1.0.2: `allowFileAccess=false`, portable `gradle.properties` (machine-specific JDK/aapt2 overrides moved out of the repo), obsolete Jetifier flag removed, `local.properties` no longer tracked.
- Added the `INTERNET` permission (unused today; reserved for future fetch/API features).
- Removed leftover template starter files (`app.js`, `style.css`) and a dead `fonts.gstatic.com` preconnect.

### Added

- **CI workflow** — every push/PR gets wrapper validation and a cached debug-APK build.
- **Automated releases** — pushing a `v*` tag builds debug + release APKs, signs them, publishes SHA-256 checksums, and creates the GitHub Release with notes extracted from `CHANGELOG.md`.
- **Signed release builds.** Releases are signed so Android will actually install them: a public keystore (`signing/release.keystore`, alias `pebbledo`) is committed for consistent FOSS signing; a private key can take over anytime via `keystore.properties` or CI secrets (note: switching keys later requires uninstall/reinstall).
- Optional release signing configuration via a gitignored `keystore.properties`.

---

## [1.0.0] — 2025

Initial release.

- Minimal, distraction-free task list: single-tap complete/edit, archive with restore, drag-to-reorder, long-press multi-select with bulk actions
- Multiline task support
- Export to clipboard as plain text or Markdown, scoped to active/archived/all
- 8 themes, 4 fonts, adjustable font size and corner radius
- English & Slovak UI, haptics toggle, 5-page onboarding
- Fully offline; all data stored locally
