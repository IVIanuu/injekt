package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertNull
import org.junit.Test

class ProviderTest {

    @Test
    fun testProvider() = codegen(
        """
            @Component
            abstract class ProviderComponent {
                abstract val fooFactory: () -> Foo
                @Binding
                protected fun foo() = Foo()
            }

            fun invoke() {
                component<ProviderComponent>().fooFactory()
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testSuspendProvider() = codegen(
        """
            @Component
            abstract class ProviderComponent {
                abstract val fooFactory: suspend () -> Foo
                @Binding
                protected suspend fun foo() = Foo()
            }

            fun invoke() {
                runBlocking {
                    component<ProviderComponent>().fooFactory()
                }
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testComposableProvider() = codegen(
        """
            @Component
            abstract class ProviderComponent {
                abstract val fooFactory: @Composable () -> Foo
                @Composable
                @Binding
                protected fun foo() = Foo()
            }

            fun invoke() {
                component<ProviderComponent>().fooFactory
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testNullableProviderMissingBinding() = codegen(
        """
            @Component
            abstract class ProviderComponent {
                abstract val fooFactory: () -> Foo?
            }

            fun invoke() = component<ProviderComponent>().fooFactory()
        """
    ) {
        assertNull(invokeSingleFile())
    }

}
