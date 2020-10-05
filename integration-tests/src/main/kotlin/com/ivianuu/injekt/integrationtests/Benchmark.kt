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
