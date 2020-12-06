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

package com.ivianuu.injekt.samples.comparison.fibonacci

const val FIB_COUNT = 400

fun main() {
    println(
        buildString {
            (1..FIB_COUNT).forEach { index ->
                if (index == 1 || index == 2) {
                    appendLine(
                        """
                        @Binding class Fib$index @Inject constructor()
                        """
                    )
                } else {
                    appendLine(
                        """
                        @Binding class Fib$index @Inject constructor(
                            val fibM1: Fib${index - 1},
                            val fibM2: Fib${index - 2}
                        )
                        """
                    )
                }
            }
        }
    )
}
