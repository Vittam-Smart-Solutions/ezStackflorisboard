# Remove Clipboard, Emoji/Emoticon, and Remaining Special-Char Dead Code — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** This is the follow-up plan flagged at the bottom of `docs/superpowers/plans/2026-09-08-minimal-keyboard-layout.md` (already merged — number row + Symbols page removal are done, commits `56af0594`/`6ae2c369`/`e5636d24`). It removes the two subsystems that plan deliberately deferred — the clipboard history manager/panel and the emoji + emoticon panel — and finishes the "no special characters" cleanup by deleting the unreachable `view_symbols` keys and dead gesture-cycle branches that plan's scope note explicitly left for later.

**Architecture / key decisions locked in by investigation (see rationale below each):**
1. **Media (`-212`) and Clipboard (`-213`) are two independent panels/key codes, not tabs of one shared panel.** `MediaInputLayout` hosts only the emoji picker (emoticon is already dead code, wired nowhere). Removing emoji empties Media entirely, so the Media key/panel is removed too. Clipboard is a fully separate code path.
2. **`ClipboardManager` is not history-only** — it's also the app's generic system-clipboard bridge, used by basic cut/copy/paste (`EditorInstance`), and by three unrelated "copy to clipboard" conveniences (About screen's copy-version button, debug-log export, a devtools overlay). Deleting the class outright would break basic paste. Instead, **`ClipboardManager` is shrunk to a ~30-line raw system-clipboard wrapper** (no Room DB, no history, no file storage, no pinning) — cut/copy/paste keep working, backed directly by Android's system clipboard, with no in-keyboard history.
3. **Emoticon/kaomoji code (`EmoticonKeyData.kt`, `EmoticonLayoutData.kt`, `emoticons.json`) is pre-existing dead code** — zero references anywhere in the codebase. Deleted as a zero-risk bonus, not something this plan's removal introduces.
4. **`FlorisCopyToClipboardActivity` (the "Share → Copy image to clipboard" share-target) is kept, untouched.** It writes directly to Android's raw system `ClipboardManager` service already — it has zero dependency on our `ClipboardManager` class, the history DB, or the panel being removed. It's an unrelated share-sheet utility, not part of the keyboard's clipboard-history feature. Flag to user: say so explicitly if this should go too.
5. **Basic quick-action buttons `CLIPBOARD_COPY`/`CLIPBOARD_CUT`/`CLIPBOARD_PASTE`/`CLIPBOARD_SELECT_ALL` are kept** in the default quick-action row — they're generic text-editing shortcuts, not clipboard-*history* specific, and now route through the shrunk system-clipboard-only `ClipboardManager`. `CLIPBOARD_CLEAR_PRIMARY_CLIP` (clears the *history*-tracked clip specifically) and the two panel-opening keys `IME_UI_MODE_MEDIA`/`IME_UI_MODE_CLIPBOARD` are removed, since what they opened no longer exists. Flag to user: say so if you'd rather drop copy/cut/paste/select-all too and rely solely on the OS's own text-selection toolbar.
6. **Clipboard's Room DB (`ClipboardHistoryDatabase`, `ClipboardFilesDatabase`) is fully independent of the dictionary DB** — confirmed via `ClipboardDatabase.kt`, two separate `@Database` classes, `fallbackToDestructiveMigration()`. No migration or version bump needed anywhere else when it's deleted.

**Tech Stack:** Kotlin, Jetpack Compose, Room (being removed for clipboard), JetPref preferences, Android asset JSON, Gradle. No UI/instrumentation test harness for this repo (per `CLAUDE.md`) — verification is `./gradlew :app:assembleDebug` (a missed reference anywhere in these two subsystems fails the build immediately, since every deleted `KeyCode`/pref/class is referenced by name) plus manual on-device typing/paste checks.

**Task ordering rationale:** Task 1 (emoji/media) and Task 2 (clipboard) touch several of the same shared files (`KeyCode.kt`, `TextKeyData.kt`, `KeyboardManager.kt`, `ComputingEvaluator.kt`, `PopupUiController.kt`, `QuickAction.kt`, `QuickActionArrangement.kt`, `SwipeAction.kt`, `EnumDisplayEntries.kt`, `Routes.kt`, `HomeScreen.kt`) but at different, non-overlapping lines/constants — each task is independently buildable and is verified with its own `assembleDebug` before moving to the next, so a mistake in Task 2 can't be confused with a Task 1 regression. Task 3 (dead special-char code) is fully independent of both. This mirrors the granularity the prior plan used (one task per coherent, separately-buildable removal, not one task per file).

---

### Task 1: Remove the emoji panel, emoji subsystem, and dead emoticon code

**Files:**
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoji/Emoji.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoji/EmojiCategory.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoji/EmojiData.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoji/EmojiHistory.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoji/EmojiPaletteView.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoji/EmojiSet.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoji/EmojiSuggestionProvider.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoji/EmojiSuggestionType.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoji/FlorisEmojiCompat.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoticon/EmoticonKeyData.kt` (dead code, zero refs)
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoticon/EmoticonLayoutData.kt` (dead code, zero refs)
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/MediaInputLayout.kt`
- Delete: `app/src/main/assets/ime/media/emoji/` (whole directory — `root.txt`, `en.txt`, `de.txt`, `es.txt`, `pt.txt`, `fr.txt`, `it.txt`, ~2.6 MB)
- Delete: `app/src/main/assets/ime/media/emoticon/emoticons.json` (dead, unreachable, but delete alongside since the containing feature is gone)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/FlorisApplication.kt` (remove `FlorisEmojiCompat.init(this)` call)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/window/ImeWindow.kt` (remove the `ImeUiMode.MEDIA` branch)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/ImeUiMode.kt` (remove `MEDIA` enum entry)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/key/KeyCode.kt` (remove `IME_UI_MODE_MEDIA` constant)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyData.kt` (remove `IME_UI_MODE_MEDIA` `TextKeyData` object, ~line 415-420)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt` (remove `KeyCode.IME_UI_MODE_MEDIA ->` branch at line 741, and the `SwipeAction.SWITCH_TO_MEDIA_CONTEXT ->` branch at line 272)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/ComputingEvaluator.kt` (remove the `KeyCode.IME_UI_MODE_MEDIA ->` icon branch at line 259-261)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/popup/PopupUiController.kt` (remove `KeyCode.IME_UI_MODE_MEDIA` from `ExceptionsForKeyCodes` at line 67)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickAction.kt` (remove `KeyCode.IME_UI_MODE_MEDIA ->` entries in `computeDisplayName`/`computeTooltip`, lines 92 and 132)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickActionArrangement.kt` (remove `QuickAction.InsertKey(TextKeyData.IME_UI_MODE_MEDIA)` from the `Default` list, line 77)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/gestures/SwipeAction.kt` (remove `SWITCH_TO_MEDIA_CONTEXT` enum entry, line 49)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/EnumDisplayEntries.kt` (remove the `EmojiHistory.UpdateStrategy::class`, `EmojiSkinTone::class`, `EmojiSuggestionType::class` blocks; remove the `SwipeAction.SWITCH_TO_MEDIA_CONTEXT` gesture-display entry; remove `UtilityKeyAction.SWITCH_TO_EMOJIS`/`DYNAMIC_SWITCH_LANGUAGE_EMOJIS` entries — read the file first, these are scattered across ~lines 190-272 and ~619-732 per the investigation, exact line numbers will have shifted)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/AppPrefs.kt` (remove the entire `val emoji = Emoji()` / `inner class Emoji { ... }` block, current lines 227-286; remove the `media__emoji_recently_used*` migration shim block, current lines ~754-765 — read the file first to confirm current line numbers since Task order may shift them)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/Routes.kt` (remove `import ...settings.media.MediaScreen` at line 72, remove `@Deeplink("settings/media") object Media` at lines 172-173, remove `composableWithDeepLink(Settings.Media::class) { MediaScreen() }` at line 315)
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/media/MediaScreen.kt`
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/HomeScreen.kt` (remove the Media settings-list row, lines 143-146)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/NlpManager.kt` (remove the `EmojiSuggestionProvider` inner class and its registration — read the file first to find current extent, it's a self-contained inner class per the investigation)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/SuggestionCandidate.kt` (remove any `EmojiSuggestionCandidate` type if present — grep to confirm, the investigation flagged this file for `ClipboardSuggestionCandidate` but noted emoji suggestion wiring lives in `NlpManager.kt`'s `EmojiSuggestionProvider`; check for an emoji-specific candidate type too)

- [ ] **Step 1: Delete the emoji/emoticon source files and assets**

```bash
rm -rf "app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoji"
rm -rf "app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/emoticon"
rm "app/src/main/kotlin/dev/patrickgold/florisboard/ime/media/MediaInputLayout.kt"
rm -rf "app/src/main/assets/ime/media/emoji"
rm -rf "app/src/main/assets/ime/media/emoticon"
```

- [ ] **Step 2: Remove the `ImeUiMode.MEDIA` route**

In `ime/window/ImeWindow.kt`, current code (~line 226-230):
```kotlin
            when (state.imeUiMode) {
                ImeUiMode.TEXT -> TextInputLayout()
                ImeUiMode.MEDIA -> ProvideActualLayoutDirection { MediaInputLayout() }
                ImeUiMode.CLIPBOARD -> ProvideActualLayoutDirection { ClipboardInputLayout() }
            }
```
New code (remove the `MEDIA` branch; the `CLIPBOARD` branch stays until Task 2):
```kotlin
            when (state.imeUiMode) {
                ImeUiMode.TEXT -> TextInputLayout()
                ImeUiMode.CLIPBOARD -> ProvideActualLayoutDirection { ClipboardInputLayout() }
            }
```
Remove the `MEDIA` entry from `ime/ImeUiMode.kt`'s enum (it's `TEXT(0), MEDIA(1), CLIPBOARD(2)` — removing the middle entry is safe since Kotlin enums aren't matched to these ordinals anywhere sensitive; grep `ImeUiMode.MEDIA` across the repo first to confirm no other reference survives after this task's other edits).

- [ ] **Step 3: Remove `FlorisEmojiCompat.init(this)` from the app composition root**

In `FlorisApplication.kt`, delete this line from `onCreate()` (currently line 98):
```kotlin
            FlorisEmojiCompat.init(this)
```

- [ ] **Step 4: Remove `IME_UI_MODE_MEDIA` from `KeyCode.kt`**

Current (`ime/text/key/KeyCode.kt` lines 90-92):
```kotlin
    const val IME_UI_MODE_TEXT =            -211
    const val IME_UI_MODE_MEDIA =           -212
    const val IME_UI_MODE_CLIPBOARD =       -213
```
New (remove the middle line only — `IME_UI_MODE_CLIPBOARD` stays until Task 2):
```kotlin
    const val IME_UI_MODE_TEXT =            -211
    const val IME_UI_MODE_CLIPBOARD =       -213
```

- [ ] **Step 5: Remove the `IME_UI_MODE_MEDIA` `TextKeyData` object**

In `ime/text/keyboard/TextKeyData.kt` (~lines 415-420), delete the `val IME_UI_MODE_MEDIA = TextKeyData(...)` block (leave `IME_UI_MODE_CLIPBOARD` at ~421-425 until Task 2).

- [ ] **Step 6: Remove the `IME_UI_MODE_MEDIA` key-press and swipe-action handlers**

In `ime/keyboard/KeyboardManager.kt`, delete this line (~741):
```kotlin
            KeyCode.IME_UI_MODE_MEDIA -> activeState.imeUiMode = ImeUiMode.MEDIA
```
And delete this branch from `executeSwipeAction()` (~272):
```kotlin
            SwipeAction.SWITCH_TO_MEDIA_CONTEXT -> TextKeyData.IME_UI_MODE_MEDIA
```

- [ ] **Step 7: Remove the `IME_UI_MODE_MEDIA` icon branch**

In `ime/keyboard/ComputingEvaluator.kt` (~lines 259-261), delete:
```kotlin
        KeyCode.IME_UI_MODE_MEDIA -> {
            Icons.Default.SentimentSatisfiedAlt
        }
```

- [ ] **Step 8: Remove from `PopupUiController.kt`'s exceptions list**

In `ime/popup/PopupUiController.kt` (~line 67), remove the `KeyCode.IME_UI_MODE_MEDIA,` line from `ExceptionsForKeyCodes`.

- [ ] **Step 9: Remove from quick-action display name/tooltip and default arrangement**

In `ime/smartbar/quickaction/QuickAction.kt`, remove the two `KeyCode.IME_UI_MODE_MEDIA -> R.string...` lines (in `computeDisplayName` ~line 92 and `computeTooltip` ~line 132).

In `ime/smartbar/quickaction/QuickActionArrangement.kt` (~line 77), remove:
```kotlin
                QuickAction.InsertKey(TextKeyData.IME_UI_MODE_MEDIA),
```

- [ ] **Step 10: Remove the `SWITCH_TO_MEDIA_CONTEXT` gesture action**

In `ime/text/gestures/SwipeAction.kt` (~line 49), remove the `SWITCH_TO_MEDIA_CONTEXT,` enum entry.

In `app/EnumDisplayEntries.kt`, remove its corresponding display-string entry (grep `SWITCH_TO_MEDIA_CONTEXT` after Step 10's enum edit — the compiler will flag anything missed as an exhaustive-`when` error, which is the actual safety net here).

- [ ] **Step 11: Remove emoji-related `EnumDisplayEntries.kt` blocks**

Read the current file first (line numbers will have shifted from the investigation's ~190-272/~619-732 estimates). Remove display-entry blocks for: `EmojiHistory.UpdateStrategy::class`, `EmojiSkinTone::class`, `EmojiSuggestionType::class`, and the `UtilityKeyAction.SWITCH_TO_EMOJIS` / `UtilityKeyAction.DYNAMIC_SWITCH_LANGUAGE_EMOJIS` entries (search for `Emoji` case-insensitively in the file to find all of them).

- [ ] **Step 12: Remove the emoji prefs group and its migration shim**

In `app/AppPrefs.kt`, delete the entire block:
```kotlin
    val emoji = Emoji()
    inner class Emoji {
        val preferredSkinTone = enum(
            key = "emoji__preferred_skin_tone",
            default = EmojiSkinTone.DEFAULT,
        )
        val preferredHairStyle = enum(
            key = "emoji__preferred_hair_style",
            default = EmojiHairStyle.DEFAULT,
        )
        val historyEnabled = boolean(
            key = "emoji__history_enabled",
            default = true,
        )
        val historyData = custom(
            key = "emoji__history_data",
            default = EmojiHistory.Empty,
            serializer = EmojiHistory.Serializer,
        )
        val historyPinnedUpdateStrategy = enum(
            key = "emoji__history_pinned_update_strategy",
            default = EmojiHistory.UpdateStrategy.MANUAL_SORT_PREPEND,
        )
        val historyPinnedMaxSize = int(
            key = "emoji__history_pinned_max_size",
            default = EmojiHistory.MaxSizeUnlimited,
        )
        val historyRecentUpdateStrategy = enum(
            key = "emoji__history_recent_update_strategy",
            default = EmojiHistory.UpdateStrategy.AUTO_SORT_PREPEND,
        )
        val historyRecentMaxSize = int(
            key = "emoji__history_recent_max_size",
            default = 90,
        )
        val suggestionEnabled = boolean(
            key = "emoji__suggestion_enabled",
            default = true,
        )
        val suggestionType = enum(
            key = "emoji__suggestion_type",
            default = EmojiSuggestionType.LEADING_COLON,
        )
        val suggestionUpdateHistory = boolean(
            key = "emoji__suggestion_update_history",
            default = true,
        )
        val suggestionCandidateShowName = boolean(
            key = "emoji__suggestion_candidate_show_name",
            default = false,
        )
        val suggestionQueryMinLength = int(
            key = "emoji__suggestion_query_min_length",
            default = 3,
        )
        val suggestionCandidateMaxCount = int(
            key = "emoji__suggestion_candidate_max_count",
            default = 5,
        )
    }
```
Also remove the `media__emoji_recently_used*` → `emoji__*` pref-migration block (read the file to find its current location, investigation found it at ~lines 754-765 before this task's other deletions shift things — it's a one-time migration `when`/`if` shim referencing now-deleted pref keys as string literals, safe to delete outright since it only migrates old datastore keys).

- [ ] **Step 13: Remove the Media settings screen, its route, and its home-screen entry**

```bash
rm "app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/media/MediaScreen.kt"
```
In `app/Routes.kt`: remove `import dev.patrickgold.florisboard.app.settings.media.MediaScreen` (line 72), remove the `@Deeplink("settings/media") object Media` block (lines 172-173), remove `composableWithDeepLink(Settings.Media::class) { MediaScreen() }` (line 315).

In `app/settings/HomeScreen.kt`: remove the Media row (lines 143-146, the one navigating to `Routes.Settings.Media` with `Icons.Default.SentimentSatisfiedAlt`).

- [ ] **Step 14: Remove the emoji suggestion provider from `NlpManager.kt`**

Read `ime/nlp/NlpManager.kt` first. Remove the `EmojiSuggestionProvider` inner/nested class and wherever it's registered/invoked in the suggestion pipeline (the clipboard suggestion provider in the same file is a separate class handled in Task 2 — don't touch it here). Grep `EmojiSuggestionCandidate` in `ime/nlp/SuggestionCandidate.kt` and remove that type too if it exists.

- [ ] **Step 15: Build and fix any compile errors from missed references**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`. Any missed reference to a deleted emoji/media symbol fails the build with an unresolved-reference error naming the exact file/line — fix those before proceeding (this is the real verification gate for this task, since there's no test harness for keyboard UI in this repo).

- [ ] **Step 16: Install and manually verify on a device/emulator**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

Expected: no key on the Characters keyboard opens an emoji/media panel (the smiley key is gone from the default quick-action row and from wherever `ime_ui_mode_media` was reachable). FlorisBoard's Settings home screen no longer shows a "Media" row.

- [ ] **Step 17: Commit**

```bash
git add -A -- app/src/main/kotlin/dev/patrickgold/florisboard/ime/media app/src/main/assets/ime/media/emoji app/src/main/assets/ime/media/emoticon
git add app/src/main/kotlin/dev/patrickgold/florisboard/FlorisApplication.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/window/ImeWindow.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/ImeUiMode.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/key/KeyCode.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyData.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/ComputingEvaluator.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/popup/PopupUiController.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickAction.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickActionArrangement.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/gestures/SwipeAction.kt app/src/main/kotlin/dev/patrickgold/florisboard/app/EnumDisplayEntries.kt app/src/main/kotlin/dev/patrickgold/florisboard/app/AppPrefs.kt app/src/main/kotlin/dev/patrickgold/florisboard/app/Routes.kt app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/HomeScreen.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/NlpManager.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/SuggestionCandidate.kt
git commit -m "feat: remove emoji/emoticon panel and subsystem"
```

---

### Task 2: Remove clipboard history; shrink ClipboardManager to a raw system-clipboard bridge

**Files:**
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/ClipboardHistory.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/ClipboardSyncBehavior.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/ClipboardInputLayout.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/provider/ClipboardDatabase.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/provider/ClipboardFileStorage.kt`
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/provider/ClipboardMediaProvider.kt`
- Rewrite: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/ClipboardManager.kt` (410 lines → ~35 lines)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/editor/EditorInstance.kt` (simplify `performClipboardCut/Copy/Paste`, delete `commitClipboardItem`)
- Modify: `app/src/main/AndroidManifest.xml` (remove the `ClipboardMediaProvider` `<provider>` entry; keep `FlorisCopyToClipboardActivity` — see decision #4 above)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/window/ImeWindow.kt` (remove the `ImeUiMode.CLIPBOARD` branch)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/ImeUiMode.kt` (remove `CLIPBOARD` enum entry)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/key/KeyCode.kt` (remove `IME_UI_MODE_CLIPBOARD`; remove `CLIPBOARD_CLEAR_PRIMARY_CLIP` per decision #5 — keep `CLIPBOARD_COPY`/`CUT`/`PASTE`/`SELECT_ALL`)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyData.kt` (remove `IME_UI_MODE_CLIPBOARD` object ~line 421-425, remove `CLIPBOARD_CLEAR_PRIMARY_CLIP` object ~line 333-337)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt` (remove `KeyCode.IME_UI_MODE_CLIPBOARD ->` branch, ~742; remove `SwipeAction.SWITCH_TO_CLIPBOARD_CONTEXT ->` branch, ~271)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/ComputingEvaluator.kt` (remove `KeyCode.IME_UI_MODE_CLIPBOARD ->` icon branch, ~262-264)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/popup/PopupUiController.kt` (remove `KeyCode.IME_UI_MODE_CLIPBOARD` from `ExceptionsForKeyCodes`, ~line 68)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickAction.kt` (remove `IME_UI_MODE_CLIPBOARD` and `CLIPBOARD_CLEAR_PRIMARY_CLIP` display-name/tooltip lines; keep the copy/cut/paste/select-all lines)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickActionArrangement.kt` (remove `IME_UI_MODE_CLIPBOARD` and `CLIPBOARD_CLEAR_PRIMARY_CLIP` entries from `Default`; keep the copy/cut/paste/select-all entries)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/gestures/SwipeAction.kt` (remove `SWITCH_TO_CLIPBOARD_CONTEXT` entry)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/EnumDisplayEntries.kt` (remove `ClipboardSyncBehavior::class` block ~108-128, remove `SwipeAction.SWITCH_TO_CLIPBOARD_CONTEXT` gesture entry ~619-620 — read file first, lines shift after Task 1)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/AppPrefs.kt` (remove `val clipboard = Clipboard()` block lines 78-157; remove `devtools.showPrimaryClip` pref lines 185-188; remove the `suggestion__clipboard_content_*`/`clipboard__*` migration shims ~788-899 — read file first)
- Delete: `app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/clipboard/ClipboardScreen.kt`
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/Routes.kt` (remove `ClipboardScreen` import line 60, `@Deeplink("settings/clipboard") object Clipboard` lines 168-169, `composableWithDeepLink(Settings.Clipboard::class) { ClipboardScreen() }` line 313)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/HomeScreen.kt` (remove Clipboard row, lines 138-141)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/devtools/DevtoolsOverlay.kt` (repoint `showPrimaryClip` overlay to the new `ClipboardManager.primaryClipText`, or delete the overlay feature if `devtools.showPrimaryClip` pref is removed — read file first, it's a small self-contained overlay gated by one pref)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/about/AboutScreen.kt` (keep `clipboardManager.addNewPlaintext(...)` call working — no change needed if `addNewPlaintext(String)` signature is preserved in the rewrite, see Step 2)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/devtools/ExportDebugLogScreen.kt` (same — no change needed if `addNewPlaintext(String)` is preserved)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/NlpManager.kt` (remove `ClipboardSuggestionProvider` inner class, lines 57/63/91/97/259/281/349-450 per investigation — read file first)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/SuggestionCandidate.kt` (remove `ClipboardSuggestionCandidate` type, lines 28/124-125)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/CandidatesRow.kt` (remove the `ClipboardSuggestionCandidate` import and styling branch, lines 43/150)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/advanced/BackupScreen.kt` (remove clipboard-history backup section — read file first, it's mixed with unrelated prefs/theme/keyboard backup per investigation, lines ~196-384)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/advanced/RestoreScreen.kt` (remove clipboard-history restore section — read file first, lines ~180-231)

- [ ] **Step 1: Delete the clipboard history/UI/storage files**

```bash
rm "app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/ClipboardHistory.kt"
rm "app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/ClipboardSyncBehavior.kt"
rm "app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/ClipboardInputLayout.kt"
rm "app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/provider/ClipboardDatabase.kt"
rm "app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/provider/ClipboardFileStorage.kt"
rm "app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard/provider/ClipboardMediaProvider.kt"
rm "app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/clipboard/ClipboardScreen.kt"
```

- [ ] **Step 2: Rewrite `ClipboardManager.kt` as a minimal raw system-clipboard bridge**

Full new content of `ime/clipboard/ClipboardManager.kt`:
```kotlin
/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.clipboard

import android.content.ClipData
import android.content.Context
import java.io.Closeable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.florisboard.lib.android.AndroidClipboardManager
import org.florisboard.lib.android.AndroidClipboardManager_OnPrimaryClipChangedListener
import org.florisboard.lib.android.systemService

/**
 * Minimal wrapper around the system clipboard. No history, no persistence, no rich-content
 * paste — just enough for basic cut/copy/paste plus the few "copy this text" convenience
 * buttons elsewhere in the app (About screen, debug log export, devtools overlay).
 */
class ClipboardManager(
    context: Context,
) : AndroidClipboardManager_OnPrimaryClipChangedListener, Closeable {
    private val systemClipboardManager = context.systemService(AndroidClipboardManager::class)

    val primaryClipTextFlow: StateFlow<String?>
        field = MutableStateFlow(readPrimaryClipText())
    val primaryClipText: String?
        get() = primaryClipTextFlow.value

    init {
        systemClipboardManager.addPrimaryClipChangedListener(this)
    }

    override fun onPrimaryClipChanged() {
        primaryClipTextFlow.value = readPrimaryClipText()
    }

    private fun readPrimaryClipText(): String? {
        val clip = systemClipboardManager.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).text?.toString()
    }

    fun addNewPlaintext(text: String) {
        systemClipboardManager.setPrimaryClip(ClipData.newPlainText(text, text))
        primaryClipTextFlow.value = text
    }

    override fun close() {
        systemClipboardManager.removePrimaryClipChangedListener(this)
    }
}
```
This drops `initializeForContext()` (was only for opening the Room DB) — remove its call site next.

- [ ] **Step 3: Remove `clipboardManager.value.initializeForContext(this)` from `FlorisApplication.kt`**

Delete this line from `init()` (currently line 126):
```kotlin
        clipboardManager.value.initializeForContext(this)
```

- [ ] **Step 4: Simplify `EditorInstance.kt`'s clipboard methods**

Remove these imports (no longer used):
```kotlin
import android.content.ClipDescription
import android.content.ContentUris
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardFileStorage
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
```

Delete the `commitClipboardItem()` method entirely (current lines 296-334, the whole doc comment + function).

Replace `performClipboardCut()`/`performClipboardCopy()`/`performClipboardPaste()` (current lines 414-465) with:
```kotlin
    /**
     * Performs a cut command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardCut(): Boolean {
        autoSpace.setInactive()
        phantomSpace.setInactive()
        val text = activeContent.selectedText.ifBlank { currentInputConnection()?.getSelectedText(0) }
        if (text != null) {
            clipboardManager.addNewPlaintext(text.toString())
        } else {
            appContext.showShortToastSync("Failed to retrieve selected text requested to cut: Eiter selection state is invalid or an error occurred within the input connection.")
        }
        return deleteBackwards(OperationUnit.CHARACTERS)
    }

    /**
     * Performs a copy command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardCopy(): Boolean {
        autoSpace.setInactive()
        phantomSpace.setInactive()
        val text = activeContent.selectedText.ifBlank { currentInputConnection()?.getSelectedText(0) }
        if (text != null) {
            clipboardManager.addNewPlaintext(text.toString())
        } else {
            appContext.showShortToastSync("Failed to retrieve selected text requested to copy: Eiter selection state is invalid or an error occurred within the input connection.")
        }
        val activeSelection = activeContent.selection
        return setSelection(activeSelection.end, activeSelection.end)
    }

    /**
     * Performs a paste command on this editor instance and adjusts both the cursor position and
     * composing region, if any.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun performClipboardPaste(): Boolean {
        autoSpace.setInactive()
        phantomSpace.setInactive()
        val text = clipboardManager.primaryClipText
        return if (text != null) {
            commitText(text).also { updateLastCommitPosition() }
        } else {
            appContext.showShortToastSync("Failed to paste item.")
            false
        }
    }
```
`performClipboardSelectAll()` (current lines 473+) is untouched — it doesn't use `ClipboardItem`/`clipboardManager` at all.

- [ ] **Step 5: Remove the `ClipboardMediaProvider` manifest entry**

In `app/src/main/AndroidManifest.xml`, delete (current lines 133-139):
```xml
        <!-- Clipboard Media File Provider -->
        <provider
            android:name="dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardMediaProvider"
            android:authorities="${applicationId}.provider.clipboard"
            android:grantUriPermissions="true"
            android:exported="false">
        </provider>
```
Leave the `FlorisCopyToClipboardActivity` block above it (lines 121-131) untouched — see decision #4.

- [ ] **Step 6: Remove the `ImeUiMode.CLIPBOARD` route**

In `ime/window/ImeWindow.kt` (post-Task-1 state):
```kotlin
            when (state.imeUiMode) {
                ImeUiMode.TEXT -> TextInputLayout()
                ImeUiMode.CLIPBOARD -> ProvideActualLayoutDirection { ClipboardInputLayout() }
            }
```
New:
```kotlin
            when (state.imeUiMode) {
                ImeUiMode.TEXT -> TextInputLayout()
            }
```
Remove the `CLIPBOARD` entry from `ime/ImeUiMode.kt`'s enum, leaving just `TEXT(0)`.

- [ ] **Step 7: Remove `IME_UI_MODE_CLIPBOARD` and `CLIPBOARD_CLEAR_PRIMARY_CLIP` from `KeyCode.kt` and `TextKeyData.kt`**

`KeyCode.kt`: delete `const val IME_UI_MODE_CLIPBOARD = -213` and the `CLIPBOARD_CLEAR_PRIMARY_CLIP` constant (grep its numeric value, it's near the other `CLIPBOARD_*` constants). Keep `CLIPBOARD_COPY`/`CLIPBOARD_CUT`/`CLIPBOARD_PASTE`/`CLIPBOARD_SELECT_ALL`.

`TextKeyData.kt`: delete the `IME_UI_MODE_CLIPBOARD` object (~421-425) and the `CLIPBOARD_CLEAR_PRIMARY_CLIP` object (~333-337). Keep the four basic clipboard `TextKeyData` objects (~291-321).

- [ ] **Step 8: Remove `IME_UI_MODE_CLIPBOARD` handling from `KeyboardManager.kt`/`ComputingEvaluator.kt`/`PopupUiController.kt`**

`KeyboardManager.kt`: delete `KeyCode.IME_UI_MODE_CLIPBOARD -> activeState.imeUiMode = ImeUiMode.CLIPBOARD` (~742) and `SwipeAction.SWITCH_TO_CLIPBOARD_CONTEXT -> TextKeyData.IME_UI_MODE_CLIPBOARD` (~271).

`ComputingEvaluator.kt`: delete the `KeyCode.IME_UI_MODE_CLIPBOARD -> { Icons.AutoMirrored.Outlined.Assignment }` branch (~262-264).

`PopupUiController.kt`: delete `KeyCode.IME_UI_MODE_CLIPBOARD,` from `ExceptionsForKeyCodes` (~line 68).

- [ ] **Step 9: Trim the quick-action row**

`QuickAction.kt`: remove the `IME_UI_MODE_CLIPBOARD`/`CLIPBOARD_CLEAR_PRIMARY_CLIP` lines from `computeDisplayName`/`computeTooltip`. Keep the `CLIPBOARD_COPY`/`CUT`/`PASTE`/`SELECT_ALL` lines untouched.

`QuickActionArrangement.kt`: from the `Default` list, remove:
```kotlin
                QuickAction.InsertKey(TextKeyData.IME_UI_MODE_CLIPBOARD),
                QuickAction.InsertKey(TextKeyData.CLIPBOARD_CLEAR_PRIMARY_CLIP),
```
Keep the four `CLIPBOARD_COPY`/`CUT`/`PASTE`/`SELECT_ALL` `QuickAction.InsertKey(...)` lines.

- [ ] **Step 10: Remove the `SWITCH_TO_CLIPBOARD_CONTEXT` gesture action**

`SwipeAction.kt`: remove the `SWITCH_TO_CLIPBOARD_CONTEXT,` enum entry (line 48). `EnumDisplayEntries.kt`: remove its matching gesture-display entry (search `SWITCH_TO_CLIPBOARD_CONTEXT`, ~619-620 pre-Task-1 numbering).

- [ ] **Step 11: Remove `ClipboardSyncBehavior` display entries and the clipboard prefs group**

`EnumDisplayEntries.kt`: remove the `ClipboardSyncBehavior::class` block (~108-128 pre-Task-1 numbering — read the file first).

`AppPrefs.kt`: delete the entire `val clipboard = Clipboard()` / `inner class Clipboard { ... }` block (lines 78-157, the 15 prefs listed in the investigation). Delete `devtools.showPrimaryClip` (lines 185-188) — see Step 14 for its one consumer. Delete the clipboard-related migration shims (`suggestion__clipboard_content_*`, `clipboard__*` renames — read the file first, investigation found them at ~788-899 pre-Task-1 numbering).

- [ ] **Step 12: Remove the Clipboard settings screen, its route, and its home-screen entry**

In `app/Routes.kt`: remove `import dev.patrickgold.florisboard.app.settings.clipboard.ClipboardScreen` (line 60), the `@Deeplink("settings/clipboard") object Clipboard` block (lines 168-169), and `composableWithDeepLink(Settings.Clipboard::class) { ClipboardScreen() }` (line 313).

In `app/settings/HomeScreen.kt`: remove the Clipboard row (lines 138-141, navigating to `Routes.Settings.Clipboard` with `Icons.AutoMirrored.Outlined.Assignment`).

- [ ] **Step 13: Remove the clipboard suggestion provider from the smartbar**

`ime/nlp/NlpManager.kt`: read the file, remove the `ClipboardSuggestionProvider` inner class and its registration (investigation locates it at lines 57/63/91/97/259/281/349-450).

`ime/nlp/SuggestionCandidate.kt`: remove `ClipboardSuggestionCandidate` (lines 28, 124-125).

`ime/smartbar/CandidatesRow.kt`: remove the `ClipboardSuggestionCandidate` import and its styling branch (lines 43, 150).

- [ ] **Step 14: Fix the devtools "show primary clip" overlay**

Read `app/devtools/DevtoolsOverlay.kt`. It currently reads `clipboardManager.primaryClipFlow` (a `StateFlow<ClipboardItem?>`) gated by `prefs.devtools.showPrimaryClip`. Since Step 11 removes that pref and the new `ClipboardManager` exposes `primaryClipTextFlow: StateFlow<String?>` instead, either:
- (a) delete the "show primary clip" overlay section entirely (simplest — it's a debug-only nicety, not requested functionality), or
- (b) repoint it to `clipboardManager.primaryClipTextFlow` and display the raw string instead of a `ClipboardItem`.
Default to (a) for minimalism unless the user wants devtools clipboard inspection kept.

- [ ] **Step 15: Confirm `AboutScreen.kt` and `ExportDebugLogScreen.kt` still compile unchanged**

Both call `clipboardManager.addNewPlaintext(someString)` — the rewritten `ClipboardManager` (Step 2) keeps that exact method name and signature, so no edit should be needed. Build will confirm.

- [ ] **Step 16: Remove clipboard-history backup/restore sections**

Read `app/settings/advanced/BackupScreen.kt` and `app/settings/advanced/RestoreScreen.kt` (both mix clipboard-history backup with unrelated prefs/theme/keyboard backup, per investigation ~196-384 and ~180-231). Remove only the clipboard-history-specific sections (the ones importing `ime.clipboard.provider.*`), leaving prefs/theme/keyboard backup-restore intact.

- [ ] **Step 17: Build and fix any compile errors from missed references**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`. Fix any unresolved-reference errors the compiler surfaces.

- [ ] **Step 18: Install and manually verify on a device/emulator**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

Expected:
- No clipboard-history panel is reachable from any key or quick action.
- Selecting text and using the copy/cut/paste/select-all quick-action buttons still works (copy → paste round-trips through the system clipboard).
- Settings home screen no longer shows a "Clipboard" row.
- About screen's "copy app version" button and the debug-log export's "copy" button still work.

- [ ] **Step 19: Commit**

```bash
git add -A -- app/src/main/kotlin/dev/patrickgold/florisboard/ime/clipboard app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/clipboard
git add app/src/main/AndroidManifest.xml app/src/main/kotlin/dev/patrickgold/florisboard/FlorisApplication.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/editor/EditorInstance.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/window/ImeWindow.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/ImeUiMode.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/key/KeyCode.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyData.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/ComputingEvaluator.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/popup/PopupUiController.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickAction.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickActionArrangement.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/gestures/SwipeAction.kt app/src/main/kotlin/dev/patrickgold/florisboard/app/EnumDisplayEntries.kt app/src/main/kotlin/dev/patrickgold/florisboard/app/AppPrefs.kt app/src/main/kotlin/dev/patrickgold/florisboard/app/Routes.kt app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/HomeScreen.kt app/src/main/kotlin/dev/patrickgold/florisboard/app/devtools/DevtoolsOverlay.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/NlpManager.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/nlp/SuggestionCandidate.kt app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/CandidatesRow.kt app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/advanced/BackupScreen.kt app/src/main/kotlin/dev/patrickgold/florisboard/app/settings/advanced/RestoreScreen.kt
git commit -m "feat: remove clipboard history subsystem, shrink ClipboardManager to raw system-clipboard bridge"
```

---

### Task 3: Remaining special-char dead-code cleanup

Finishes the scope note left by the prior plan (`docs/superpowers/plans/2026-09-08-minimal-keyboard-layout.md` lines 156-160): unreachable `view_symbols` keys still sitting in layouts nothing can navigate to anymore, plus gesture-cycle branches that could still jump to Symbols mode.

**Files:**
- Modify: `app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/symbols2Mod/cjk.json`
- Modify: `app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/symbols2Mod/default.json`
- Modify: `app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/numericAdvanced/bengali.json`
- Modify: `app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/numericAdvanced/western_arabic.json`
- Modify: `app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/numericAdvanced/western_arabic_pc.json`
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt` (`CYCLE_TO_PREVIOUS_KEYBOARD_MODE`/`CYCLE_TO_NEXT_KEYBOARD_MODE` branches, lines 243-254)
- Modify: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/gestures/SwipeAction.kt` (remove `CYCLE_TO_PREVIOUS_KEYBOARD_MODE`/`CYCLE_TO_NEXT_KEYBOARD_MODE` if this was the only remaining reachability path — verify first, see Step 2)

- [ ] **Step 1: Remove the dead `view_symbols` key from each layout file**

Each of the 5 files has a line matching `{"code": -202, "label": "view_symbols", ...}` inside a modifier row that's otherwise identical in shape to the `charactersMod/default.json` edit already done in the prior plan. Read each file first (they're small, ~40-50 lines), remove that one key object and its trailing/leading comma exactly as `charactersMod/default.json` was edited previously, and re-validate with:
```bash
python -m json.tool "app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/symbols2Mod/cjk.json"
python -m json.tool "app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/symbols2Mod/default.json"
python -m json.tool "app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/numericAdvanced/bengali.json"
python -m json.tool "app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/numericAdvanced/western_arabic.json"
python -m json.tool "app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/numericAdvanced/western_arabic_pc.json"
```
Expected: all 5 pretty-print with no error.

- [ ] **Step 2: Decide the fate of `CYCLE_TO_PREVIOUS_KEYBOARD_MODE`/`CYCLE_TO_NEXT_KEYBOARD_MODE`**

These are gesture actions (assignable in Settings → Gestures, not bound by default) that let a user cycle `CHARACTERS → SYMBOLS → SYMBOLS2 → NUMERIC_ADVANCED → CHARACTERS` regardless of what keys are on screen. With no default binding, they're not reachable without the user deliberately opting in via a gesture setting — but since Symbols is otherwise fully unreachable from the Characters keyboard, leaving this gesture bound would re-open exactly the door "no special characters" is meant to close, for a user who assigns it. Remove both `KeyboardMode.SYMBOLS`/`KeyboardMode.SYMBOLS2` branches from the `when` in `KeyboardManager.kt` (lines 243-254), collapsing the cycle to skip directly between `CHARACTERS` and `NUMERIC_ADVANCED`:
```kotlin
            SwipeAction.CYCLE_TO_PREVIOUS_KEYBOARD_MODE -> when (activeState.keyboardMode) {
                KeyboardMode.CHARACTERS -> TextKeyData.VIEW_NUMERIC_ADVANCED
                else -> TextKeyData.VIEW_CHARACTERS
            }
            SwipeAction.CYCLE_TO_NEXT_KEYBOARD_MODE -> when (activeState.keyboardMode) {
                KeyboardMode.CHARACTERS -> TextKeyData.VIEW_NUMERIC_ADVANCED
                else -> TextKeyData.VIEW_CHARACTERS
            }
```
This keeps the two gesture actions available for cycling to the numeric-advanced keyboard (still useful, still reachable via its own key) while closing the Symbols/Symbols2 loophole. The `SwipeAction` enum entries themselves stay (they still do something valid) — only the `when` branches inside them change.

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Manually verify on a device/emulator**

If you have a gesture bound to "Cycle to next/previous keyboard mode" (Settings → Gestures), trigger it repeatedly from the Characters keyboard. Expected: it toggles between the letters keyboard and the numeric-advanced keyboard only — it never lands on a `!@#$%^&*` Symbols page.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/symbols2Mod/cjk.json app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/symbols2Mod/default.json app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/numericAdvanced/bengali.json app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/numericAdvanced/western_arabic.json app/src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/numericAdvanced/western_arabic_pc.json app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt
git commit -m "chore: remove remaining dead view_symbols keys and close the gesture-cycle loophole to Symbols"
```

---

## Explicitly out of scope (per the prior plan and this one)

- Word suggestions / dictionary / NLP core (`ime/dictionary/`, the Room user-dictionary DB, autocorrect) — untouched.
- Multi-language support and the theme/extension store — kept, per the user's prior answer.
- `FlorisCopyToClipboardActivity` (share-target "copy image to clipboard") — kept; flag if this should go too (decision #4).
- Caps/shift toggle and font-size adjustment — already verified working in the prior plan, untouched here.
