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

package dev.patrickgold.florisboard.ime.api

/**
 * Contract for the host-app settings-update API. A host app (e.g. ezPigmy) pushes theme/font-size
 * changes by sending a broadcast shaped like this:
 *
 * ```kotlin
 * Intent(EzStackKeyboardApi.ACTION_UPDATE_SETTINGS).apply {
 *     setPackage("io.vittam.ezstack.keyboard")
 *     putExtra(EzStackKeyboardApi.EXTRA_THEME_ID, "org.florisboard.themes:floris_night_borderless")
 *     putExtra(EzStackKeyboardApi.EXTRA_FONT_SIZE_PERCENT, 120)
 * }.also { intent ->
 *     context.sendBroadcast(intent, EzStackKeyboardApi.PERMISSION_UPDATE_SETTINGS)
 * }
 * ```
 *
 * Both extras are optional - omit one to leave that setting unchanged. The sending app's own
 * manifest must declare `<uses-permission android:name="io.vittam.ezstack.keyboard.permission.UPDATE_SETTINGS"/>`
 * and be signed with the same certificate as this app for the broadcast to be delivered
 * (`signature`-level permission - see AndroidManifest.xml).
 *
 * Valid [EXTRA_THEME_ID] values (full `extensionId:componentId` form) - setting one applies it to
 * both the day and night theme, so it renders unconditionally regardless of theme mode:
 * - org.florisboard.themes:floris_day / floris_day_borderless
 * - org.florisboard.themes:floris_night / floris_night_borderless
 * - org.florisboard.themes:floris_pure_night / floris_pure_night_borderless
 * - org.florisboard.themes.my:floris_day_my / floris_day_my_borderless
 * - org.florisboard.themes.my:floris_night_my / floris_night_my_borderless
 * - org.florisboard.themes.my:floris_pure_night_my / floris_pure_night_my_borderless
 *
 * [EXTRA_FONT_SIZE_PERCENT] must be an Int in [FONT_SIZE_MIN]..[FONT_SIZE_MAX] (matches the
 * existing Settings slider range) and only affects the portrait font size, since the host device
 * is portrait-only. Out-of-range or malformed values are ignored.
 */
object EzStackKeyboardApi {
    private const val APP_ID = "io.vittam.ezstack.keyboard"
    const val ACTION_UPDATE_SETTINGS = "$APP_ID.action.UPDATE_SETTINGS"
    const val PERMISSION_UPDATE_SETTINGS = "$APP_ID.permission.UPDATE_SETTINGS"
    const val EXTRA_THEME_ID = "theme_id"
    const val EXTRA_FONT_SIZE_PERCENT = "font_size_percent"
    const val FONT_SIZE_MIN = 50
    const val FONT_SIZE_MAX = 150
}
