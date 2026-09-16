# Minimal Keyboard Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the default FlorisBoard characters keyboard into the user's minimal keyboard: a digit row always shown on top, QWERTY letters below, no way to reach the Symbols/Symbols2 page, with the caps toggle and font-size adjustment that already exist in FlorisBoard kept working.

**Architecture:** No new subsystem is built. Two existing, already-wired FlorisBoard mechanisms are repointed by editing configuration, not by writing new logic:
1. The built-in "number row" extension (`prefs.keyboard.numberRow`, `KeyboardManager.computeKeyboardAsync()`) is switched on by default instead of off, so digits 1-9,0 render as a real row prepended above the qwerty row for every subtype.
2. The `view_symbols` key (`KeyCode.VIEW_SYMBOLS = -202`) is removed from the one shared modifier-row layout (`charactersMod/default.json`) that every language's Characters keyboard merges in, so `KeyboardMode.SYMBOLS`/`SYMBOLS2` become unreachable from the keyboard UI.

Caps/shift (`InputShiftState` state machine in `KeyboardManager`) and the font-size multiplier (`prefs.keyboard.fontSizeMultiplierPortrait/Landscape`, wired through `ImeWindowController` → `FlorisImeTheme` → Snygg) are untouched, pre-existing, and already exposed in Settings → Keyboard — this plan only verifies they still work after the two edits above, it does not build them.

**Assumption to flag to the user before merging:** "no special characters" is scoped here to *removing the Symbols/Symbols2 keyboard pages* (the `!@#$%^&*` page). Basic punctuation already present on the default row — comma, period, and the language/media/space/enter keys — is left in place because it's needed for normal sentence typing. If the user actually wants those gone too, that's a follow-up edit to `charactersMod/default.json`.

**Tech Stack:** Kotlin (JetPref preferences), Android asset JSON (FlorisBoard's own layout format), Gradle, adb (manual verification — this repo has no UI/instrumentation test harness for keyboard layouts; CI itself only runs `assembleDebug`).

**Scope note:** This plan intentionally does **not** remove any subsystem (dictionary/NLP, clipboard history, emoji panel, etc.) — that is deliberately a separate follow-up plan, written and executed only after this one is confirmed working on-device. Doing the risky deletions after the new layout is proven means a bad deletion can't take down the keyboard behavior the user actually needs.

---

### Task 1: Turn the always-on number row on by default

**Files:**
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/AppPrefs.kt:480-483`

- [ ] **Step 1: Confirm current default**

Current code at `AppPrefs.kt:480-483`:

```kotlin
        val numberRow = boolean(
            key = "keyboard__number_row",
            default = false,
        )
```

- [ ] **Step 2: Flip the default to `true`**

```kotlin
        val numberRow = boolean(
            key = "keyboard__number_row",
            default = true,
        )
```

- [ ] **Step 3: Build the debug APK**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, output at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 4: Install and manually verify on a device/emulator**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

Then: enable FlorisBoard as the active input method (Settings → System → Languages & input → On-screen keyboard), open any text field, and switch to FlorisBoard.

Expected: a row showing `1 2 3 4 5 6 7 8 9 0` appears above the `q w e r t y u i o p` row, with no toggle needed. Also open FlorisBoard's own Settings → Keyboard screen and confirm the "Number row" switch shows as ON.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/dev/patrickgold/florisboard/app/AppPrefs.kt
git commit -m "feat: enable number row by default"
```

---

### Task 2: Remove the Symbols-page toggle key

**Files:**
- Modify: `app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/charactersMod/default.json`

This is the one shared modifier-row layout every language's Characters keyboard merges in (`KeyboardManager.kt:336-343` resolves it via `extCoreLayout("default")` regardless of subtype), so removing the key here removes Symbols-page access for every language, not just English.

- [ ] **Step 1: Validate the current JSON parses (baseline)**

Run: `python -m json.tool "app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/charactersMod/default.json"`
Expected: pretty-printed JSON is echoed back with no error (confirms the file is valid before editing, so any failure after editing is attributable to the edit).

- [ ] **Step 2: Remove the `view_symbols` key object**

Current file content:

```json
[
  [
    { "code":  -11, "label": "shift", "type": "modifier" },
    { "code":    0, "type": "placeholder" },
    { "code":   -7, "label": "delete", "type": "enter_editing" }
  ],
  [
    { "code": -202, "label": "view_symbols", "type": "system_gui" },
    { "$": "variation_selector",
      "default":  { "code":   44, "label": ",", "groupId": 1 },
      "email":    { "code":   64, "label": "@", "groupId": 1 },
      "uri":      { "code":   47, "label": "/", "groupId": 1 }
    },
    { "code": -227, "label": "language_switch", "type": "system_gui" },
    { "code": -212, "label": "ime_ui_mode_media", "type": "system_gui" },
    { "code":   32, "label": "space" },
    { "code":   46, "label": ".", "groupId": 2 },
    { "code":   10, "label": "enter", "groupId": 3, "type": "enter_editing" }
  ]
]
```

New file content (the `view_symbols` entry and its trailing comma removed, everything else identical):

```json
[
  [
    { "code":  -11, "label": "shift", "type": "modifier" },
    { "code":    0, "type": "placeholder" },
    { "code":   -7, "label": "delete", "type": "enter_editing" }
  ],
  [
    { "$": "variation_selector",
      "default":  { "code":   44, "label": ",", "groupId": 1 },
      "email":    { "code":   64, "label": "@", "groupId": 1 },
      "uri":      { "code":   47, "label": "/", "groupId": 1 }
    },
    { "code": -227, "label": "language_switch", "type": "system_gui" },
    { "code": -212, "label": "ime_ui_mode_media", "type": "system_gui" },
    { "code":   32, "label": "space" },
    { "code":   46, "label": ".", "groupId": 2 },
    { "code":   10, "label": "enter", "groupId": 3, "type": "enter_editing" }
  ]
]
```

- [ ] **Step 3: Validate the edited JSON still parses**

Run: `python -m json.tool "app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/charactersMod/default.json"`
Expected: pretty-printed JSON echoed back with no error.

- [ ] **Step 4: Build the debug APK**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Install and manually verify on a device/emulator**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

Open a text field with FlorisBoard active.

Expected: the bottom row no longer has a "?123"/symbols key — it now starts directly with the comma key, followed by language-switch, media/emoji, space, period, enter. There is no key anywhere on the Characters keyboard that opens the Symbols page.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/charactersMod/default.json
git commit -m "feat: remove symbols-page toggle key from the keyboard"
```

**Scope correction (found during code review of the above):** `charactersMod/default.json` is not the only modifier layout — `extension.json` routes ~20 of 77 bundled `characters` layouts (Arabic, Hebrew, Armenian, Persian, Kurdish, Dvorak variants, Neo2, JIS, Bengali Unijoy, Diktor, Urdu Phonetic) to one of 14 other named `charactersMod/*.json` files via a per-layout `"modifier"` override, and each of those still had its own `view_symbols` key. Fixed by repeating the identical edit on all 14 sibling files (`armenian.json`, `arabic.json`, `bengali_unijoy.json`, `diktor.json`, `dvorak.json`, `dvorak_de.json`, `dvorak_se.json`, `hebrew.json`, `jis.json`, `kurdish.json`, `neo2.json`, `persian.json`, `persian2.json`, `persian3.json`), committed separately as `feat: remove symbols-page toggle key from remaining language layouts`.

**Final accepted scope for "no special characters":** the Symbols/Symbols2 page is now unreachable from the Characters keyboard for every bundled language — no `characters`/`charactersMod` layout anywhere in the tree contains a key that opens it. Two things were deliberately left alone as out of scope:
- `symbols2Mod/*.json` and `numericAdvanced/*.json` still contain their own internal `view_symbols`/`-202` keys (used for navigation once already inside those pages), but since nothing can reach those pages from Characters anymore, these are dead/unreachable keys with no user-visible effect — cosmetic dead-code removal, not behavior, so it belongs in the Plan B cleanup pass, not here.
- FlorisBoard's `CYCLE_TO_NEXT_KEYBOARD_MODE`/`CYCLE_TO_PREVIOUS_KEYBOARD_MODE` swipe gesture actions (`KeyboardManager.kt`) can still jump straight to Symbols in code, independent of any layout key — but this is not a default gesture binding; it only applies if the user deliberately assigns it in Settings → Gestures. Not fixed by this plan.

---

### Task 3: Regression-verify caps toggle and font-size adjustment

No code changes in this task — both features already exist and are unmodified by Tasks 1-2 (`InputShiftState` state machine in `KeyboardManager.kt:484-531`, and `prefs.keyboard.fontSizeMultiplierPortrait/Landscape` wired through `FlorisImeTheme.kt`/`ImeWindowController.kt`). This task only confirms the two layout edits didn't regress them.

**Files:** none modified.

- [ ] **Step 1: Verify caps/shift toggle**

With FlorisBoard active in a text field: tap Shift once, type a letter — expected it's capitalized and the next letter reverts to lowercase (`SHIFTED_MANUAL`, one-shot). Tap Shift twice quickly (or once, if Settings → Keyboard → "Capitalization behavior" is set to cycle) — expected the keyboard enters caps-lock and stays capitalized until Shift is tapped again.

- [ ] **Step 2: Verify font-size adjustment**

Open FlorisBoard Settings → Keyboard → "Font size", drag the slider to a different value (e.g. 130%), return to a text field with FlorisBoard active.

Expected: key labels (letters and the digit row from Task 1) render visibly larger/smaller matching the new percentage, live.

- [ ] **Step 3: Record the result**

If either check fails, that's a regression from Task 1 or Task 2 — stop and diagnose before proceeding to the follow-up cleanup plan, rather than layering more changes on top of a broken keyboard.

---

## Next plan (separate document, written after this one is verified on-device)

A follow-up plan will remove the two subsystems the user confirmed are unneeded for this personal fork:
- Word suggestions / dictionary / NLP (`ime/nlp/`, `ime/dictionary/`, the smartbar suggestion strip, the Room dictionary DB)
- Clipboard history manager and the emoji/emoticon panel (`ime/clipboard/`, `ime/media/emoji`, `ime/media/emoticon`, their Room DB/assets, and the smartbar clipboard preview)

Multi-language support and the theme/extension store are being kept, per the user's answer, so they are out of scope for that cleanup pass too.
