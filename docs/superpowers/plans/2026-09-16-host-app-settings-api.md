# Expose a host-app API to update theme and font size

## Context

The Theme and Addons settings screens were deliberately deleted earlier this session as part of locking this keyboard down for kiosk deployment — there's now no in-app UI left for a user to change theme or font size. The `Keyboard` settings screen (font size slider) is the sole survivor, but operators on a locked MDM device aren't expected to touch Settings at all. ezPigmy (`io.vittam.ezpigmy`, a separate installed APK, not a library dependency) can now push theme and font-size changes into the keyboard programmatically instead.

Investigation established:
- **Both preferences are fully reactive** — `prefs.theme.dayThemeId`/`nightThemeId` (`ThemeManager.kt:106-115`) and `prefs.keyboard.fontSizeMultiplierPortrait` (`ImeWindowController.kt:117-146`) are `StateFlow`-backed; a write takes effect live while the keyboard is open, no restart needed.
- **`FlorisPreferenceStore`** (`app/AppPrefs.kt:49`) is a process-wide singleton, reachable from any class in the process — including a `BroadcastReceiver`. `.set()` is a `suspend fun`, so writes need a coroutine.
- **There was no existing cross-app IPC surface in this app** — no exported `ContentProvider`, no manifest-declared `BroadcastReceiver`, no custom `<permission>`. This was the first.
- **Neither app had real release signing configured** at the time this was built (`ezStackflorisboard`'s `release`/`beta` build types have no `signingConfig`; ezPigmy's `release` currently placeholders with the local machine's debug keystore, explicitly TODO'd). The `signature`-level permission is declared anyway — both apps' *debug* builds happen to share the local machine's default debug keystore, so it's already enforceable between debug builds; production enforcement needs both apps' real release signing sorted first (a pre-existing, separately-tracked gap).
- **Theme selection is "pick among 12 built-in ids"** — 6 in `org.florisboard.themes` (`floris_day[_borderless]`, `floris_night[_borderless]`, `floris_pure_night[_borderless]`) + 6 Material-You variants in `org.florisboard.themes.my`. Arbitrary custom `.flex` theme import is dead code (handler already gutted) — out of scope.
- **ezPigmy's device is portrait-only** — only `fontSizeMultiplierPortrait` is exposed; landscape is unreachable hardware-wise and left untouched.

**Confirmed choices:** write-only (`BroadcastReceiver`, not a `ContentProvider`); theme scope is just the theme ID (not mode/accent color); font size is a single value applied to portrait only; permission is `signature`-level.

**Design decision:** the API takes one theme ID and writes it to **both** `dayThemeId` and `nightThemeId` in one update. Since `theme.mode` isn't part of this API's scope, setting only the day theme would leave the night theme mismatched whenever `mode` resolves to night (system dark mode, or `FOLLOW_TIME` after sunset). Writing both makes the requested theme render unconditionally regardless of `mode`.

---

## What shipped

**Files:**
- `app/src/main/kotlin/dev/patrickgold/florisboard/ime/api/EzPigmyKeyboardApi.kt` — the wire contract (action, extras, permission name), documented with the exact `Intent` shape a host app must send.
- `app/src/main/kotlin/dev/patrickgold/florisboard/ime/api/SettingsUpdateReceiver.kt` — the receiver.
- `app/src/main/kotlin/dev/patrickgold/florisboard/FlorisApplication.kt` — added a one-line `Context.preferenceStoreLoaded()` accessor, matching the file's existing `Context.themeManager()`-style pattern, since `preferenceStoreLoaded` had no public accessor yet.
- `app/src/main/AndroidManifest.xml` — a `<permission android:protectionLevel="signature">` declaration plus the `<receiver>` registration.

`SettingsUpdateReceiver.onReceive()`: validates both extras, applies only the ones present, and ignores (with a `flogWarning`, no crash) anything invalid — an unparseable/unknown theme id, or a font size outside 50..150. It calls `goAsync()` and does the real work in a short-lived `CoroutineScope(Dispatchers.IO + SupervisorJob())`, finishing the `PendingResult` in a `finally` block. No response is sent back to the caller (write-only, per the design choice).

**Two race conditions handled**, both because a broadcast can cold-start the app process:
1. `FlorisPreferenceStore.initAndroid()` loads from disk asynchronously (`FlorisApplication.preferenceStoreLoaded: MutableStateFlow<Boolean>`) — the receiver awaits it (bounded, 5s timeout) before writing, so an early write isn't silently discarded once the real load completes.
2. `ExtensionManager.init()` (`lib/ext/ExtensionManager.kt:110-116`) is fire-and-forget — `fun init() { ioScope.launch { keyboardExtensions.init(); themes.init(); ... } }`, not suspend — so the theme index can still be empty when the receiver's validation runs right after a cold start. **This was found via on-device testing, not anticipated in the original plan**: the theme write was silently rejected as "invalid" on a fresh process start because `extensionManager.themes.value` was still `emptyList()`. Fixed by awaiting `themes.filter { it.isNotEmpty() }.first()` (bounded, 3s timeout) before checking membership.

## Verified on-device (not just build-clean)

- **The original plan assumed `adb shell am broadcast` (running as `shell`) is exempt from `signature`-level permission checks. This was wrong** — confirmed via `dumpsys activity broadcasts history`, which showed an explicit `Permission Denial: ... requires io.vittam.ezpigmy.keyboard.permission.UPDATE_SETTINGS` for a shell-sent broadcast. A `signature`-level permission blocks *any* unsigned/mismatched-signature sender, `shell` included — this is the security gate working correctly, not a bug.
- To verify the receiver's internal logic in isolation, the `android:permission` attribute was temporarily removed from the manifest, rebuilt, and tested via `adb shell am broadcast --include-stopped-packages`, then restored once confirmed working. Both the preference-load race and the theme-index race above were caught this way.
- Confirmed end-to-end by reading the JetPref datastore file directly off the device (`adb shell run-as io.vittam.ezpigmy.keyboard.debug cat .../jetpref_datastore/florisboard-app-prefs.jetpref`):
  - Valid `theme_id` + `font_size_percent` → both `theme__day_theme_id`/`theme__night_theme_id` and `keyboard__font_size_multiplier_portrait` updated correctly; `..._landscape` left untouched.
  - Invalid theme id + out-of-range font size → both silently ignored (logged via `flogWarning`), no crash, prior valid values preserved.
- Confirmed the `signature`-level permission is back in effect after restoring it (repeat `shell` broadcast → `Permission Denial` again via `dumpsys`).

## Explicitly out of scope

- Any change to the `ezStackEzPigmyAndroidApp` repo (adding the `<uses-permission>` tag, building the sending `Intent`, wiring it into ezPigmy's own settings/admin flow) — a separate task in a separate repo, picked up once this side is merged and reviewed.
- Making the permission enforceable in *production* — depends on both apps getting real release signing (a pre-existing, separately-tracked gap on both sides).
- Read/query support (`ContentProvider`) — deliberately not built, per the write-only choice.
- `theme.mode`, `accentColor`, `sunriseTime`/`sunsetTime`, or `fontSizeMultiplierLandscape` — deliberately not exposed.
