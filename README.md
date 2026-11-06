# Student Planner (વિદ્યાર્થી પ્લાનર)

A study planner for Android and the web, in Gujarati and English.

[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE) ![Languages: Gujarati + English](https://img.shields.io/badge/languages-Gujarati%20%2B%20English-blue)

**Live demo (phone and PC, no install):** https://1darshankumarsavsani-blip.github.io/Student-Planner/

## Features

- Exam countdown, syllabus checklist, homework with progress rings
- Study timer, notes and break time
- Weekly timetable with "now" and "up next"
- Attendance tracker with target % and classes you can miss or need
- Marks and CGPA calculator
- Appearance: 8 theme colours, dark mode, 4 text sizes, bold, high contrast
- Google sync between phone and PC

## Download

Latest release (V3.1): the Android APK and the Windows launcher on the [Releases page](../../releases/latest).

## Tech stack

- Android: pure Java, offline-first
- Web: single-file HTML/CSS/JavaScript
- Sync: Firebase Authentication (Google) and the Firestore REST API

## Project structure

```text
app/   Android app (Java) and unit tests
docs/  website (single index.html, GitHub Pages)
release/  release files
.github/  issue templates and release workflow
```

## Build from source

Open the `app/` project in Android Studio, or run:

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleRelease     # build the APK (sign it with your own keystore)
```

Requirements: JDK 17, Android SDK 34. The website needs no build: open `docs/index.html` in a browser.

## Privacy

Data stays on your device unless you turn on Google sync, which stores it in your own Google account's document.

## Honest notes

Tested on a simulated Android 14 phone and in a headless browser. The newer Android screens and Google sign-in still need confirming on a real phone.

## Contributing

Bug reports and ideas are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) and the issue templates. Please report security problems as described in [SECURITY.md](SECURITY.md).

## License

MIT, see [LICENSE](LICENSE).

## Author

Darshankumar Savsani, DARSHAN CORPORATION, Rajkot. GitHub: [1darshankumarsavsani-blip](https://github.com/1darshankumarsavsani-blip), organization: [DARSHAN-CROP](https://github.com/DARSHAN-CROP).
