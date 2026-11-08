# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

`dominoApp` is a freshly generated Android Studio "Empty Compose Activity" project. No domino logic exists yet: `MainActivity.kt` still renders the template `Greeting` composable, and the tests are the generated `ExampleUnitTest` / `ExampleInstrumentedTest`. Treat the current structure as a starting point, not as established conventions.

## Build & test commands

Run from the repo root. On Windows use `.\gradlew.bat` (PowerShell) or `./gradlew` (Git Bash).

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
- UI is Jetpack Compose + Material 3 only (no XML layouts). Theme lives in `ui/theme/` (`DominoAppTheme` uses dynamic color on Android 12+, falling back to the Purple/Pink template palette).
- Dependencies are managed through the version catalog `gradle/libs.versions.toml`; build scripts are Groovy (`build.gradle`, not `.kts`). Add new libraries to the catalog and reference them as `libs.<alias>`.
- Toolchain pins that constrain upgrades:
  - AGP 8.4.1, Gradle 8.6, Kotlin 1.9.0, Compose compiler extension `1.5.1` (must stay matched to the Kotlin version — Kotlin 2.x requires switching to the `org.jetbrains.kotlin.plugin.compose` plugin instead of `composeOptions`).
  - `core-ktx` is held at 1.13.1 and `activity-compose` at 1.9.3 on purpose: 1.16.0 / 1.10.x require `compileSdk 35` and AGP 8.6+ and break the build. Before bumping any AndroidX library, check its minimum compileSdk/AGP. Upgrading AGP also requires a newer Gradle wrapper.
  - Compose BOM `2023.08.00` is old; newer BOMs may require a newer Kotlin/Compose compiler.
  - `compileSdk`/`minSdk`/`targetSdk` are all 34, so no API-level guards below 34 are needed.
  - Java/JVM target is 1.8.

## Agent skills

`.claude/skills/` contains Android-specific skills (Compose, Material 3, Room, Navigation 3, coroutines/Flow, testing setup, edge-to-edge, AGP 9 upgrade, etc.), tracked in `skills-lock.json`. Prefer invoking the relevant skill when doing that kind of work.


# App de dominó

## Qué hace
- Registra los puntos de cada equipo o jugador durante un partido de dominó.
- El usuario define el puntaje meta al iniciar; al alcanzarlo, la app declara al ganador.
- Guarda el historial de partidas en el dispositivo.

## Stack
- Kotlin, Jetpack Compose, Material 3.
- Room (sobre SQLite) para la base de datos local. Sin backend ni internet.
- Arquitectura MVVM: Screen (Compose) -> ViewModel -> Repository -> Room DAO.

## Reglas
- Soy principiante: explica cada decisión importante en español antes de escribir el código.
- Usa siempre los colores y tipografía del tema de Material 3, nunca valores sueltos.
- Botones táctiles de al menos 48dp y textos del marcador grandes.
- Compila con ./gradlew assembleDebug después de cada cambio.