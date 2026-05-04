# PebbleDo Documentation

Technical documentation for developers contributing to or maintaining PebbleDo.

---

## Architecture

PebbleDo uses a **WebView-as-app** pattern where the entire application lives in a single HTML file, wrapped in a thin native Android shell.

```
┌─────────────────────────────────────────────┐
│           Android WebView                   │
│  ┌───────────────────────────────────────┐  │
│  │       index.html (HTML/CSS/JS)        │  │
│  │  - UI rendering                       │  │
│  │  - State management (localStorage)    │  │
│  │  - All app logic                      │  │
│  └───────────────────────────────────────┘  │
│                  ↕                          │
│  ┌───────────────────────────────────────┐  │
│  │      NativeBridge.kt (Kotlin)         │  │
│  │  - Toast notifications                │  │
│  │  - Haptic feedback                    │  │
│  │  - Native share sheet                 │  │
│  │  - External URL handling              │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

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
├── app/src/main/
│   ├── assets/www/
│   │   ├── index.html           # Complete web app (~1186 lines)
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
- Configures WebView settings (JavaScript, DOM storage, file access, zoom)
- Attaches `NativeBridge` as `window.Native` JavaScript interface
- Handles external URL navigation (opens in system browser)
- Manages hardware back button
- Implements edge-to-edge display (draws behind system bars)

**Key WebView settings:**
```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    setAllowFileAccess(true)
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
| `vibrate(durationMs)` | Haptic feedback vibration | Main thread |
| `share(text)` | Opens native share sheet | Main thread |
| `emit(event, payload)` | Generic event bus from JS to native | Main thread |

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

---

## Web Application (index.html)

The entire web app lives in a single `index.html` file containing:

- **Inline CSS** (~290 lines) — Complete styling with 8 theme definitions using CSS custom properties
- **Inline JavaScript** (~900 lines) — All application logic

### State Management

```javascript
// Persisted to localStorage
{
  "todos": [...],        // Active tasks
  "archived": [...],     // Archived tasks
  "settings": {
    "theme": "slate",
    "font": "system",
    "fontSize": 16,
    "cornerRadius": 16,
    "haptics": true,
    "language": "en",
    "onboardingComplete": true
  }
}
```

### Key JavaScript Functions

| Function | Purpose |
|----------|---------|
| `loadState()` | Load tasks and settings from localStorage |
| `saveState()` | Persist current state to localStorage |
| `addTodo(text)` | Create new task |
| `toggleTodo(index)` | Toggle task completion |
| `archiveTodo(index)` | Move task to archive |
| `deleteTodo(index)` | Delete task from active list |
| `unarchiveTodo(index)` | Restore task from archive |
| `reorderTodos(from, to)` | Reorder tasks via drag-and-drop |
| `enterMultiSelect(index)` | Enter multi-select mode |
| `toggleMultiSelect(index)` | Toggle task selection |
| `bulkArchive()` | Archive all selected tasks |
| `bulkDelete()` | Delete all selected tasks |
| `exportTodos(format, scope)` | Export as text or markdown |
| `showOnboarding()` | Display onboarding flow |

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
| `./gradlew assembleRelease` | Build release APK (minified, ProGuard) |
| `./gradlew installDebug` | Install debug APK to connected device/emulator |
| `./gradlew downloadAssets` | Download Font Awesome & Inter fonts |
| `./gradlew tasks` | List all available Gradle tasks |

The `downloadAssets` task is automatically triggered before `merge*` and `assemble*` tasks.

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
| `app/build.gradle.kts` | App module config, downloadAssets task, build settings |
| `settings.gradle.kts` | Plugin repositories, dependency resolution, project name |
| `gradle.properties` | JVM args, build caching, AndroidX, Kotlin settings |
| `local.properties` | Local SDK path (sdk.dir) — not committed |
| `gradle/libs.versions.toml` | Version catalog for AGP, Kotlin, dependencies |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 8.12 distribution |
| `AndroidManifest.xml` | App permissions (VIBRATE), activity config, hardware acceleration |

---

## Permissions

Declared in `AndroidManifest.xml`:
- `android.permission.VIBRATE` — for haptic feedback

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
