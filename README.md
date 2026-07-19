# Cosmere Companion

[![CI](https://github.com/mmahr22/Cosmere-Companion/actions/workflows/ci.yml/badge.svg)](https://github.com/mmahr22/Cosmere-Companion/actions/workflows/ci.yml)

An unofficial Android companion app for the **Cosmere RPG** (Stormlight Handbook)
by Brotherwise Games — in the spirit of apps like Fight Club 5e for D&D.

> **This is unofficial fan content, created and shared for non-commercial use.
> It has not been reviewed by Dragonsteel Entertainment, LLC or Brotherwise
> Games, LLC.** It contains no book text; you need your own copy of the
> Stormlight Handbook to play. See [NOTICE.md](NOTICE.md).

## Planned features

- **Characters** — guided character creation (ancestry, culture, attributes,
  heroic path, radiant path) and an interactive character sheet with health,
  focus, and investiture trackers, defenses, skills, and expertises.
- **Dice** — skill test roller with advantage/disadvantage and the plot die
  (Opportunities and Complications), plus damage rolls.
- **Reference** — a searchable browser for paths, talents, surges, items,
  and conditions.

## Project structure

| Module  | What it is |
| ------- | ---------- |
| `:core` | Pure Kotlin (JVM) module: game mechanics engine, data models, dice logic. Unit-tested; no Android dependency. |
| `:app`  | Android app (Jetpack Compose, Material 3). Depends on `:core`. |

The `:app` module is only included in the build when an Android SDK is
detected (`ANDROID_HOME`/`ANDROID_SDK_ROOT` env var or `local.properties`
with `sdk.dir`), so `:core` can be built and tested in headless environments:

```sh
./gradlew :core:test
```

## Building the app

1. Open the project in Android Studio (Ladybug or newer). It will create
   `local.properties` pointing at your SDK automatically.
2. Run the `app` configuration on a device/emulator (min SDK 26, Android 8.0).

Or from the command line with an SDK installed:

```sh
./gradlew :app:assembleDebug
# APK lands in app/build/outputs/apk/debug/
```
