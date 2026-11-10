# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Built so far (MVVM, one package per layer):
- `domain/DominoRules` — pure winner rule (unit-tested in `app/src/test`).
- `data/local` — Room: `matches`, `participants`, `hands`, `hand_scores`; `MatchDao` (instrumented tests in `app/src/androidTest`).
- `data/repository/MatchRepository` — single entry point for the UI; every write that changes totals recomputes the winner (instrumented tests).
- `ui/newmatch` — "Nueva partida" screen + ViewModel; shows a "Continuar" card when a match is in progress.
- `ui/scoreboard` — scoreboard, add/edit/delete hands in a bottom sheet, winner banner. Abandoned matches are read-only (the repository rejects changes to them).
- `ui/history` — every match (`MatchRepository.observeMatchSummaries()`): in progress first, then most recently finished; delete with confirmation.
- `ui/DominoNavHost` — routes `new_match`, `history` and `scoreboard/{matchId}` (ViewModels read `matchId` via `SavedStateHandle`).
- ViewModels get the repository from `DominoApplication` through a `viewModelFactory` (no DI library).

Not built yet: ViewModel unit tests (behaviour is covered by the instrumented screen tests).

## Testing

- Instrumented tests (`app/src/androidTest`) need the emulator (`Pixel_3a_API_34`). UI tests use the Compose testing APIs; there is no Robolectric, so `./gradlew test` only runs `DominoRules` tests.
- `DominoTestRunner` (set as `testInstrumentationRunner`) swaps `DominoApplication` for `TestDominoApplication`, which uses an **in-memory** Room database: tests never touch matches saved on the device. Keep `DominoApplication.database` `open` for this.
- `AppNavigationTest` wipes the in-memory database before each test with an `ExternalResource` rule (`order = 0`, before the activity launches): the database lives as long as the test process, so data would otherwise leak between tests.
- Screen tests (`ui/newmatch/NewMatchScreenTest`, `ui/scoreboard/ScoreboardScreenTest`, `ui/history/HistoryScreenTest`) build the real ViewModel with an in-memory repository and pass it to the screen; `ui/AppNavigationTest` drives `MainActivity` end to end (navigation, back, rotation via `scenario.recreate()`).
- Every new screen gets a `*ScreenTest`; every bug found gets a regression test.
- Helpers in `ui/ComposeTestHelpers.kt`: `str(R.string.x)` (never hardcode UI text), `field(label)`, and `waitForText` / `waitForNoText` / `waitForDescription`. Room writes run off the Compose test clock, so always wait for DB-driven UI; on timeout they print the whole semantics tree.
- Gotchas: during navigation both screens are on screen at once (wait for something only the new screen has); with the keyboard open the pinned bottom bar covers content, so `performScrollTo()` before clicking; repeated buttons need a distinct `contentDescription` (e.g. "Anotar puntos para X").
- Run only UI tests: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.error404.dominoapp.ui`

## Build & test commands

Run from the repo root. On Windows use `.\gradlew.bat` (PowerShell) or `./gradlew` (Git Bash).

The default `java` on this machine is JDK 22, which Gradle 8.6 cannot run on ("Unsupported class file major version 66"). Point Gradle at Android Studio's bundled JDK 17 first:
`export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (Git Bash) or `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` (PowerShell).

```
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # install on connected device/emulator
./gradlew test                          # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest          # instrumented tests (app/src/androidTest), needs device/emulator
./gradlew lint                          # Android lint

# single unit test class / method
./gradlew :app:testDebugUnitTest --tests "com.error404.dominoapp.ExampleUnitTest"
./gradlew :app:testDebugUnitTest --tests "com.error404.dominoapp.ExampleUnitTest.addition_isCorrect"

# single instrumented test class
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.error404.dominoapp.ExampleInstrumentedTest
```

## Architecture & configuration

- Single module `:app`, package/namespace `com.error404.dominoapp`. Entry point is `MainActivity` (a `ComponentActivity` using `enableEdgeToEdge()` + `setContent { DominoAppTheme { ... } }`).
- UI is Jetpack Compose + Material 3 only (no XML layouts). Theme lives in `ui/theme/`: custom "Caribbean" palette with light and dark schemes (primary = coral for actions, secondary = turquoise for the brand top bar, tertiary = sun yellow reserved for the winner), dynamic color intentionally off. Every screen's `TopAppBar` uses `dominoTopAppBarColors()`; status bar icons are forced light in `MainActivity`, so every screen needs that turquoise top bar. Display/headline styles use tabular digits (`tnum`) for scores.
- Dependencies are managed through the version catalog `gradle/libs.versions.toml`; build scripts are Groovy (`build.gradle`, not `.kts`). Add new libraries to the catalog and reference them as `libs.<alias>`.
- Toolchain pins that constrain upgrades:
  - AGP 8.4.1, Gradle 8.6, Kotlin 1.9.0, Compose compiler extension `1.5.1` (must stay matched to the Kotlin version — Kotlin 2.x requires switching to the `org.jetbrains.kotlin.plugin.compose` plugin instead of `composeOptions`).
  - `core-ktx` is held at 1.13.1 and `activity-compose` at 1.9.3 on purpose: 1.16.0 / 1.10.x require `compileSdk 35` and AGP 8.6+ and break the build. Before bumping any AndroidX library, check its minimum compileSdk/AGP. Upgrading AGP also requires a newer Gradle wrapper.
  - Compose BOM is `2024.02.00` (Compose 1.6.x, Material 3 1.2). It must stay at least as new as the Compose version that `navigation-compose`/`lifecycle` pull in transitively: a mismatch compiles fine but crashes at runtime with `NoSuchMethodError` (seen with BOM 2023.08 + navigation 2.7.7). Check with `./gradlew :app:dependencies --configuration debugRuntimeClasspath`. Newer BOMs may require a newer Kotlin/Compose compiler.
  - `compileSdk`/`minSdk`/`targetSdk` are all 34, so no API-level guards below 34 are needed.
  - Java/JVM target is 1.8.

## Agent skills

`.claude/skills/` contains Android-specific skills (Compose, Material 3, Room, Navigation 3, coroutines/Flow, testing setup, edge-to-edge, AGP 9 upgrade, etc.), tracked in `skills-lock.json`. Prefer invoking the relevant skill when doing that kind of work.


# Domino app

## What it does
- Tracks the points of each team or player during a domino match.
- The user sets the target score when starting; when it is reached, the app declares the winner.
- Saves the match history on the device.

## Stack
- Kotlin, Jetpack Compose, Material 3.
- Room (on top of SQLite) for the local database. No backend, no internet.
- MVVM architecture: Screen (Compose) -> ViewModel -> Repository -> Room DAO.

## Rules
- I'm a beginner: explain every important decision in Spanish before writing the code.
- Always use the Material 3 theme's colors and typography, never hardcoded values.
- Touch targets of at least 48dp, and large scoreboard text.
- Build with ./gradlew assembleDebug after every change.
- All code in English: classes, functions, variables, Room tables/columns and comments. Only user-facing text lives in `strings.xml`, translated per language.

## Translations

- The app follows the device language: `values/` is **English** (fallback for any untranslated language), plus `values-es/`, `values-pt/` and `values-fr/`. Every new string must be added to all four files; `./gradlew lint` reports missing ones (`MissingTranslation`).
- Text with a count uses `<plurals>` + `pluralStringResource` (tests: `plural(R.plurals.x, n)`), never `"%d points"` in a plain string.
- `app_name` ("Capicúa") is `translatable="false"` and only lives in `values/`.
- French uses ` ` (non-breaking space) before `: ? !` and inside `« »`.
- `res/xml/locales_config.xml` (referenced from the manifest) lets users pick the app language in system Settings; add new languages there too.
- Instrumented tests pass in any emulator language because they read text through `str()` / `plural()`.
