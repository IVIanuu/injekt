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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source

class Benchmark {

    //@Test
    fun testPerformanceInternal() {
        val sources = buildList {
            repeat(4000) { index ->
                add(
                    source(
                        if (index == 0 || index == 1) {
                            """
                                @Binding class Fib${index + 1}
                            """
                        } else {
                            """
                                @Binding
                                class Fib${index + 1}(
                                    val fibM1: Fib$index,
                                    val fibM2: Fib${index - 1}
                                )
                            """
                        }
                    )
                )
            }
        }
        codegen(
            *sources.toTypedArray(),
            source(
                """
                    interface TestComponent {
                        val fib30: Fib30
                    }
                    
                    @RootFactory
                    typealias TestComponentFactory = () -> TestComponent
                    
                    fun invoke() {
                        rootFactory<TestComponentFactory>()().fib30
                    }
            """
            )
        ) {
            assertOk()
        }
    }

    // @Test
    fun testPerformanceExternal() {
        val sources = buildList {
            repeat(4000) { index ->
                add(
                    source(
                        if (index == 0 || index == 1) {
                            """
                                @Binding class Fib${index + 1}
                            """
                        } else {
                            """
                                @Binding
                                class Fib${index + 1}(
                                    val fibM1: Fib$index,
                                    val fibM2: Fib${index - 1}
                                )
                            """
                        }
                    )
                )
            }
        }
        multiCodegen(
            sources,
            listOf(
                source(
                    """
                        interface TestComponent {
                            val fib30: Fib30
                        }
                        
                        @RootFactory
                        typealias TestComponentFactory = () -> TestComponent
                        
                        fun invoke() {
                            rootFactory<TestComponentFactory>()().fib30
                        }
                    """
                )
            )
        ) {
            it.last().assertOk()
        }
    }
}
