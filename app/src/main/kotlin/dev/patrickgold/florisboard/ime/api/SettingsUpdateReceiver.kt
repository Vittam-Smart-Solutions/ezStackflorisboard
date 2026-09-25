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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.theme.ThemeExtension
import dev.patrickgold.florisboard.lib.devtools.flogWarning
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.preferenceStoreLoaded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Receives [EzStackKeyboardApi.ACTION_UPDATE_SETTINGS] broadcasts from a host app and applies the
 * requested theme/font-size changes. See [EzStackKeyboardApi] for the wire contract.
 */
class SettingsUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != EzStackKeyboardApi.ACTION_UPDATE_SETTINGS) return

        val themeIdRaw = intent.getStringExtra(EzStackKeyboardApi.EXTRA_THEME_ID)
        val fontSizePercent = if (intent.hasExtra(EzStackKeyboardApi.EXTRA_FONT_SIZE_PERCENT)) {
            intent.getIntExtra(EzStackKeyboardApi.EXTRA_FONT_SIZE_PERCENT, 0)
        } else {
            null
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                // The broadcast may have cold-started the process; wait for the preference store
                // to finish loading from disk before writing, so an early write isn't silently
                // discarded once the real load completes.
                withTimeoutOrNull(5000) {
                    context.preferenceStoreLoaded().filter { it }.first()
                }

                val prefs by FlorisPreferenceStore

                if (themeIdRaw != null) {
                    val themeId = ExtensionComponentName.Serializer.deserialize(themeIdRaw)
                    if (themeId != null && isKnownTheme(context, themeId)) {
                        prefs.theme.dayThemeId.set(themeId)
                        prefs.theme.nightThemeId.set(themeId)
                    } else {
                        flogWarning { "SettingsUpdateReceiver: ignoring invalid theme_id \"$themeIdRaw\"" }
                    }
                }

                if (fontSizePercent != null) {
                    if (fontSizePercent in EzStackKeyboardApi.FONT_SIZE_MIN..EzStackKeyboardApi.FONT_SIZE_MAX) {
                        prefs.keyboard.fontSizeMultiplierPortrait.set(fontSizePercent)
                    } else {
                        flogWarning { "SettingsUpdateReceiver: ignoring out-of-range font_size_percent $fontSizePercent" }
                    }
                }
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    private suspend fun isKnownTheme(context: Context, name: ExtensionComponentName): Boolean {
        val extensionManager = context.extensionManager().value
        // ExtensionManager.init() is fire-and-forget (launches asset indexing on its own scope
        // rather than suspending), so a broadcast that cold-started the process can race ahead of
        // the theme index being populated. Wait for it, bounded, same reasoning as the prefs wait
        // above.
        withTimeoutOrNull(3000) {
            extensionManager.themes.filter { it.isNotEmpty() }.first()
        }
        val themeExt = extensionManager.getExtensionById(name.extensionId) as? ThemeExtension
        return themeExt?.themes?.any { it.id == name.componentId } == true
    }
}
