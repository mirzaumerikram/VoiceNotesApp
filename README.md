# VoiceNotes App 🎙️

VoiceNotes is a native Android application built in Kotlin that allows users to easily record, organize, and replay voice recordings. 

This project was developed for a Mobile Application Development course at the University of Central Punjab, Lahore.

## 🚀 Features
* **Audio Recording:** High-quality voice recording using `MediaRecorder`.
* **Dynamic Waveform:** Real-time visual waveform animation during recording.
* **Audio Playback:** Built-in playback functionality with a seek bar and play/pause/stop controls using `MediaPlayer`.
* **Organization:** Rename and delete your recordings with ease.
* **Local Storage:** All recordings and their metadata (title, duration, date) are saved locally using an **SQLite Database**.
* **Authentication:** Simple Login and Registration screens with session management.

## 🧵 Technical Highlights
This application was specifically designed to demonstrate proficiency in handling background tasks and multithreading in Android:
* **Background Processing:** Audio File I/O (saving, deleting, renaming) is handled entirely off the main UI thread.
* **Multithreading:** Recording tasks and live amplitude polling (for the waveform animation) run concurrently on separate threads to ensure a smooth, lag-free user experience.

## 🛠️ Built With
* **Language:** Kotlin
* **Framework:** Android SDK
* **Database:** SQLite
* **UI:** XML Layouts, Material Design

## 👥 Authors
* **Group 5:** Mirza Umer Ikram & Ruhma Bilal
* **University:** University of Central Punjab (UCP), Lahore

## 📱 Screenshots
*(Screenshots can be added here showing the Dashboard, Recording screen, and Playback screen)*
