package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import org.junit.Test

class DivergenceTest {

    @Test
    fun testWithGiven() = codegen(
        """
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

            @Given fun <T> unwrapped(@Given wrapped: Wrapper<T>): T = wrapped.value

            fun lol() {
                given<Foo>()
            }
        """
    ) {
        assertCompileError("divergent")
    }

    @Test
    fun testUnresolvableDivergenceWithQualifiers() = codegen(
        """
            @Given fun <T> unwrapped(@Given qualified: @Qualifier1 T): T = qualified
            fun lol() {
                given<Foo>()
            }
        """
    ) {
        assertCompileError("divergent")
    }

    @Test
    fun testUnresolvableDivergenceWithProvidersAndQualifiers() = codegen(
        """
            @Given fun <T> any1(@Given t: () -> @Qualifier1 T): T = t()
            @Given fun <T> any2(@Given t: () -> @Qualifier2("a") T): T = t()
            fun invoke() = given<String>()
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

            @Given fun <T> unwrapped(@Given wrapped: Wrapper<T>): T = wrapped.value

            @Given fun fooWrapper(): Wrapper<Wrapper<Foo>> = error("")

            fun lol() {
                given<Foo>()
            }
        """
    )

    @Test
    fun testResolvableDivergenceWithQualifiers() = codegen(
        """
            @Given fun <T> unwrapped(@Given qualified: @Qualifier1 T): T = qualified

            @Given fun qualifiedFoo(): @Qualifier1 Foo = error("")

            fun lol() {
                given<Foo>()
            }
        """
    )

    @Test
    fun testCircularDependencyFails() = codegen(
        """
            @Given class A(@Given b: B)
            @Given class B(@Given a: A)
            fun invoke() = given<A>()
        """
    ) {
        assertCompileError("divergent")
    }

    @Test
    fun testSelfDependencyFails() = codegen(
        """
            @Given fun <T> anyFromStream(@Given t: T): T = t
            fun invoke() = given<String>()
        """
    ) {
        assertCompileError("divergent")
    }

    @Test
   fun testProviderBreaksCircularDependency() = codegen(
       """
            @Given class A(@Given b: B)
            @Given class B(@Given a: () -> A)
            fun invoke() = given<B>()
       """
   ) {
       invokeSingleFile()
    }

   @Test
   fun testIrrelevantProviderInChainDoesNotBreakCircularDependency() = codegen(
       """
            @Given class A(@Given b: () -> B)
            @Given class B(@Given b: C)
            @Given class C(@Given b: B)
            fun invoke() = given<C>()
       """
   ) {
       assertCompileError("divergent")
   }

   @Test
   fun testLazyRequestInSetBreaksCircularDependency() = codegen(
       """
            typealias A = () -> Unit
            @Given fun a(@Given b: () -> B): A = {}
            @GivenSetElement fun aIntoSet(@Given a: A): () -> Unit = a
            typealias B = () -> Unit
            @Given fun b(@Given a: () -> A): B = {}
            @GivenSetElement fun bIntoSet(@Given b: B): () -> Unit = b
            fun invoke() = given<Set<() -> Unit>>()
       """
   ) {
       invokeSingleFile()
   }

}