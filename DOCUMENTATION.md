# PebbleDo Documentation

Technical documentation for developers contributing to or maintaining PebbleDo.

---

## Architecture

PebbleDo uses a **WebView-as-app** pattern where the entire application lives in a single HTML file, wrapped in a thin native Android shell.

```
┌─────────────────────────────────────────────┐
│           Android WebView                           │
│  ┌───────────────────────────────────────┐  │
│  │       index.html (HTML/CSS/JS)               │  │
│  │  - UI rendering                              │  │
│  │  - State management (localStorage)           │  │
│  │  - All app logic                             │  │
│  └───────────────────────────────────────┘  │
│                  ↕                                  │
│  ┌───────────────────────────────────────┐  │
│  │      NativeBridge.kt (Kotlin)                │  │
│  │  - Toast notifications                       │  │
│  │  - Haptic feedback                           │  │
│  │  - Native share sheet                        │  │
│  │  - External URL handling                     │  │
│  │  - Back-button dispatch / app exit           │  │
│  │  - Theme background sync                     │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

### Back-Button Contract

The web app is a single-page app with internal UI state, so every hardware
back press is dispatched to JavaScript as a `backButton` bridge event
(`window.__bridge_backButton`). JS pops one UI level at a time: topmost
dialog → multi-select mode → Archive view → back to Main. When nothing is
left, JS emits `Native.emit("exit")` and the native side exits the app.
The forced onboarding flow consumes back presses without dismissing.

---

## Tech Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language (Native) | Kotlin | 2.0.21 |
| Language (Web) | HTML5, CSS3, JavaScript (ES6+) | - |
| Android SDK | API 26-35 | minSdk 26, targetSdk 35 |
| WebView | Android WebView | - |
| Icons | Font Awesome | 7.0.0 |
| Fonts | Inter (optional) | - |
| Build System | Gradle | 8.12 |
| Build Files | Kotlin DSL | - |
| Android Gradle Plugin | AGP | 8.7.3 |
| Java Runtime | JVM | 17+ |

---

## Project Structure

```
pebbledo/
├── .github/workflows/
│   ├── ci.yml                    # Debug APK build on push/PR
│   └── release.yml               # Tag-triggered release builds & publishing
├── CHANGELOG.md                  # Curated release notes (source for GitHub Releases)
├── app/src/main/
│   ├── assets/www/
│   │   ├── index.html           # Complete web app (~1200 lines)
│   │   ├── bridge.js            # JS-side wrapper for native bridge
│   │   ├── font-awesome.min.css # Font Awesome 7.0.0
│   │   ├── fonts/               # Inter font files (woff2)
│   │   │   ├── inter-400.woff2
│   │   │   ├── inter-500.woff2
│   │   │   └── inter-600.woff2
│   │   └── webfonts/            # Font Awesome webfonts
│   ├── kotlin/com/s17labs/pebbledo/
│   │   ├── MainActivity.kt      # WebView host activity
│   │   └── NativeBridge.kt      # JS ↔ Kotlin bridge
│   ├── res/
│   │   ├── mipmap-*/            # Launcher icons (hdpi to xxxhdpi)
│   │   ├── values/
│   │   │   ├── strings.xml      # App name
│   │   │   ├── themes.xml       # WebShell edge-to-edge theme
│   │   │   └── colors.xml       # Icon background color
│   │   └── play_store_icon.png
│   ├── AndroidManifest.xml       # Permissions & app config
│   ├── build.gradle.kts          # App module build configuration
│   └── proguard-rules.pro       # ProGuard rules for release
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts              # Root build file
├── settings.gradle.kts           # Project settings & plugin management
├── gradle.properties             # Gradle configuration
├── local.properties              # Local SDK path (gitignored)
├── gradlew / gradlew.bat         # Gradle wrapper scripts
├── .gitignore
├── README.md
├── CONTRIBUTING.md
└── LICENSE
```

---

## Native Layer (Kotlin)

### MainActivity.kt

Single Activity hosting the WebView.

**Responsibilities:**
- Configures WebView settings (JavaScript, DOM storage, zoom; file access off)
- Attaches `NativeBridge` as `window.Native` JavaScript interface
- Handles external URL navigation (opens in system browser)
- Dispatches hardware back button to JS (`backButton` event), exits on request
- Syncs WebView background color with the active web theme (`bg` event)
- Implements edge-to-edge display (draws behind system bars)

**Key WebView settings:**
```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    allowFileAccess = false   // file:///android_asset works regardless
    builtInZoomControls = false
    displayZoomControls = false
}
```

### NativeBridge.kt

Provides native Android functionality to JavaScript via `@JavascriptInterface`.

**Methods exposed to JS:**

| Method | Description | Thread |
|--------|-------------|--------|
| `showToast(message)` | Display native toast notifications | Main thread |
| `getDeviceInfo()` | Returns JSON with device model, manufacturer, SDK, app version | Main thread |
| `openUrl(url)` | Opens URL in system browser | Main thread |
| `vibrate(durationMs)` | Haptic feedback vibration (used for all haptics; WebView has no `navigator.vibrate`) | Main thread |
| `share(text)` | Opens native share sheet | Main thread |
| `emit(event, payload)` | Generic event bus from JS to native. Handled events: `"exit"` (leave the app), `"bg"` (`{color}` — sync WebView background to theme) | Main thread |

All UI operations run on the main thread via `runOnUiThread {}`.

### bridge.js

JavaScript-side wrapper for the native bridge with graceful fallbacks for desktop browser development.

**API (same as NativeBridge.kt):**
- `NativeBridge.toast(message)`
- `NativeBridge.getDeviceInfo()`
- `NativeBridge.openUrl(url)`
- `NativeBridge.vibrate(durationMs)`
- `NativeBridge.share(text)`
- `NativeBridge.emit(event, payload)`
- `NativeBridge.on(event, callback)` — register event listeners
  (PebbleDo registers `backButton` — see the Back-Button Contract above)

---

## Web Application (index.html)

The entire web app lives in a single `index.html` file containing:

- **Inline CSS** (~290 lines) — Complete styling with 8 theme definitions using CSS custom properties
- **Inline JavaScript** (~900 lines) — All application logic

### State Management

State is persisted to `localStorage` under three keys plus an onboarding flag:

```javascript
// localStorage keys
"pd_t"   → JSON array of active tasks    [{ id, text }]
"pd_a"   → JSON array of archived tasks  [{ id, text }]
"pd_s"   → JSON settings object:
{
  "theme": "slate",     // slate|sage|rose|sand|ocean|plum|dark|ember
  "font": "sys",        // sys|inter|mono|serif
  "fs": 16,             // font size (px)
  "r": 10,              // corner radius (px)
  "haptics": true,
  "lang": "en"          // en|sk
}
"pd_ob"  → "1" once the onboarding flow has been completed
```

### Key JavaScript Functions

| Function | Purpose |
|----------|---------|
| `load()` / `save()` / `scheduleSave()` | localStorage persistence (validated on load) |
| `render(anim)` / `renderArch(full)` | Diff-render task & archive lists |
| `makeRow(task)` / `makeArchRow(task)` | Build DOM rows with handlers |
| `startEdit(id)` / `commitEdit(id)` | Inline textarea editing |
| `doArchive(id, rowEl)` / `doUnarchive(id, rowEl)` | Animated move between lists |
| `enterSelectMode(firstId)` / `exitSelectMode()` | Multi-select mode |
| `archiveSelected()` / `deleteSelected()` | Bulk operations |
| `initDrag(e, tid)` / `onDM(e)` / `onDE()` | Drag & drop reordering |
| `buildExportText()` | Returns `{ text, count }` for the chosen scope/format |
| `handleBack()` | Pops one UI level; emits `exit` when at root |
| `vibrate(pattern)` / `toast(msg)` | Native-routed haptics & toasts |
| `notifyBg()` | Pushes current theme background to the shell |
| `applyLang()` / `setLang(l)` | Translation system (en/sk) |

### Themes

Defined as CSS custom properties on `:root`:

| Theme | Primary Color | Background |
|-------|---------------|------------|
| Slate | `#6060a0` | `#f8f8fa` |
| Sage | `#5a8a6a` | `#f6f9f6` |
| Rose | `#b06070` | `#faf6f7` |
| Sand | `#a08050` | `#faf8f4` |
| Ocean | `#4070a0` | `#f5f8fa` |
| Plum | `#7a5090` | `#f8f5fa` |
| Dark | `#404050` | `#1a1a2e` |
| Ember | `#a04040` | `#faf5f5` |

---

## Build System

### Gradle Tasks

| Command | Description |
|---------|-------------|
| `./gradlew assembleDebug` | Build debug APK → `app/build/outputs/apk/debug/app-debug.apk` |
| `./gradlew assembleRelease` | Build release APK (minified, ProGuard; signed if `keystore.properties` exists) |
| `./gradlew installDebug` | Install debug APK to connected device/emulator |
| `./gradlew downloadAssets` | Download Font Awesome & Inter fonts |
| `./gradlew tasks` | List all available Gradle tasks |

The `downloadAssets` task is automatically triggered before `merge*` and `assemble*` tasks
(requires network access on first run / after asset changes).

### Release Signing (optional)

If a `keystore.properties` file exists at the repo root:

```properties
storeFile=path/to/release.keystore   # relative to repo root, or absolute
storePassword=…
keyAlias=…
keyPassword=…
```

…a `release` signing config is created and applied automatically. The file is
gitignored. Without it, release APKs build unsigned.

### CI / Release Workflows (.github/workflows)

| Workflow | Trigger | What it does |
|----------|---------|--------------|
| `ci.yml` | push to `main`, PRs, manual | Validates the Gradle wrapper, builds a debug APK, uploads it as an artifact |
| `release.yml` | tag push `v*`, manual | Builds debug + release APKs, signs if signing secrets are configured (`ANDROID_KEYSTORE_B64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`), renames artifacts to `PebbleDo-<version>-*.apk`, generates SHA-256 checksums (`checksums.txt` lets users verify downloads), and publishes a GitHub Release whose body is extracted from the matching `## [version]` section of `CHANGELOG.md` (GitHub's auto-generated commit notes are appended below it) |

To cut a release: `git tag vX.Y.Z && git push origin vX.Y.Z`.

### Release Notes

Release notes are curated in `CHANGELOG.md` (Keep-a-Changelog format). For each
release, the workflow extracts the `## [X.Y.Z]` section matching the pushed tag
and uses it as the release body — so write the notes there when preparing a
version, and the GitHub Release fills itself in.

### Build Configuration

**app/build.gradle.kts:**
- `downloadAssets` task fetches Font Awesome CSS/webfonts and Inter font files
- Build features: BuildConfig enabled
- Release build: minification + resource shrinking + ProGuard

**gradle.properties:**
- JVM args: 2048m
- Build caching enabled
- AndroidX enabled
- Kotlin daemon JVM args configured

**gradle/libs.versions.toml:**
- Version catalog for AGP, Kotlin, and dependencies

---

## Configuration Files

| File | Purpose |
|------|---------|
| `build.gradle.kts` (root) | Top-level build file with plugin declarations |
| `app/build.gradle.kts` | App module config, downloadAssets task, optional signing config, build settings |
| `settings.gradle.kts` | Plugin repositories, dependency resolution, project name |
| `gradle.properties` | JVM args, build caching — portable only; machine-specific overrides (JDK path, aapt2 override) belong in `~/.gradle/gradle.properties` or env vars |
| `local.properties` | Local SDK path (sdk.dir) — not committed |
| `gradle/libs.versions.toml` | Version catalog for AGP, Kotlin, dependencies |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 8.12 distribution |
| `AndroidManifest.xml` | App permissions (INTERNET, VIBRATE), activity config, hardware acceleration |

---

## Permissions

Declared in `AndroidManifest.xml`:
- `android.permission.VIBRATE` — for haptic feedback
- `android.permission.INTERNET` — reserved for future fetch/API use; safe to remove for a strictly offline build

---

## Translation System

PebbleDo supports multiple languages via a simple translation system in `index.html`:

**Supported Languages:**
- English (`en`) — default
- Slovenčina/Slovak (`sk`)

**Adding a new language:**
1. Add language entry to settings panel dropdown
2. Add translation object to `translations` in `index.html`
3. Update `setLanguage()` function to handle the new language code

---

## Development Workflow

### Running in Android Studio
1. Open project in Android Studio (Hedgehog or newer)
2. Connect device or start emulator
3. Click Run or use `./gradlew installDebug`

### Modifying the Web App
1. Edit `app/src/main/assets/www/index.html`
2. Rebuild and install

### Modifying Native Layer
1. Edit Kotlin files in `app/src/main/kotlin/com/s17labs/pebbledo/`
2. Rebuild and install

### Desktop Browser Development
The `bridge.js` file provides fallbacks for all native methods, allowing the web app to run in a desktop browser during development:
- `NativeBridge.toast()` → `console.log()`
- `NativeBridge.vibrate()` → no-op
- `NativeBridge.share()` → `navigator.clipboard.writeText()`
- `NativeBridge.openUrl()` → `window.open()`

---

## Release Build

```bash
./gradlew assembleRelease
```

Release APK location: `app/build/outputs/apk/release/app-release.apk`

Release builds include:
- Code minification (R8/ProGuard)
- Resource shrinking
- Optimized APK size

---

## Troubleshooting

### WebView not loading
- Check that `index.html` exists in `assets/www/`
- Verify WebView has JavaScript enabled

### Native bridge not working
- Ensure `NativeBridge` is attached in `MainActivity.kt`
- Check browser console for `window.Native` availability

### Fonts not loading
- Run `./gradlew downloadAssets` manually
- Check `assets/www/fonts/` and `assets/www/webfonts/` exist

### Build fails
- Verify Java 17+ is installed: `java -version`
- Verify Android SDK path in `local.properties`
- Try `./gradlew clean` then rebuild
