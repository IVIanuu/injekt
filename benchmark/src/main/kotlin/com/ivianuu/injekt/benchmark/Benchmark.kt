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

package com.ivianuu.injekt.benchmark

import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class Benchmark {

    @Test
    fun testPerformanceInternal() {
        val sources = buildList {
            repeat(4000) { index ->
                add(
                    source(
                        if (index == 0 || index == 1) {
                            """
                                @Given class Fib${index + 1}
                            """
                        } else {
                            """
                                @Given
                                class Fib${index + 1} {
                                    val fibM1: Fib$index = given()
                                    val fibM2: Fib${index - 1} = given()
                                }
                        """
                        },
                        initializeInjekt = false
                    )
                )
            }
        }
        codegen(
            *sources.toTypedArray(),
            source(
                """
                    fun invoke() {
                        context<TestComponent>().runReader { given<Fib4>() }
                    }
            """
            )
        ) {
            assertOk()
        }
    }

    @Test
    fun testPerformanceExternal() {
        val sources = buildList {
            repeat(4000) { index ->
                add(
                    source(
                        if (index == 0 || index == 1) {
                            """
                                @Given class Fib${index + 1}
                            """
                        } else {
                            """
                                @Given
                                class Fib${index + 1} {
                                    val fibM1: Fib$index = given()
                                    val fibM2: Fib${index - 1} = given()
                                }
                        """
                        },
                        initializeInjekt = false
                    )
                )
            }
        }
        multiCodegen(
            sources,
            listOf(
                source(
                    """
                    fun invoke() {
                        context<TestComponent>().runReader { given<Fib4>() }
                    }
            """
                )
            )
        ) {
            it.last().assertOk()
        }
    }

}
