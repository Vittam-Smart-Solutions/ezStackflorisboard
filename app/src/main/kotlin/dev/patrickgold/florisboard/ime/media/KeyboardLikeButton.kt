/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.media

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import dev.patrickgold.florisboard.ime.input.InputEventDispatcher
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox

/**
 * Generic keyboard-styled button used outside the regular text keyboard (e.g. by the clipboard
 * panel header) to dispatch a [KeyData] key press with the same press/ripple/haptic behavior as a
 * normal keyboard key.
 */
@Composable
internal fun KeyboardLikeButton(
    modifier: Modifier = Modifier,
    inputEventDispatcher: InputEventDispatcher,
    keyData: KeyData,
    elementName: String = FlorisImeUi.MediaEmojiKey.elementName,
    content: @Composable () -> Unit,
) {
    val inputFeedbackController = LocalInputFeedbackController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val selector = if (isPressed) {
        SnyggSelector.PRESSED
    } else {
        SnyggSelector.NONE
    }

    SnyggBox(
        elementName = elementName,
        attributes = mapOf(FlorisImeUi.Attr.Code to keyData.code),
        selector = selector,
        clickAndSemanticsModifier = modifier
            .indication(interactionSource, ripple())
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false).also {
                        if (it.pressed != it.previousPressed) it.consume()
                    }
                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    inputEventDispatcher.sendDown(keyData)
                    inputFeedbackController.keyPress(keyData)
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        interactionSource.tryEmit(PressInteraction.Release(press))
                        inputEventDispatcher.sendUp(keyData)
                    } else {
                        interactionSource.tryEmit(PressInteraction.Cancel(press))
                        inputEventDispatcher.sendCancel(keyData)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
