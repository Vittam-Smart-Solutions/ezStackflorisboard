# Adopt ezStack's git-tag semver versioning

## Context

Every other ezStack Android app (ezPigmy, ezBill) derives `versionCode`/`versionName` from git tags at build time via a `scripts/release.sh` + `scripts/version.sh` pair, per the platform-wide "Versioning Standard" in `d:\VittamWorkSpace\ezStack\CLAUDE.md`. This repo still had hardcoded `projectVersionCode=119` / `projectVersionName=0.6.0-alpha02` in `gradle.properties` — upstream FlorisBoard's own in-flight alpha numbering, inherited by the fork, with no ezStack meaning at all. Now that this is a signed, integrated, production-bound component (ezPigmy Keyboard), it follows the same convention as its siblings.

**Complication specific to this repo**: it's a fork with an `upstream` remote (`florisboard/florisboard`) still configured, carrying 111 inherited upstream `v*` tags (`v0.1.0` ... `v0.6.0-alpha02`, matching the old hardcoded version exactly). Reusing that number range for ezStack's own releases would be ambiguous going forward. **Decision: ezStack's own versioning starts fresh at `v1.0.0`** — a clean break, zero collision risk with upstream's alpha-series numbering. The 111 inherited tags were left alone (deleting shared tags is a separate, destructive call, not part of this change) — `git describe` is ancestry-based, not a global version-sort, so it resolves correctly to `v1.0.0` (and later ezStack tags) once one exists at the tip.

## What shipped

Reference implementation: `ezStackEzPigmyAndroidApp/scripts/release.sh` + `scripts/version.sh` (byte-identical copies also exist in `ezBillOperatorAndroidApp`, app name substituted). Ported both here with two deliberate adaptations:

1. **`app/build.gradle.kts`'s version computation reuses this file's own existing `providers.exec`-based idiom** (already present for `getGitCommitHash()`, used for `BuildConfig.BUILD_COMMIT_HASH`) rather than ezPigmy's raw-`ProcessBuilder` version — same formula/output, but consistent with how this file already shells out to git, and sidesteps ezPigmy/ezBill's diverging Windows `cmd /c`-wrapping question entirely (`providers.exec` handles that natively). Placed near the top of the file (before `configure<ApplicationExtension>`, not near `getGitCommitHash()` at the bottom) — Gradle Kotlin DSL scripts hoist top-level `fun` declarations but not `val` initializers, so the computed version values must be declared before the block that consumes them.
2. **Branch-name checks extended to include `dev`** (this repo's actual primary branch, not `main`/`master` like ezPigmy/ezBill) in both scripts' pre-flight checks.

**Files:**
- `gradle.properties` — `projectVersionCode`/`projectVersionName` (2 fields) replaced with a single reference-only `version=1.0.0` line (updated by `release.sh`, never read by the build — matches ezPigmy/ezBill exactly).
- `app/build.gradle.kts` — removed the `projectVersionCode`/`projectVersionName`/`projectVersionNameSuffix` property-reading block; added a `gitOutput()` helper + `computedVersionName`/`computedVersionCode` (formula: `major * 1_000_000 + minor * 10_000 + patch`, patch = tag's patch + commits since tag). `defaultConfig.versionCode`/`versionName` now use the computed values. `beta` build type's suffix hardcoded to `"-beta"` (previously derived from a manually-typed `-alpha02`-style string that no longer exists). `release` build type's suffix removed entirely (clean semver, no suffix). `debug`/`benchmark` build types untouched — they already derive their suffix from `getGitCommitHash(short = true)` directly.
- `scripts/release.sh`, `scripts/version.sh` (new) — direct ports, `chmod +x`'d.
- `v1.0.0` annotated tag created on the pre-existing tip commit and pushed to `origin` only (never `upstream`).

## Verified

- `./gradlew :app:assembleDebug` compiled clean both before and after tagging `v1.0.0`.
- **Before tagging**: built APK showed `versionCode=60051`, `versionName=0.6.51-debug+<hash>` — correctly (if unhelpfully) computed off the still-nearest inherited upstream tag `v0.6.0-alpha02` (51 commits since it). This confirmed the computation logic works and exactly matched the documented pre-condition for needing the bootstrap tag.
- **After tagging `v1.0.0` and pushing to origin**: rebuilt, confirmed via `aapt2 dump badging` — `versionCode=1000000`, `versionName=1.0.0-debug+<hash>`.

## Explicitly out of scope

- Deleting or rewriting the 111 inherited upstream tags.
- Actually running `release.sh` to cut a *new* release (e.g. `v1.0.1`) — the scripts are in place and verified; cutting further releases is a separate, deliberate action each time (the script itself gates on interactive confirmation before tagging and again before pushing).
- Any change to the `upstream` remote or fetch behavior.
- Wiring `version.sh`/`git.properties` into the Android build — matches ezPigmy/ezBill precedent of shipping it unused, for platform consistency only.
