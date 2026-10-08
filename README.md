<img align="left" width="80" height="80"
src=".github/repo_icon.png" alt="App icon">

# ezStack Keyboard

**ezStack Keyboard** is Vittam's private internal Android IME, built as a fork of the
open-source [FlorisBoard](https://github.com/florisboard/florisboard) project. It provides
a consistent, branded keyboard experience across the ezStack product line (ezPigmy, and
future Vittam apps) instead of relying on whatever system keyboard happens to be installed
on a device.

This is **not** a public release — there is no F-Droid/Play Store/Obtainium distribution.
Debug/beta/release APKs are built and installed directly across ezStack's own apps and
test devices.

## Relationship to upstream FlorisBoard

This repo tracks two remotes:
- `origin` — this fork, `Vittam-Smart-Solutions/ezStackflorisboard`
- `upstream` — the real open-source project, `florisboard/florisboard`

`AI_POLICY.md` (referenced from `CONTRIBUTING.md`) prohibits AI-authored code, commit
messages, PR descriptions, and documentation in anything contributed **upstream** to
florisboard/florisboard, and forbids autonomous ("vibe coding") upstream contributions
entirely. It does **not** restrict private/local use — normal AI-assisted development is
fine within this fork's own history.

## Where this is used

- ezPigmy Android pushes its day/night theme selection and font size to this keyboard at
  runtime (`KeyboardSettingsSync.kt` on the ezPigmy side), targeting the
  `floris_night_borderless` / `floris_day_borderless` theme IDs specifically — those two
  are restyled in place for ezStack branding (dark fills, 12dp corners, blue `#1A6CF0`
  accent) rather than shipped as a separately-named theme.
- Release builds share a signing certificate (`vittam-internal-release.jks`; debug builds
  use `vittam-internal-debug.jks`) with other Vittam internal apps, so this keyboard can
  pass signature-level permission checks when other ezStack apps talk to it.
- The numeric keypad (`inputType="number"` fields) is customized for ezStack's own
  amount/PIN-entry screens: a 3x4 grid (digits + decimal + backspace) with a full-width
  confirm bar below, instead of upstream's default layout.

## Build

```bash
./gradlew clean assembleDebug   # exactly what CI runs
./gradlew test                  # all unit tests, all modules
./gradlew :app:lintDebug        # Android Lint, baseline app/lint.xml
```

Requires JDK 17, Android SDK/NDK, CMake 3.22+, Clang 15+, and **Rust** (rustup + cargo on
PATH) — `:lib:native`'s CMake build shells out to `cargo`. Upstream officially supports
Linux/WSL2 only; plain Windows is untested by them.

## Module structure

| Module | Namespace | Purpose |
|---|---|---|
| `:app` | `dev.patrickgold.florisboard` | The IME app itself (`applicationId io.vittam.ezstack.keyboard`) |
| `:lib:kotlin` | — (pure JVM) | Base Kotlin utils, no Android deps |
| `:lib:android` | `org.florisboard.lib.android` | Android-framework utility layer |
| `:lib:color`, `:lib:compose` | `org.florisboard.lib.*` | Compose/color utilities |
| `:lib:snygg` | `org.florisboard.lib.snygg` | FlorisBoard's own CSS-like theme stylesheet engine/DSL |
| `:lib:native` | `org.florisboard.libnative` | The real JNI/Rust bridge, built into `libfl_native.so` |

## Versioning

Git-tag semver, consistent with the rest of the ezStack platform. Never bump manually —
run `scripts/release.sh` (`--dry-run` to preview), which scans commits since the last
`v*` tag, bumps by conventional-commit type, updates `CHANGELOG.md` and
`gradle.properties`, tags, and pushes.

## Used libraries, components and icons
* [AndroidX libraries](https://github.com/androidx/androidx) by
  [Android Jetpack](https://github.com/androidx)
* [AboutLibraries](https://github.com/mikepenz/AboutLibraries) by
  [mikepenz](https://github.com/mikepenz)
* [Google Material icons](https://github.com/google/material-design-icons) by
  [Google](https://github.com/google)
* [JetPref preference library](https://github.com/patrickgold/jetpref) by
  [patrickgold](https://github.com/patrickgold)
* [KotlinX coroutines library](https://github.com/Kotlin/kotlinx.coroutines) by
  [Kotlin](https://github.com/Kotlin)
* [KotlinX serialization library](https://github.com/Kotlin/kotlinx.serialization) by
  [Kotlin](https://github.com/Kotlin)

Many thanks to [Nikolay Anzarov](https://www.behance.net/nikolayanzarov) ([@BloodRaven0](https://github.com/BloodRaven0)) for designing and providing the original app icons this fork builds on!

## License
```
Copyright 2020-2026 The FlorisBoard Contributors
Copyright 2026 Vittam Smart Solutions (ezStack Keyboard fork)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

Built on top of [FlorisBoard](https://github.com/florisboard/florisboard) — thanks to
[The FlorisBoard Contributors](https://github.com/florisboard/florisboard/graphs/contributors)
for the original project this fork is based on.
