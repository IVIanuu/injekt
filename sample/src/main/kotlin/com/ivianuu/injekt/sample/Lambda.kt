/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Distinct
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given

@Distinct
typealias HelloMsg = String
@Distinct
typealias ByeMsg = String

@Reader
fun main() {
    print { "${given<HelloMsg>()}$it" }
    print { "${given<ByeMsg>()}$it" }
}

@Reader
fun print(
    block: @Reader (String) -> String
) {
    println(block("a"))
    println(block("b"))
}
