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
 * buttons elsewhere in the app (About screen, debug log export).
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
