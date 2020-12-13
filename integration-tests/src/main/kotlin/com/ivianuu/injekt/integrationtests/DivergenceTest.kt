package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class DivergenceTest {

    @Test
    fun testWithGiven() = codegen(
        """
            inline fun <A, B, R> withGiven(a: A, b: B, block: (@Given WithGiven1<A, B>) -> R) = block(WithGiven1(a, b)) 

            fun invoke() = withGiven("", 0) {
                given<String>()
                given<Int>()
            }
        """
    )

    @Test
    fun testUnresolvableDivergence() = codegen(
        """
                interface Wrapper<T> {
                    val value: T
                }

                @Given fun <T> unwrapped(wrapped: Wrapper<T> = given): T = wrapped.value

                fun lol() {
                    given<Foo>()
                }
        """
    ) {
        assertCompileError("divergent")
    }

    @Test
    fun testResolvableDivergence() = codegen(
        """
                interface Wrapper<T> {
                    val value: T
                }

                @Given fun <T> unwrapped(wrapped: Wrapper<T> = given): T = wrapped.value

                @Given fun fooWrapper(): Wrapper<Wrapper<Foo>> = error("")

                fun lol() {
                    given<Foo>()
                }
        """
    )

}