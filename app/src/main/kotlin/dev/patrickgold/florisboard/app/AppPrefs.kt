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

package dev.patrickgold.florisboard.app

import androidx.compose.ui.graphics.Color
import dev.patrickgold.florisboard.app.settings.theme.ColorPreferenceSerializer
import dev.patrickgold.florisboard.app.settings.theme.DisplayKbdAfterDialogs
import dev.patrickgold.florisboard.app.settings.theme.SnyggLevel
import dev.patrickgold.florisboard.app.setup.NotificationPermissionState
import dev.patrickgold.florisboard.ime.core.DisplayLanguageNamesIn
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.input.CapitalizationBehavior
import dev.patrickgold.florisboard.ime.input.HapticVibrationMode
import dev.patrickgold.florisboard.ime.input.InputFeedbackActivationMode
import dev.patrickgold.florisboard.ime.keyboard.IncognitoMode
import dev.patrickgold.florisboard.ime.keyboard.SpaceBarMode
import dev.patrickgold.florisboard.ime.landscapeinput.LandscapeInputUiMode
import dev.patrickgold.florisboard.ime.nlp.SpellingLanguageMode
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.KeyHintConfiguration
import dev.patrickgold.florisboard.ime.text.key.KeyHintMode
import dev.patrickgold.florisboard.ime.text.key.UtilityKeyAction
import dev.patrickgold.florisboard.ime.theme.ThemeMode
import dev.patrickgold.florisboard.ime.theme.extCoreTheme
import dev.patrickgold.florisboard.ime.window.ImeWindowConfig
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.lib.util.VersionName
import dev.patrickgold.jetpref.datastore.annotations.Preferences
import dev.patrickgold.jetpref.datastore.jetprefDataStoreOf
import dev.patrickgold.jetpref.datastore.model.LocalTime
import dev.patrickgold.jetpref.datastore.model.PreferenceMigrationEntry
import dev.patrickgold.jetpref.datastore.model.PreferenceModel
import dev.patrickgold.jetpref.material.ui.ColorRepresentation

val FlorisPreferenceStore = jetprefDataStoreOf(FlorisPreferenceModel::class)

@Preferences
abstract class FlorisPreferenceModel : PreferenceModel() {
    companion object {
        const val NAME = "florisboard-app-prefs"
    }

    val correction = Correction()
    inner class Correction {
        val autoCapitalization = boolean(
            key = "correction__auto_capitalization",
            default = true,
        )
        val autoSpacePunctuation = boolean(
            key = "correction__auto_space_punctuation",
            default = false,
        )
        val doubleSpacePeriod = boolean(
            key = "correction__double_space_period",
            default = true,
        )
        val rememberCapsLockState = boolean(
            key = "correction__remember_caps_lock_state",
            default = false,
        )
    }

    val devtools = Devtools()
    inner class Devtools {
        val enabled = boolean(
            key = "devtools__enabled",
            default = false,
        )
        val showInputStateOverlay = boolean(
            key = "devtools__show_input_state_overlay",
            default = false,
        )
        val showSpellingOverlay = boolean(
            key = "devtools__show_spelling_overlay",
            default = false,
        )
        val showInlineAutofillOverlay = boolean(
            key = "devtools__show_inline_autofill_overlay",
            default = false,
        )
        val showKeyTouchBoundaries = boolean(
            key = "devtools__show_touch_boundaries",
            default = false,
        )
        val showDragAndDropHelpers = boolean(
            key = "devtools__show_drag_and_drop_helpers",
            default = false,
        )
        val showWindowResizeHandleBoundaries = boolean(
            key = "devtools__show_window_resize_handle_boundaries",
            default = false,
        )
    }

    val dictionary = Dictionary()
    inner class Dictionary {
        val enableSystemUserDictionary = boolean(
            key = "suggestion__enable_system_user_dictionary",
            default = true,
        )
        val enableFlorisUserDictionary = boolean(
            key = "suggestion__enable_floris_user_dictionary",
            default = true,
        )
    }

    val gestures = Gestures()
    inner class Gestures {
        val swipeUp = enum(
            key = "gestures__swipe_up",
            default = SwipeAction.SHIFT,
        )
        val swipeDown = enum(
            key = "gestures__swipe_down",
            default = SwipeAction.HIDE_KEYBOARD,
        )
        val swipeLeft = enum(
            key = "gestures__swipe_left",
            default = SwipeAction.SWITCH_TO_NEXT_SUBTYPE,
        )
        val swipeRight = enum(
            key = "gestures__swipe_right",
            default = SwipeAction.SWITCH_TO_PREV_SUBTYPE,
        )
        val spaceBarSwipeUp = enum(
            key = "gestures__space_bar_swipe_up",
            default = SwipeAction.NO_ACTION,
        )
        val spaceBarSwipeLeft = enum(
            key = "gestures__space_bar_swipe_left",
            default = SwipeAction.MOVE_CURSOR_LEFT,
        )
        val spaceBarSwipeRight = enum(
            key = "gestures__space_bar_swipe_right",
            default = SwipeAction.MOVE_CURSOR_RIGHT,
        )
        val spaceBarLongPress = enum(
            key = "gestures__space_bar_long_press",
            default = SwipeAction.SHOW_INPUT_METHOD_PICKER,
        )
        val deleteKeySwipeLeft = enum(
            key = "gestures__delete_key_swipe_left",
            default = SwipeAction.DELETE_CHARACTERS_PRECISELY,
        )
        val deleteKeyLongPress = enum(
            key = "gestures__delete_key_long_press",
            default = SwipeAction.DELETE_CHARACTER,
        )
        val swipeDistanceThreshold = int(
            key = "gestures__swipe_distance_threshold",
            default = 32,
        )
        val swipeVelocityThreshold = int(
            key = "gestures__swipe_velocity_threshold",
            default = 1900,
        )
    }

    val glide = Glide()
    inner class Glide {
        val enabled = boolean(
            key = "glide__enabled",
            default = false,
        )
        val showTrail = boolean(
            key = "glide__show_trail",
            default = true,
        )
        val trailDuration = int(
            key = "glide__trail_fade_duration",
            default = 200,
        )
        val showPreview = boolean(
            key = "glide__show_preview",
            default = true,
        )
        val previewRefreshDelay = int(
            key = "glide__preview_refresh_delay",
            default = 150,
        )
        val immediateBackspaceDeletesWord = boolean(
            key = "glide__immediate_backspace_deletes_word",
            default = true,
        )
    }

    val inputFeedback = InputFeedback()
    inner class InputFeedback {
        val audioEnabled = boolean(
            key = "input_feedback__audio_enabled",
            default = true,
        )
        val audioActivationMode = enum(
            key = "input_feedback__audio_activation_mode",
            default = InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS,
        )
        val audioVolume = int(
            key = "input_feedback__audio_volume",
            default = 50,
        )
        val audioFeatKeyPress = boolean(
            key = "input_feedback__audio_feat_key_press",
            default = true,
        )
        val audioFeatKeyLongPress = boolean(
            key = "input_feedback__audio_feat_key_long_press",
            default = false,
        )
        val audioFeatKeyRepeatedAction = boolean(
            key = "input_feedback__audio_feat_key_repeated_action",
            default = false,
        )
        val audioFeatGestureSwipe = boolean(
            key = "input_feedback__audio_feat_gesture_swipe",
            default = false,
        )
        val audioFeatGestureMovingSwipe = boolean(
            key = "input_feedback__audio_feat_gesture_moving_swipe",
            default = false,
        )

        val hapticEnabled = boolean(
            key = "input_feedback__haptic_enabled",
            default = true,
        )
        val hapticActivationMode = enum(
            key = "input_feedback__haptic_activation_mode",
            default = InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS,
        )
        val hapticVibrationMode = enum(
            key = "input_feedback__haptic_vibration_mode",
            default = HapticVibrationMode.USE_VIBRATOR_DIRECTLY,
        )
        val hapticVibrationDuration = int(
            key = "input_feedback__haptic_vibration_duration",
            default = 50,
        )
        val hapticVibrationStrength = int(
            key = "input_feedback__haptic_vibration_strength",
            default = 50,
        )
        val hapticFeatKeyPress = boolean(
            key = "input_feedback__haptic_feat_key_press",
            default = true,
        )
        val hapticFeatKeyLongPress = boolean(
            key = "input_feedback__haptic_feat_key_long_press",
            default = false,
        )
        val hapticFeatKeyRepeatedAction = boolean(
            key = "input_feedback__haptic_feat_key_repeated_action",
            default = true,
        )
        val hapticFeatGestureSwipe = boolean(
            key = "input_feedback__haptic_feat_gesture_swipe",
            default = false,
        )
        val hapticFeatGestureMovingSwipe = boolean(
            key = "input_feedback__haptic_feat_gesture_moving_swipe",
            default = true,
        )
    }

    val internal = Internal()
    inner class Internal {
        val homeIsBetaToolboxCollapsed = boolean(
            key = "internal__home_is_beta_toolbox_collapsed_040a01",
            default = false,
        )
        val isImeSetUp = boolean(
            key = "internal__is_ime_set_up",
            default = false,
        )
        val versionOnInstall = string(
            key = "internal__version_on_install",
            default = VersionName.DEFAULT_RAW,
        )
        val versionLastUse = string(
            key = "internal__version_last_use",
            default = VersionName.DEFAULT_RAW,
        )
        val versionLastChangelog = string(
            key = "internal__version_last_changelog",
            default = VersionName.DEFAULT_RAW,
        )
        val notificationPermissionState = enum(
            key = "internal__notification_permission_state",
            default = NotificationPermissionState.NOT_SET,
        )
    }

    val keyboard = Keyboard()
    inner class Keyboard {
        val windowConfig = custom(
            key = "keyboard__window_config",
            default = emptyMap(),
            serializer = ImeWindowConfig.ByTypeSerializer,
        )
        val numberRow = boolean(
            key = "keyboard__number_row",
            default = true,
        )
        val hintedNumberRowEnabled = boolean(
            key = "keyboard__hinted_number_row_enabled",
            default = true,
        )
        val hintedNumberRowMode = enum(
            key = "keyboard__hinted_number_row_mode",
            default = KeyHintMode.SMART_PRIORITY,
        )
        val hintedSymbolsEnabled = boolean(
            key = "keyboard__hinted_symbols_enabled",
            default = false,
        )
        val hintedSymbolsMode = enum(
            key = "keyboard__hinted_symbols_mode",
            default = KeyHintMode.SMART_PRIORITY,
        )
        val utilityKeyEnabled = boolean(
            key = "keyboard__utility_key_enabled",
            default = true,
        )
        val utilityKeyAction = enum(
            key = "keyboard__utility_key_action",
            default = UtilityKeyAction.SWITCH_LANGUAGE,
        )
        val spaceBarMode = enum(
            key = "keyboard__space_bar_display_mode",
            default = SpaceBarMode.CURRENT_LANGUAGE,
        )
        val capitalizationBehavior = enum(
            key = "keyboard__capitalization_behavior",
            default = CapitalizationBehavior.CAPSLOCK_BY_DOUBLE_TAP,
        )
        val fontSizeMultiplierPortrait = int(
            key = "keyboard__font_size_multiplier_portrait",
            default = 100,
        )
        val fontSizeMultiplierLandscape = int(
            key = "keyboard__font_size_multiplier_landscape",
            default = 100,
        )
        val landscapeInputUiMode = enum(
            key = "keyboard__landscape_input_ui_mode",
            default = LandscapeInputUiMode.DYNAMICALLY_SHOW,
        )
        val keySpacingVertical = int(
            key = "keyboard__key_spacing_vertical",
            default = 100,
        )
        val keySpacingHorizontal = int(
            key = "keyboard__key_spacing_horizontal",
            default = 100,
        )
        val popupEnabled = boolean(
            key = "keyboard__popup_enabled",
            default = true,
        )
        val mergeHintPopupsEnabled = boolean(
            key = "keyboard__merge_hint_popups_enabled",
            default = false,
        )
        val longPressDelay = int(
            key = "keyboard__long_press_delay",
            default = 300,
        )
        val spaceBarSwitchesToCharacters = boolean(
            key = "keyboard__space_bar_switches_to_characters",
            default = true,
        )

        fun keyHintConfiguration(): KeyHintConfiguration {
            return KeyHintConfiguration(
                numberHintMode = when {
                    hintedNumberRowEnabled.get() -> hintedNumberRowMode.get()
                    else -> KeyHintMode.DISABLED
                },
                symbolHintMode = when {
                    hintedSymbolsEnabled.get() -> hintedSymbolsMode.get()
                    else -> KeyHintMode.DISABLED
                },
                mergeHintPopups = mergeHintPopupsEnabled.get(),
            )
        }
    }

    val localization = Localization()
    inner class Localization {
        val displayLanguageNamesIn = enum(
            key = "localization__display_language_names_in",
            default = DisplayLanguageNamesIn.SYSTEM_LOCALE,
        )
        val displayKeyboardLabelsInSubtypeLanguage = boolean(
            key = "localization__display_keyboard_labels_in_subtype_language",
            default = false,
        )
        val activeSubtypeId = long(
            key = "localization__active_subtype_id",
            default = Subtype.DEFAULT.id,
        )
        val subtypes = string(
            key = "localization__subtypes",
            default = "[]",
        )
    }

    val other = Other()
    inner class Other {
        val settingsTheme = enum(
            key = "other__settings_theme",
            default = AppTheme.AUTO,
        )
        val accentColor = custom(
            key = "other__accent_color",
            default = Color.Unspecified,
            serializer = ColorPreferenceSerializer,
        )
        val settingsLanguage = string(
            key = "other__settings_language",
            default = "auto",
        )
        val showAppIcon = boolean(
            key = "other__show_app_icon",
            default = true,
        )
    }

    val physicalKeyboard = PhysicalKeyboard()
    inner class PhysicalKeyboard {
        val showOnScreenKeyboard = boolean(
            key = "physical_keyboard__show_on_screen_keyboard",
            default = false,
        )
    }

    val spelling = Spelling()
    inner class Spelling {
        val languageMode = enum(
            key = "spelling__language_mode",
            default = SpellingLanguageMode.USE_KEYBOARD_SUBTYPES,
        )
        val useContacts = boolean(
            key = "spelling__use_contacts",
            default = true,
        )
        val useUdmEntries = boolean(
            key = "spelling__use_udm_entries",
            default = true,
        )
    }

    val suggestion = Suggestion()
    inner class Suggestion {
        val api30InlineSuggestionsEnabled = boolean(
            key = "suggestion__api30_inline_suggestions_enabled",
            default = true,
        )
        val enabled = boolean(
            key = "suggestion__enabled",
            default = false,
        )
        val blockPossiblyOffensive = boolean(
            key = "suggestion__block_possibly_offensive",
            default = true,
        )
        val incognitoMode = enum(
            key = "suggestion__incognito_mode",
            default = IncognitoMode.DYNAMIC_ON_OFF,
        )
        // Internal pref
        val forceIncognitoModeFromDynamic = boolean(
            key = "suggestion__force_incognito_mode_from_dynamic",
            default = false,
        )
    }

    val theme = Theme()
    inner class Theme {
        val mode = enum(
            key = "theme__mode",
            default = ThemeMode.FOLLOW_SYSTEM,
        )
        val dayThemeId = custom(
            key = "theme__day_theme_id",
            default = extCoreTheme("floris_day"),
            serializer = ExtensionComponentName.Serializer,
        )
        val nightThemeId = custom(
            key = "theme__night_theme_id",
            default = extCoreTheme("floris_night"),
            serializer = ExtensionComponentName.Serializer,
        )
        val accentColor = custom(
            key = "theme__accent_color",
            default = Color.Unspecified,
            serializer = ColorPreferenceSerializer,
        )
        val sunriseTime = localTime(
            key = "theme__sunrise_time",
            default = LocalTime(6, 0),
        )
        val sunsetTime = localTime(
            key = "theme__sunset_time",
            default = LocalTime(18, 0),
        )
        val editorColorRepresentation = enum(
            key = "theme__editor_color_representation",
            default = ColorRepresentation.HEX,
        )
        val editorDisplayKbdAfterDialogs = enum(
            key = "theme__editor_display_kbd_after_dialogs",
            default = DisplayKbdAfterDialogs.REMEMBER,
        )
        val editorLevel = enum(
            key = "theme__editor_level",
            default = SnyggLevel.ADVANCED,
        )
    }

    override fun migrate(entry: PreferenceMigrationEntry): PreferenceMigrationEntry {
        return when (entry.key) {

            // Migrate advanced prefs to other prefs
            // Keep migration rules until: 0.7 dev cycle
            "advanced__settings_theme" -> {
                entry.transform(key = "other__settings_theme")
            }
            "advanced__accent_color" -> {
                entry.transform(key = "other__accent_color")
            }
            "advanced__settings_language" -> {
                entry.transform(key = "other__settings_language")
            }
            "advanced__show_app_icon" -> {
                entry.transform(key = "other__show_app_icon")
            }
            "advanced__incognito_mode" -> {
                entry.transform(key = "suggestion__incognito_mode")
            }
            "advanced__force_incognito_mode_from_dynamic" -> {
                entry.transform(key = "suggestion__force_incognito_mode_from_dynamic")
            }
            //Migrate one hand mode prefs keep until: 0.7 dev cycle
            "keyboard__one_handed_mode" -> {
                if (entry.rawValue == "OFF") {
                    entry.reset()
                } else {
                    entry.keepAsIs()
                }
            }
            // Migrate theme editor fine-tuning
            // Keep migration rule until: 0.6 dev cycle
            "theme__editor_display_colors_as" -> {
                val colorRepresentation = when (entry.rawValue) {
                    "RGBA" -> ColorRepresentation.RGB
                    else -> ColorRepresentation.HEX
                }
                entry.transform(
                    key = "theme__editor_color_representation",
                    rawValue = colorRepresentation.name,
                )
            }

            // Migrate key spacing rules
            // Keep migration rules until: 0.8 dev cycle
            "keyboard__key_spacing_horizontal" -> {
                if (entry.type.isFloat()) {
                    entry.reset()
                } else {
                    entry.keepAsIs()
                }
            }
            "keyboard__key_spacing_vertical" -> {
                if (entry.type.isFloat()) {
                    entry.reset()
                } else {
                    entry.keepAsIs()
                }
            }

            // Default: keep entry
            else -> entry.keepAsIs()
        }
    }
}
