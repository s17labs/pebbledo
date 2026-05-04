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
- **Multiline tasks** supports multiple lines

### Archive Management
- **View archived and restore tasks** back to your active list
- **Delete individual tasks** or clear all

### Appearance
- **8 themes** — Slate, Sage, Rose, Sand, Ocean, Plum, Dark, and Ember
- **4 font choices** — System, Inter, Mono, and Serif
- **Adjustable font size and corner radius** — tune the UI font and roundness to your liking

### Export & Sharing
- **Export tasks** — copy your list as plain text or Markdown
- **Choose scope** — export active tasks, archived tasks, or everything

### Extras
- **Multi-language support** — English and Slovenčina (Slovak)
- **Haptic feedback** — subtle vibrations on key interactions, toggleable in settings
- **Onboarding flow** — 5-page walkthrough on first launch
- **Persistent storage** — everything saved locally, works fully offline

---

## Screenshots

| Main View | Archive | Settings |
|-----------|---------|----------|
| <img width="189" height="420" alt="main-view" src="https://github.com/user-attachments/assets/29339357-9fe1-4308-8ab5-41bfdaff733a" /> | <img width="189" height="420" alt="archive-view" src="https://github.com/user-attachments/assets/f99c1b65-5077-4d5a-bfa9-599fc81fb64e" /> | <img width="189" height="420" alt="settings" src="https://github.com/user-attachments/assets/d143e10b-c56e-414e-bab5-82416ba6b5d4" /> |

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0.21 |
| Android SDK | API 26-35 |
| Web App | HTML5, CSS3, JavaScript |
| Build System | Gradle 8.12 |
| Icons | Font Awesome 7.0.0 |

For detailed technical documentation, see [DOCUMENTATION.md](DOCUMENTATION.md).

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

## Contributing

Contributions are welcome! Check out the [Contributing Guide](CONTRIBUTING.md) to get started.

---

## License

[MIT License](LICENSE)

