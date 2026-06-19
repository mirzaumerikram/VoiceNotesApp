# VoiceNotes — Android Application
## Group Project Report | Week 14: Audio Management & Multithreading

**Group:** 5
**Members:** Mirza Umer Ikram, Ruhma Bilal
**Course:** Mobile Application Development
**University:** University of Central Punjab (UCP), Lahore
**Presentation Date:** 9 July 2026

---

## 1. Introduction

VoiceNotes is a fully functional Android application built in Kotlin that allows users to record,
organise, and replay voice recordings from their smartphone. The app provides a clean, professional
interface with a dark navy and gold colour scheme, and implements all mandatory Android course
requirements alongside the Week 14 specific technologies: **Audio Management** and **Multithreading**.

The application is structured around six screens (Activities), a SQLite database for persistent
storage, a custom RecyclerView adapter, two dedicated threading classes, and a complete user
authentication flow with input validation.

---

## 2. Objectives

The primary objectives of this project are:

1. Demonstrate Audio Management using `MediaRecorder` (recording) and `MediaPlayer` (playback).
2. Implement Multithreading using the `Thread` class and the `Runnable` interface for background
   audio I/O and live waveform animation.
3. Build a complete Android application satisfying all Part A mandatory requirements:
   - Splash, Login, Registration, and Home/Dashboard screens
   - CRUD operations backed by an SQLite database
   - RecyclerView with a custom adapter
   - Input validation with AlertDialog and Toast feedback
   - Options menu for sorting and navigation
4. Write clean, well-commented, maintainable Kotlin code with a proper package structure.

---

## 3. System Design

### 3.1 Package Structure

```
com.example.voicenotes/
├── activities/         — All 6 Activity classes
│   ├── SplashActivity.kt
│   ├── LoginActivity.kt
│   ├── RegisterActivity.kt
│   ├── HomeActivity.kt
│   ├── RecordActivity.kt
│   └── PlaybackActivity.kt
├── adapters/
│   └── NotesAdapter.kt         (RecyclerView adapter)
├── db/
│   └── DatabaseHelper.kt       (SQLiteOpenHelper, full CRUD)
├── models/
│   └── VoiceNote.kt            (data class)
├── threads/
│   ├── RecordingThread.kt      (Thread subclass)
│   └── WaveformRunnable.kt     (Runnable implementation)
└── utils/
    └── SessionManager.kt       (SharedPreferences auth)
```

### 3.2 Database Schema

The app uses a single SQLite table `voice_notes`:

| Column      | Type    | Constraints          | Description                  |
|-------------|---------|----------------------|------------------------------|
| id          | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique note identifier  |
| title       | TEXT    | NOT NULL             | Display name (user-editable) |
| file_path   | TEXT    | NOT NULL             | Absolute path to .m4a file   |
| duration    | INTEGER | DEFAULT 0            | Recording length in seconds  |
| timestamp   | INTEGER | NOT NULL             | Unix epoch (ms) of creation  |

### 3.3 CRUD Operations

| Operation | Screen           | Method                          |
|-----------|------------------|---------------------------------|
| Create    | RecordActivity   | `DatabaseHelper.insertNote()`   |
| Read      | HomeActivity     | `DatabaseHelper.getAllNotes()`  |
| Update    | HomeActivity, PlaybackActivity | `DatabaseHelper.updateTitle()` |
| Delete    | HomeActivity     | `DatabaseHelper.deleteNote()`   |

### 3.4 Navigation Flow

```
SplashActivity
    ├── (logged in)  → HomeActivity
    └── (new user)   → LoginActivity
                          ├── (valid)  → HomeActivity
                          └── (new)    → RegisterActivity → HomeActivity
HomeActivity
    ├── FAB tap      → RecordActivity → HomeActivity (on save)
    └── Card tap     → PlaybackActivity
```

---

## 4. Technologies Used

| Technology             | Purpose                                              |
|------------------------|------------------------------------------------------|
| Kotlin                 | Primary development language                         |
| Android SDK 34         | Target platform (Android 14)                         |
| View Binding           | Type-safe layout access                              |
| MediaRecorder          | Audio recording from device microphone               |
| MediaPlayer            | Audio playback with seek support                     |
| Thread (subclass)      | RecordingThread — off-main-thread recording I/O      |
| Runnable (interface)   | WaveformRunnable — live amplitude polling            |
| Handler + Looper       | Post UI updates from background threads to main thread |
| SQLite / SQLiteOpenHelper | Persistent local storage for voice notes          |
| RecyclerView           | Scrollable, efficient list of recordings             |
| SharedPreferences      | User session and account storage                     |
| AlertDialog            | Input validation, delete confirmation, rename        |
| Toast                  | Success and error feedback                           |
| AnimationUtils         | Splash fade-in, recording pulse animation            |

---

## 5. Features

### Part A — Mandatory Features (All Groups)

| Feature | Implementation |
|---------|---------------|
| Attractive UI | Dark navy `#0D1B2A` + gold `#F5A623` theme, card-based design |
| Splash Screen | SAFORA-style mic logo with fade-in animation, auto-navigates after 2s |
| Login Screen | Email + password with AlertDialog validation (empty fields, invalid email format) |
| Registration Screen | Full name, email, password, confirm password with 5-point validation |
| Home/Dashboard | RecyclerView list of all recordings with count and FAB |
| ≥3 Activities | 6 activities: Splash, Login, Register, Home, Record, Playback |
| Navigation via Intents | All screen transitions use explicit `Intent` |
| CRUD | Create (record), Read (list + playback), Update (rename), Delete (long-press confirm) |
| RecyclerView | Custom `NotesAdapter` with card items, click + long-click listeners |
| Input Validation | AlertDialog for: empty fields, invalid email, short password, name empty on rename |
| Toast Messages | "Recording saved!", "Renamed successfully.", "Recording deleted.", "Title saved." |
| AlertDialogs | Validation, delete confirmation, rename dialog, sort confirmation, about, logout |
| Options Menu | Sort by Date, Sort by Name, About, Logout — all implemented in `menu_home.xml` |

### Part B — Week 14 Specific Features

#### Audio Recording (`MediaRecorder`)
- `RecordingThread` configures `MediaRecorder` with:
  - Audio source: `MIC`
  - Output format: `MPEG_4`
  - Audio encoder: `AAC` at 128 kbps, 44.1 kHz
- Files saved as `.m4a` to `getFilesDir()/recordings/VN_yyyyMMdd_HHmmss.m4a`
- Runtime `RECORD_AUDIO` permission requested before first recording

#### Audio Playback (`MediaPlayer`)
- `PlaybackActivity` uses `MediaPlayer` with:
  - `setDataSource()` → `prepare()` → `start()` / `pause()` / `seekTo()`
  - SeekBar updated every 500ms via `Handler`
  - Completion listener resets UI to stopped state
- Users can edit the recording title inline and save it (updates SQLite)

#### Save/Load Audio Files
- Recordings saved to app's private files directory (`getFilesDir()/recordings/`)
- On `HomeActivity.onResume()`, all notes are loaded from SQLite (which stores file paths)
- File existence checked before playback; graceful error Toast shown if missing

#### Thread Class (`RecordingThread extends Thread`)
- `RecordingThread` extends `Thread` and is named `"RecordingThread"` for Logcat visibility
- `run()` calls `MediaRecorder.prepare()` and `start()` — keeping I/O off the main thread
- `stopRecording()` calls `stop()` and `release()` safely

#### Runnable Interface (`WaveformRunnable implements Runnable`)
- `WaveformRunnable` implements `Runnable`
- `run()` loops every 100ms, calling `getMaxAmplitude()` on the `RecordingThread`
- Normalises amplitude (0–32767) to 0–100 and posts to main thread via `Handler`
- Drives 12 animated waveform bars in `RecordActivity`

#### Multiple Threads
- `RecordingThread` ("RecordingThread") and the waveform thread (`Thread(waveformRunnable, "WaveformThread")`) run **concurrently** during a recording session
- File I/O operations (delete, rename, DB updates) run on a third `Thread("FileIOThread")`
- This satisfies the **3 concurrent threads** demonstration

#### Background Processing
- `FileIOThread` performs all file system operations (delete `.m4a` file, rename via DB) off the main UI thread
- `PlaybackActivity` pre-loads `MediaPlayer` (`setDataSource` + `prepare`) on `FileIOThread` to avoid UI jank

---

## 6. Screenshots

*(Insert screenshots here when compiling the final PDF)*

1. **Splash Screen** — animated mic logo, "VoiceNotes / Record. Organise. Replay."
2. **Login Screen** — dark theme, email/password fields, demo hint
3. **Registration Screen** — full name, email, password, confirm password
4. **Home Screen (empty)** — empty-state graphic with FAB
5. **Home Screen (with recordings)** — RecyclerView list of voice note cards
6. **Record Screen** — large mic button, timer, live waveform bars during recording
7. **Playback Screen** — SeekBar, play/pause/stop controls, editable title
8. **Validation AlertDialog** — "Missing Information" on Login or Registration
9. **Delete Confirmation** — long-press on recording card → "Delete Recording?"
10. **Options Menu** — Sort by Date / Sort by Name / About / Logout

---

## 7. Challenges Faced

1. **MediaRecorder on Background Thread**: Android's `MediaRecorder` must not run `prepare()` on the main thread as it can block I/O. Wrapping it in `RecordingThread` (a `Thread` subclass) solved this, while still allowing UI callbacks via `runOnUiThread`.

2. **Thread Safety for Amplitude Polling**: `WaveformRunnable` calls `getMaxAmplitude()` on a `MediaRecorder` instance owned by `RecordingThread`. Using `@Volatile` on `isRecording` and a null-safe wrapper around `getMaxAmplitude()` prevents race conditions.

3. **MediaPlayer Pre-loading**: Calling `prepare()` on the main thread caused a brief freeze when opening large recordings. Moving it to `FileIOThread` with a `runOnUiThread` callback once ready resolved this.

4. **Audio Permission on Android 14**: `targetSdk 34` requires `RECORD_AUDIO` permission at runtime. `RecordActivity` checks and requests this permission before starting the `RecordingThread`.

5. **File Path Persistence**: SQLite stores the absolute path to each `.m4a` file. Since recordings are in `getFilesDir()` (app-private, not moved on update), paths remain valid across app restarts.

---

## 8. Conclusion

VoiceNotes successfully demonstrates all Week 14 requirements — audio recording with `MediaRecorder`,
audio playback with `MediaPlayer`, saving and loading audio files, the `Thread` class, the `Runnable`
interface, background processing, and multiple concurrent threads — within a professionally designed,
fully functional Android application. All Part A mandatory requirements are also satisfied, including
a complete authentication flow, CRUD operations backed by SQLite, RecyclerView with a custom adapter,
input validation with AlertDialogs, Toast feedback, and an options menu.

The app is ready for live demonstration and runs without crashes on both the emulator and physical
devices running Android 7.0 (API 24) and above.
