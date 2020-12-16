package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertTrue
import org.junit.Test

class ProviderTest {

    @Test
    fun testProviderGiven() = codegen(
        """
            @Given val foo = Foo()
            fun invoke(): Foo {
                return given<() -> Foo>()()
            }
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testProviderWithArgsGiven() = codegen(
        """
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            fun invoke(): Bar {
                return given<(Foo) -> Bar>()(Foo())
            }
        """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testSuspendProviderGiven() = codegen(
        """
            @Given suspend fun foo() = Foo()
            fun invoke(): Foo = runBlocking { given<suspend () -> Foo>()() }
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testComposableProviderGiven() = codegen(
        """
            @Given @Composable val foo: Foo get() = Foo()
            fun invoke() {
                given<@Composable () -> Foo>()
            }
        """
    ) {
        invokeSingleFile()
    }

}