# Student Planner (વિદ્યાર્થી પ્લાનર)

Offline Android app in Gujarati and English by Darshankumar Savsani (DARSHAN CORPORATION).

Version 1: exam countdown, syllabus checklist with progress, 25/5 study timer, notes. Data stays on the phone.

Install: download `release/Student_Planner_V1.apk` on an Android phone and allow "install unknown apps".
Source code: `Student_Planner_source.zip` (open in Android Studio, or build with Gradle).

Honest note: tested on a simulated Android 14 phone, not yet on a real phone. The timer stops if the app is closed.

## Version 2.0

- Web app (works on phone and PC, no install): https://1darshankumarsavsani-blip.github.io/Student-Planner/
- `release/Student_Planner_Windows.exe`: opens the web app in its own window (Microsoft Edge or Chrome needed).
- New: Homework with a red warning when less than 24 hours are left, and a break-time setting.
- Google sign-in and online sync: the code is ready, it turns on after a Firebase project is configured in `docs/firebase-config.js`.
- Not yet: the homework rings, break notification and campus location in the Android app (planned).

## Android V2 (release v2.0)

- Homework list. When less than 24 hours are left the phone rings for 1 minute at :00, :20 and :40 of every hour (3 times per hour). Quiet hours (default 22:00-07:00) and break time are changeable in Settings.
- Break notification once a day at break time, only if you are at the campus saved with the "This is the campus" button.
- Settings has a test ring and permission buttons. Allow alarms, notifications and Location "all the time".
- Honest note: tested on a simulated Android 14 phone, not yet on a real phone. Battery saver can delay rings.
