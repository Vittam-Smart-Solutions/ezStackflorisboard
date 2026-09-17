# Delete key size and space bar language label

## Context

Two small usability complaints surfaced from actually using the keyboard on-device after the earlier bottom-row relayout (`bf1f1ffc`, which gave delete its own width factor of 2.2 once it stopped sharing a row with shift):

1. **Delete key too big, poorly placed for finger reach** — at 2.2x a normal key's width (vs. shift's 1.56x), it was visually dominant and easy to hit by accident at the end of the `z x c v b n m` row.
2. **Space bar showing "English (United States)"** — pointless clutter on a locked-down, English-only kiosk keyboard with no language switching at all.

## What shipped

### Delete key size

Went through `/superpowers:brainstorming` with the visual companion (mockups comparing same-position-smaller vs. moving delete to the number row vs. moving it next to Enter). Chosen: same position, smaller — width factor `2.2f → 1.56f` in `TextKey.kt`, now sharing the same `when` branch as `KeyCode.SHIFT` so the two keys flanking the letter rows are visually symmetric. No layout JSON changes, no position change.

**File:** `app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKey.kt` — `flayWidthFactor` computation, `KeyCode.SHIFT, KeyCode.DELETE -> 1.56f`.

### Space bar language label

Traced to `SpaceBarMode` — a 3-way JetPref enum preference (`NOTHING` / `CURRENT_LANGUAGE` / `SPACE_BAR_KEY`), defaulted to `CURRENT_LANGUAGE`, which drives a `ComputingEvaluator` that renders the active subtype's locale display name on the space key (`TextKeyboardLayout.kt`'s `TextKeyButton`). A live `ListPreference` for this already existed in Settings → Keyboard — not part of the earlier Settings-UI trim.

Changed the default to `SpaceBarMode.NOTHING` (blank space bar, not even a `␣` glyph) and removed the Settings dropdown entirely — consistent with everything else already stripped from this kiosk build; a 3-way choice implying language flexibility doesn't make sense when there's no language switching.

**Files:**
- `app/src/main/kotlin/dev/patrickgold/florisboard/app/AppPrefs.kt` — `spaceBarMode` default `CURRENT_LANGUAGE → NOTHING`.
- `app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/keyboard/KeyboardScreen.kt` — removed the `ListPreference` row + now-unused `SpaceBarMode` import.
- `app/src/main/kotlin/dev/patrickgold/florisboard/app/EnumDisplayEntries.kt` — removed the now-orphaned `SpaceBarMode::class` display-entries registration (only reachable from the removed Settings row) + its import.

## Verified on-device

Both changes built into a real signed `release` APK, installed over the existing app, confirmed via screenshot with the real IME open in ezPigmy's login screen: delete key visibly smaller and matching shift's proportions; space bar showing no text at all.

## Explicitly out of scope

- Alternative delete-key positions considered during brainstorming (number row, next to Enter) — explicitly rejected in favor of the smaller-same-spot option.
- `SpaceBarMode.SPACE_BAR_KEY` (a `␣` glyph instead of blank) — not chosen; "remove it" was interpreted literally.
