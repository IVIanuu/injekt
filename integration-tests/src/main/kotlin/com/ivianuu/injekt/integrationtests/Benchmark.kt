package com.ivianuu.injekt.integrationtests

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
                        val context = rootContext<TestContext>()
                        context.runReader { given<Fib4>() }
                    }
            """
            )
        ) {
            assertOk()
        }
    }

    //@Test
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
                        val context = rootContext<TestContext>()
                        context.runReader { given<Fib4>() }
                    }
            """
                )
            )
        ) {
            it.last().assertOk()
        }
    }

}