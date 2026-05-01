# Contributing to PebbleDo

Thanks for your interest in contributing! PebbleDo is a small project, so every bit helps.

## Ways to contribute

- **Report bugs** — open an [issue](https://github.com/s17labs/pebbledo/issues) with a description and steps to reproduce
- **Suggest features** — share your ideas in the [discussions](https://github.com/s17labs/pebbledo/discussions) or an issue
- **Submit pull requests** — fix bugs, improve the UI, or add small features
- **Improve docs** — typos, clarifications, or better examples are always welcome

## Getting started

### 1. Fork & clone

```bash
git clone https://github.com/<your-username>/pebbledo.git
cd pebbledo
```

### 2. Open in Android Studio

Open the project root in Android Studio. It will sync Gradle and download dependencies automatically.

### 3. Run the app

Connect a device or start an emulator, then press **Run** or use:

```bash
./gradlew installDebug
```

### 4. Build the APK

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

```
app/src/main/
├── assets/www/
│   └── index.html          # The entire PebbleDo web app (UI + logic)
├── kotlin/com/s17labs/pebbledo/
│   ├── MainActivity.kt     # Android activity hosting the WebView
│   └── NativeBridge.kt     # JS ↔ Kotlin bridge for external links
└── res/
    └── ...                 # Android resources (icons, splash, etc.)
```

The core of the app lives in a single `index.html` file. The Kotlin side is a thin wrapper that provides a WebView and a native bridge for opening URLs outside the app.

## Guidelines

### Code style
- **Web side** (`index.html`): keep CSS and JS inline — the project intentionally ships as one file. Follow the existing compact style.
- **Kotlin side**: follow standard Kotlin/Android conventions. Keep the native layer minimal.

### Commits
- Use [Conventional Commits](https://www.conventionalcommits.org/) for consistency:
  - `fix:` for bug fixes
  - `feat:` for new features
  - `chore:` for maintenance, refactors, or tooling
  - `docs:` for documentation changes
- Keep commits focused — one logical change per commit.

### Pull requests
- Describe **what** you changed and **why**
- Link any related issues
- Keep the scope small when possible — it's easier to review and merge
- Make sure the app builds and runs without crashes

## What to avoid

- Adding external dependencies unless absolutely necessary
- Breaking the offline-first, no-account philosophy
- Large UI overhauls without discussing them first

---

Not sure where to start? Open an issue and we'll figure it out together.
