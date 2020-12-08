package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertNull
import org.junit.Test

class ProviderTest {

    @Test
    fun testProvider() = codegen(
        """
            @Binding fun foo() = Foo()
            fun invoke() {
                create<() -> Foo>()()
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testSuspendProvider() = codegen(
        """
            @Binding suspend fun foo() = Foo()
            fun invoke() {
                runBlocking { create<suspend () -> Foo>()() }
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testComposableProvider() = codegen(
        """
            @Composable @Binding fun foo() = Foo()

            fun invoke() {
                create<@Composable () -> Foo>()
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testNullableProviderMissingBinding() = codegen(
        """
            @Component interface ProviderComponent {
                val fooFactory: () -> Foo?
            }

            fun invoke() = create<ProviderComponent>().fooFactory()
        """
    ) {
        assertNull(invokeSingleFile())
    }

}
