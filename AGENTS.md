# AGENTS.md

Guidance for AI coding agents (OpenCode, Claude Code, etc.) working in this repository.

## Project Overview

PebbleDo is a minimal, distraction-free to-do app for Android: a single-activity Kotlin
"shell" that hosts a vanilla HTML/CSS/JS single-page app inside a full-screen WebView.

- Language/stack: Kotlin 2.0.21 (WebView shell) + vanilla JavaScript/CSS/HTML front-end in `app/src/main/assets/www/` — no Compose, no frameworks
- Package/module: `com.s17labs.pebbledo`
- Toolchain: minSdk 26, compile/target SDK 35, JVM target 11 (CI builds on JDK 17), AGP 8.7.3, Gradle Kotlin DSL with version catalog
- Author/maintainer: yungsamd17 (https://github.com/yungsamd17)

## Build & Verify

```bash
./gradlew assembleDebug   # build debug APK (also triggers the downloadAssets task)
./gradlew lint            # Android lint (not part of CI; no unit-test sourceset exists yet)
```

There are no automated tests in this repo — CI only compiles. Verify UI changes by careful
review of `index.html` logic, and let CI confirm the build.

- CI (`.github/workflows/ci.yml`) runs on every push to `main` and on PRs: Gradle wrapper
  validation, JDK 17 + Android SDK setup, then `./gradlew assembleDebug --stacktrace`, uploading
  the debug APK as an artifact.
- `.github/workflows/release.yml` runs on `v*` tags: builds debug + release APKs,
  extracts the matching section from `CHANGELOG.md` into the release notes,
  and publishes a GitHub Release.
- Local sandboxes often lack the Android SDK/JDK or other toolchains — if builds can't run locally,
  rely on careful code review and let CI verify. Never skip updating tests when changing shared interfaces.

## Architecture

```
app/src/main/kotlin/com/s17labs/pebbledo/
  MainActivity.kt      # WebShell: hosts the full-screen WebView, back-button handling,
                       # theme background persistence (SharedPreferences "webshell")
  NativeBridge.kt      # @JavascriptInterface methods exposed to JS as window.Native
                       # (toast, device info, open URL, vibrate, share, emit)
app/src/main/assets/www/
  index.html           # the entire web UI: one file containing HTML, CSS and JS (~1.3k lines);
                       # tasks/archive/settings state lives here
  bridge.js            # JS wrapper around window.Native; degrades gracefully when running
                       # in a desktop browser (window.Native undefined)
```

Key patterns:

- Bridge protocol, JS -> native: call `NativeBridge.*` wrappers from `bridge.js` (which forward to
  `window.Native.*`). Native -> JS: `webView.evaluateJavascript("window.__bridge_<event>(payload)")`;
  JS registers listeners via `NativeBridge.on('<event>', cb)` (e.g. `__bridge_backButton`).
- Adding a new bridge method requires BOTH sides: a `@JavascriptInterface` method in
  `NativeBridge.kt` AND a corresponding wrapper in `assets/www/bridge.js`. Never call `window.Native`
  directly from app code.
- Threading: `@JavascriptInterface` methods run on a background thread. Any UI work must be wrapped
  in `activity.runOnUiThread { ... }`; `evaluateJavascript` must run on the main thread.
- Persistence: task/archive/settings data lives in WebView `localStorage` keys `pd_t`, `pd_a`, `pd_s`,
  saved with a ~800 ms debounce (`scheduleSave()`). The only native-side storage is the theme
  background color (SharedPreferences `"webshell"`), used to avoid a launch-time theme flash.
- Navigation policy: only `file://` asset navigation is allowed inside the WebView;
  http(s)/mailto/tel URLs open in the system browser; all other schemes are silently blocked.

## UI Conventions

The UI is entirely the web app in `index.html`:

- Theming works through CSS custom properties set at runtime (`setVar`) — 8 themes, 4 font choices,
  adjustable font size and corner radius; keep styling driven by those variables, not hardcoded colors.
- User-facing strings go through the translation table `T` with the `t(key, ...)` helper
  (English and Slovak). Never hardcode visible strings in markup or handlers.
- HTML inserted dynamically is escaped via `esc()` — keep using it for any user-provided content.

## Commit Messages

Format: `type(scope): short imperative summary` — lowercase after type, no trailing period.
Keep commits atomic — one logical change per commit.

| Type | Use for |
|---|---|
| `feat` | new user-facing feature |
| `fix` | bug fix |
| `refactor` | code change that neither fixes nor adds behavior |
| `style` | formatting/UI polish without logic change |
| `test` | adding or fixing tests |
| `docs` | documentation only |
| `chore` | build, deps, CI, tooling |
| `release` | version bump / release tagging |

Scope is a short area name for this project (e.g. `app`, `www`, `bridge`, `ci`).
Use plain `type:` only when a change genuinely spans everything (rare).

Examples:

```
feat(app): add archive screen with restore and delete
fix(bridge): keep back press inside the page until root state
chore(ci): run lint in build workflow
docs(readme): document commit message types
release: v1.1.1
```

## Agent Guardrails

- Never commit or push directly to `main`; all changes land through pull requests.
- Never open a PR unless the developer explicitly asks for it.
- One concern per change. If the description says "also", split it into another branch/PR.
- Do not commit secrets, keystores, or local-only files (e.g. `.and-code/`). Note: the release
  keystore at `signing/release.keystore` is intentionally public (FOSS-style, password public by
  design); a private `keystore.properties` or CI secrets override it and must never be committed.
- Do not widen the WebView navigation allow-list, and never enable `allowFileAccess`.
- When watching CI/bot feedback on your PRs: poll checks and comments newer than the last push,
  verify each bot finding against the source before "fixing" it, dismiss false positives with a
  written reason, and stop when checks are green on the latest commit.

## Pull Requests

All changes land on `main` through pull requests.

1. Create a branch off `main`: `<type>/<short-description>` (e.g. `feat/export-markdown`, `fix/back-navigation`).
2. Commit there using the format from **Commit Messages**; keep commits atomic.
3. Push the branch and open a PR against `main`.

PR rules:

- One feature/fix per PR — small and focused beats large and thorough.
- Title follows the commit message format: `type(scope): short imperative summary`
  (e.g. `feat(app): add export as markdown`) — it becomes the squash-merge commit message.
- Body stays concise, following the PR template: what changed and why, bullet list of touched areas,
  evidence if applicable, testing checklist (tick before merge).
- UI changes must include clear before/after screenshots; motion/timing changes need a short video.
  Upload evidence directly to GitHub — never commit PR-only screenshots or asset files.
- End the body with an AI attribution line stating exactly which model and agent made the changes,
  in this exact format:

  ```
  Built with {model} in the {agent} harness.
  ```

  Example: `Built with ox-alpha in the OpenCode harness.`

- Do **not** put AI attribution in GitHub Release notes — releases stay clean.
- CI must pass before merging.

## Releases

1. Ensure version metadata is correct (`versionName` / `versionCode` in `app/build.gradle.kts`)
   and add a matching `## [X.Y.Z] - date` section to `CHANGELOG.md`.
2. Tag on `main`: `git tag vX.Y.Z && git push origin vX.Y.Z`.
3. The `release.yml` workflow builds debug + release APKs, signs releases with private secrets when
   configured (otherwise the committed public keystore), extracts the
   matching `CHANGELOG.md` section as release notes, and creates the GitHub Release
   (`generate_release_notes: true`).

## Gotchas

- Builds require network access: the Gradle `downloadAssets` task hooks into every `merge*`/`assemble*`
  task and re-downloads Font Awesome and Inter fonts from CDNs, overwriting the copies committed under
  `assets/www/`. Do not hand-edit files under `assets/www/webfonts/` or `assets/www/fonts/`.
- `index.html` is a single ~1.3k-line file holding all app logic — make surgical edits and keep its
  existing style (plain ES5-style functions, no build step, no bundler).
- Debug builds enable remote WebView inspection (`chrome://inspect`); never move that outside the
  `BuildConfig.DEBUG` guard.
