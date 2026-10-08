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

package dev.patrickgold.florisboard.ime.keyboard

import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.lib.io.loadJsonAsset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import java.io.File

private const val NUMERIC_LAYOUT_PATH =
    "src/main/assets/ime/keyboard/org.florisboard.layouts/layouts/numeric/western_arabic.json"

class NumericLayoutTest : FunSpec({
    val jsonStr = File(NUMERIC_LAYOUT_PATH).readText()
    val arrangement = loadJsonAsset<LayoutArrangement>(jsonStr).getOrThrow()

    test("numeric layout is a 4x3 grid") {
        arrangement shouldHaveSize 4
        arrangement.forEach { row -> row shouldHaveSize 3 }
    }

    test("numeric layout has exactly the digits, decimal point, and backspace") {
        val codes = arrangement.flatten().map { (it as TextKeyData).code }
        codes.shouldContainExactly(49, 50, 51, 52, 53, 54, 55, 56, 57, 46, 48, -7)
        // Regressions this guards against: the old grid had minus (45), space (32),
        // and an in-grid enter key (10) - all now dropped in favor of NumericConfirmBar.
        codes shouldNotContain 45
        codes shouldNotContain 32
        codes shouldNotContain 10
    }
})
