# Stride (Android)

![Stride](store-assets/stride-logo-lockup.png)

A local-first habit tracker built around one idea: **consistency matters more than perfection**.
Every habit gets one grace skip a week that doesn't break its streak — life happens, and the app
accounts for it. This is the native Android rewrite of an HTML/JS prototype; the same data model,
streak math, and feature set are ported over, using Room, Jetpack Compose, and DataStore instead of
localStorage and a JS state object.

No accounts, no cloud, no backend. Everything lives in a local Room database. Backup/restore is a
JSON file you export and import yourself.

## Features

- **Goal types**: complete-once (yes/no), counter (e.g. "8 glasses"), duration (minutes)
- **Flexible scheduling**: every day, weekdays, weekends, specific days, every-N-days, or X times/week
- **Grace skips**: one per habit per week, doesn't break the streak
- **Pause / vacation mode**: pause a habit until a resume date; paused days don't count against it
- **Daily Score** + app-wide momentum streak on the Today tab
- **Calendar**: month grid with per-day status dots, tap a day to see/toggle that day's habits
- **Insights**: weekly consistency across all habits, or a per-habit heatmap + day-of-week breakdown
- **Routines**: group habits (e.g. "Morning") so they show together on Today
- **Notes & mood**: optional, non-blocking capture after completing a habit
- **Real notifications**: per-habit daily reminders via `AlarmManager`, rescheduled after reboot
- **Settings**: language (English/Spanish/French via proper Android string resources), light/dark/system
  theme, 5 accent colors, week-start day, reduce-motion, sound, export/import, reset

## App icon

The launcher icon — three overlapping gestural brush strokes (coral, amber, cream) swept across
a metallic green gradient background — is defined two ways in this repo:
- **Adaptive icon** (used on Android 8.0+, i.e. all supported devices) — vector XML at
  `app/src/main/res/mipmap-anydpi-v26/` + `app/src/main/res/drawable/ic_launcher_{background,foreground}.xml`.
  The metallic look comes from a 6-stop linear gradient (`ic_launcher_background.xml`) with
  alternating light/dark bands to read as brushed metal rather than a flat fill.
- **Legacy PNG fallback** — rendered at all five density buckets (mdpi through xxxhdpi) under
  `app/src/main/res/mipmap-*/ic_launcher.png` (and `_round.png`). Android always prefers the
  adaptive version on API 26+, so these are just a safety net.

No wordmark in the icon itself — Android already labels the app under the icon on the home
screen, so baking the name in twice was redundant (and hurt legibility at real launcher sizes).

`store-assets/ic_launcher_playstore_512.png` is a 512x512 PNG in the format Play Console expects
for the store listing icon when you get there — it isn't bundled into the app itself.
`store-assets/stride-logo-lockup.png` is the icon on a white backdrop, used at the top of this README.

## Requirements

- Android Studio (Koala or newer recommended)
- JDK 17 (Android Studio bundles one)
- Android SDK Platform 34
- minSdk 26 (Android 8.0+)

## Getting the APK from GitHub (no Android Studio needed)

This repo includes a GitHub Actions workflow (`.github/workflows/build.yml`) that automatically
builds a debug APK on every push to `main`/`master`. To install it on your phone:

1. Push this repo to GitHub (or just push a new commit if it's already there).
2. Go to the **Actions** tab on GitHub, open the latest successful "Build debug APK" run.
3. Under **Artifacts**, download `stride-debug-apk` — it's a zip containing `app-debug.apk`.
4. Transfer that `.apk` to your phone (email it to yourself, use a cloud drive, USB, whatever's easiest).
5. On your phone, open the file. Android will ask to allow installs from that source (Settings will
   prompt you the first time) — approve it, then install. This is a debug build, so no Play Store
   signing or review needed; it just installs directly, the same way any sideloaded app does.

If a build fails, the Actions tab will show exactly which Gradle step broke and why — that's the
fastest way to find out if something needs fixing.

## Getting started (building locally in Android Studio)

```bash
git clone <your-fork-url>
cd Stride
./gradlew assembleDebug
```

Or just open the project root in Android Studio and let Gradle sync — it will prompt to download
the Android SDK components it needs if you don't have them yet.

The Gradle wrapper (`gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`) is committed,
so no separate Gradle install is required.

## Important: this has not been compiled

This project was written in an environment without access to the Android SDK or Google's Maven
repository, so **it has not been built or run**. Everything was written carefully and validated by
hand (syntax review, import checks, brace/paren balancing), but Android Studio may surface small
issues on first sync — most likely minor API mismatches if a pinned library version has moved on
by the time you build. If Gradle prompts you to upgrade the Android Gradle Plugin or Gradle itself,
that's expected and safe to accept.

Pinned versions, if you need to adjust anything:
- AGP 8.4.0, Gradle 8.6, Kotlin 1.9.24, KSP 1.9.24-1.0.20
- Compose BOM 2024.06.00, Compose compiler extension 1.5.14
- Room 2.6.1, Navigation-Compose 2.7.7, DataStore 1.1.1

## Architecture

- **`data/`** — Room entities/DAOs/database, domain models (`Habit`, `Completion`, `Routine`, ...),
  mappers between the two, `StatsCalculator` (streaks, grace, schedules, heatmaps — a careful port
  of the prototype's JS logic into `java.time`), and `SettingsRepository` (DataStore).
- **`ui/MainViewModel.kt`** — one shared ViewModel holding all app state (habits, completions, notes,
  routines, settings) as a single combined `StateFlow`, plus every mutation action. This is a
  deliberate simplification versus one ViewModel per screen: it cuts the number of moving parts
  significantly for an app this size. Screens read what they need from the shared state and derive
  presentation values (streaks, rates, due-today lists) directly via `StatsCalculator`.
- **`ui/<feature>/`** — one Composable screen per feature (today, calendar, insights, habits, detail,
  addedit, routine, settings, onboarding), plus `ui/navigation/StrideNavHost.kt` wiring them
  into a `Scaffold` with bottom nav + top bar.
- **`ui/components/`** — shared pieces: the habit row (checkbox or stepper depending on goal type),
  the daily score card with a Canvas-drawn progress ring, the heatmap grid, and the shared dialogs
  (habit menu, note capture, value logging, pause picker).
- **`notifications/`** — `AlarmManager`-based daily reminders (`ReminderScheduler`, `ReminderReceiver`),
  rearmed on boot by `BootReceiver`.
- **`util/BackupManager.kt`** — JSON export/import using `org.json`, validated on import the same way
  the HTML prototype validated its backups.

## Known simplifications vs. the HTML prototype

- **Font**: uses the system font with bold weights instead of bundling Fredoka (no binary font file
  access in the build environment). Swap in `androidx.compose.ui.text.googlefonts` later if you want
  the exact look back.
- **Onboarding**: one welcome screen instead of the original 3-step flow with in-line notification
  permission request. The permission prompt now lives in Settings instead.
- **Routine ordering**: habits within a routine are ordered by selection order; there's no drag-to-reorder yet.
- **Heatmap shading**: counter/duration habits show binary done/not-done coloring, not a gradient by
  fraction-of-target — same simplification as the HTML version.
- **Confetti/haptics/sound**: not carried over in this pass. The data model and streak logic that
  would drive celebratory moments (milestone detection at 7/30/100-day streaks) is all in place in
  `StatsCalculator` — hooking up `MediaPlayer`/`Vibrator`/`Confetti` for it is a good next step.

## License

MIT — see `LICENSE`.
