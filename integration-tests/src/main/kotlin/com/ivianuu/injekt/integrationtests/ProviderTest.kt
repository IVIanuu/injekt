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
    fun testProviderWithGivenArgs() = codegen(
        """
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            fun invoke() = given<(@Given Foo) -> Bar>()(Foo())
        """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test fun testProviderWithGenericGivenArgs() = codegen(
        """ 
            typealias ComponentA = Component

            fun createComponentA() = ComponentBuilder<ComponentA>()
                .build()

            typealias ComponentB = Component

            @GivenSetElement fun componentBFactory(
                @Given parent: ComponentA,
                @Given builderFactory: () -> Component.Builder<ComponentB>
            ) = componentElement<ComponentA, () -> ComponentB> { 
                builderFactory()
                    .dependency(parent)
                    .build()
            }

            typealias ComponentC = Component

            @GivenSetElement fun componentCFactory(
                @Given parent: ComponentB,
                @Given builderFactory: () -> Component.Builder<ComponentC>
            ) = componentElement<ComponentB, () -> ComponentC> {
                builderFactory()
                    .dependency(parent)
                    .build()
            }
        """
    )

    @Test
    fun testProviderGivenGroup() = codegen(
        """
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            class FooGroup(@Given val foo: Foo)
            fun invoke(): Bar {
                return given<(@GivenGroup FooGroup) -> Bar>()(FooGroup(Foo()))
            }
        """
    )

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