# Student Planner (વિદ્યાર્થી પ્લાનર)

Offline Android app in Gujarati and English by Darshankumar Savsani (DARSHAN CORPORATION).

Version 1: exam countdown, syllabus checklist with progress, 25/5 study timer, notes. Data stays on the phone.

Install: download `release/Student_Planner_V1.apk` on an Android phone and allow "install unknown apps".
Source code: `Student_Planner_source.zip` (open in Android Studio, or build with Gradle).

Honest note: tested on a simulated Android 14 phone, not yet on a real phone. The timer stops if the app is closed.

## Version 2 preview: web app + Windows

- Web app (works on phone and PC, no install): https://1darshankumarsavsani-blip.github.io/Student-Planner/
- `release/Student_Planner_Windows.exe`: opens the web app in its own window (Microsoft Edge or Chrome needed).
- New: Homework with a red warning when less than 24 hours are left, and a break-time setting.
- Google sign-in and online sync: the code is ready, it turns on after a Firebase project is configured in `docs/firebase-config.js`.
- Not yet: the homework rings, break notification and campus location in the Android app (planned).
