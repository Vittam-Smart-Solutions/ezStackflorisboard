/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.computeImageVector
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggIcon

/**
 * Full-width confirm bar below the numeric keypad grid, which has no room for an
 * in-grid enter key. Delegates to the same `EditorInstance.performEnterAction` path a
 * real Enter key tap uses, so Done/Search/Send/etc. handling isn't duplicated here.
 */
@Composable
fun NumericConfirmBar(
    evaluator: ComputingEvaluator,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val editorInstance by context.editorInstance()

    SnyggButton(
        FlorisImeUi.ConfirmBar.elementName,
        onClick = { editorInstance.performEnterAction(evaluator.editorInfo.imeOptions.action) },
        modifier = modifier.fillMaxWidth(),
        // SnyggButton defaults this to ButtonDefaults.ContentPadding (Material's ~8dp
        // vertical button padding), a separate layer on top of the "padding" stylesheet
        // property below. Zeroing it here so the stylesheet is the only thing sizing this.
        contentPadding = PaddingValues(0.dp),
    ) {
        // SnyggButton's own Row shrink-wraps its content instead of filling the button's
        // width, so centering has to happen here, not via the Row's horizontalArrangement.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SnyggIcon(
                imageVector = evaluator.computeImageVector(TextKeyData.ENTER)
                    ?: Icons.AutoMirrored.Filled.KeyboardReturn,
                contentDescription = null,
            )
        }
    }
}
