# PebbleDo

> A quiet little to-do list built for focus.

**PebbleDo** is a minimal, distraction-free task manager for Android. No accounts, no syncing — just you and your tasks.

<p>
  <img src="https://img.shields.io/badge/Version-1.0.0-6060a0?style=flat-square" alt="Version">
  <img src="https://img.shields.io/badge/License-MIT-6060a0?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/minSDK-26-6060a0?style=flat-square" alt="minSDK">
  <img src="https://img.shields.io/badge/targetSDK-35-6060a0?style=flat-square" alt="targetSDK">
</p>

---

## Features

### Task Management
- **Add, edit, and complete tasks** with a single tap
- **Archive** completed tasks for later reference
- **Drag to reorder** — grab the grip handle and move tasks freely
- **Multi-select** — long-press any task to select multiple, then bulk archive or delete

### Appearance
- **8 themes** — Slate, Sage, Rose, Sand, Ocean, Plum, Dark, and Ember
- **4 font choices** — System, Inter, Mono, and Serif
- **Adjustable font size** — slide between 12px and 20px
- **Adjustable corner radius** — tune the UI roundness to your taste

### Extras
- **Export tasks** — copy your list as plain text or Markdown (active, archived, or both)
- **Haptic feedback** — subtle vibrations on key interactions, toggleable in settings
- **Onboarding flow** — a brief walkthrough on first launch, with a "don't show again" option
- **Persistent storage** — everything saved locally, works fully offline
- **Smooth animations** — satisfying transitions for completing, archiving, and reordering tasks

---

## Screenshots

| Main View | Archive | Settings |
|-----------|---------|----------|
| Clean task list with drag handles <img width="189" height="420" alt="main-view" src="https://github.com/user-attachments/assets/29339357-9fe1-4308-8ab5-41bfdaff733a" /> | Archived completed tasks <img width="189" height="420" alt="archive-view" src="https://github.com/user-attachments/assets/f99c1b65-5077-4d5a-bfa9-599fc81fb64e" /> | Themes, fonts, and preferences <img width="189" height="420" alt="settings" src="https://github.com/user-attachments/assets/d143e10b-c56e-414e-bab5-82416ba6b5d4" /> |

---

## Requirements

| Item | Version |
|------|---------|
| Java | 17+ |
| Android minSdk | 26 (Android 8.0) |
| Android targetSdk | 35 |
| Android Studio | Hedgehog or newer |

---

## Building

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Project Structure

```
app/src/main/
├── assets/www/
│   └── index.html          # The entire PebbleDo web app
├── java/.../
│   └── MainActivity.kt     # WebView wrapper + native bridge
└── res/
    └── ...                 # Android resources
```

The app is a single HTML file wrapped in an Android WebView, with a thin native bridge for opening external links.

---

## License

[MIT License](LICENSE)

