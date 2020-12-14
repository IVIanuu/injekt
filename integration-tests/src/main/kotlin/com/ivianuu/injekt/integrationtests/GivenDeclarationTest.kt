package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class GivenDeclarationTest {

    @Test
    fun testGivenClass() = codegen(
        """
            @Given val foo = Foo()
            @Given class Dep(val foo: Foo = given)
            fun invoke() = given<Dep>()
        """
    ) {
        assertEquals("com.ivianuu.injekt.integrationtests.Dep",
            invokeSingleFile<Any>().javaClass.name)
    }

    @Test
    fun testGivenClassPrimaryConstructor() = codegen(
        """
            @Given val foo = Foo()
            class Dep @Given constructor(val foo: Foo = given)
            fun invoke() = given<Dep>()
        """
    ) {
        assertEquals("com.ivianuu.injekt.integrationtests.Dep",
            invokeSingleFile<Any>().javaClass.name)
    }

    @Test
    fun testGivenClassSecondaryConstructor() = codegen(
        """
            @Given val foo = Foo()
            class Dep {
                @Given constructor(foo: Foo = given)
            }
            fun invoke() = given<Dep>()
        """
    ) {
        assertEquals("com.ivianuu.injekt.integrationtests.Dep",
            invokeSingleFile<Any>().javaClass.name)
    }

    @Test
    fun testGivenAliasClass() = codegen(
        """
            interface Dep<T> {
                val value: T
            }
            @Given class DepImpl<T>(override val value: T = given) : @Given Dep<T>

            @Given val foo = Foo()
            fun invoke() = given<Dep<Foo>>()
        """
    ) {
        assertEquals("com.ivianuu.injekt.integrationtests.DepImpl",
            invokeSingleFile<Any>().javaClass.name)
    }

    @Test
    fun testGivenObject() = codegen(
        """
            @Given val foo = Foo()
            @Given object Dep {
                init {
                    given<Foo>()
                }
            }
            fun invoke() = given<Dep>()
        """
    ) {
        assertEquals("com.ivianuu.injekt.integrationtests.Dep",
            invokeSingleFile<Any>().javaClass.name)
    }

    @Test
    fun testGivenProperty() = codegen(
        """
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        assertTrue(invokeSingleFile<Any>() is Foo)
    }

    @Test
    fun testGivenFunction() = codegen(
        """
            @Given fun foo() = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        assertTrue(invokeSingleFile<Any>() is Foo)
    }

    @Test
    fun testGivenExtensionFunction() = codegen(
        """
            @Given fun @Given Foo.bar() = Bar(this)
            fun invoke(@Given foo: Foo) = given<Bar>()
        """
    ) {
        assertTrue(invokeSingleFile<Any>(Foo()) is Bar)
    }

    @Test
    fun testGivenExtensionProperty() = codegen(
        """
            @Given val @Given Foo.bar get() = Bar(this)
            fun invoke(@Given foo: Foo) = given<Bar>()
        """
    ) {
        assertTrue(invokeSingleFile<Any>(Foo()) is Bar)
    }

    @Test
    fun testExplicitGivenValueParameter() = codegen(
        """
            fun invoke(@Given foo: Foo) = given<Foo>()
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile<Any>(foo))
    }

    @Test
    fun testImplicitGivenValueParameter() = codegen(
        """
            fun invoke(foo: Foo = given) = given<Foo>()
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile<Any>(foo))
    }

    @Test
    fun testGivenExtensionReceiver() = codegen(
        """
            fun @receiver:Given Foo.invoke() = given<Foo>()
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile<Any>(foo))
    }

    @Test
    fun testGivenLocalVariable() = codegen(
        """
            fun invoke(foo: Foo): Foo {
                @Given val givenFoo = foo
                return given()
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile<Any>(foo))
    }

    @Test
    fun testGivenLambdaReceiverParameter() = codegen(
        """
            inline fun <T, R> diyWithGiven(value: T, block: @Given T.() -> R) = block(value)
            fun invoke(foo: Foo): Foo {
                return diyWithGiven(foo) { given<Foo>() }
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile<Any>(foo))
    }

    @Test
    fun testGivenLambdaParameterDeclarationSite() = codegen(
        """
            inline fun <T, R> withGiven(value: T, block: (@Given T) -> R) = block(value)
            fun invoke(foo: Foo): Foo {
                return withGiven(foo) { given<Foo>() }
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile<Any>(foo))
    }

    @Test
    fun testGivenLambdaParameterDeclarationSiteWithTypeAlias() = codegen(
        """
            typealias UseContext<T, R> = (@Given T) -> R
            inline fun <T, R> withGiven(value: T, block: UseContext<T, R>) = block(value)
            fun invoke(foo: Foo): Foo {
                return withGiven(foo) { given<Foo>() }
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile<Any>(foo))
    }

    @Test
    fun testGivenLambdaParameterUseSite() = codegen(
        """
            inline fun <T, R> withGiven(value: T, block: (T) -> R) = block(value)
            fun invoke(foo: Foo): Foo {
                return withGiven(foo) { foo: @Given Foo -> given<Foo>() }
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile<Any>(foo))
    }

    @Test
    fun testGivenInNestedBlock() = codegen(
        """
            fun invoke(a: Foo, b: Foo): Pair<Foo, Foo> {
                return run {
                    @Given val givenA = a
                    given<Foo>() to run {
                        @Given val givenB = b
                        given<Foo>()
                    }
                }
            }
        """
    ) {
        val a = Foo()
        val b = Foo()
        val result = invokeSingleFile<Pair<Foo, Foo>>(a, b)
        assertSame(a, result.first)
        assertSame(b, result.second)
    }

    @Test
    fun testGivenLocalClass() = codegen(
        """
            fun invoke(_foo: Foo): Foo {
                @Given class FooProvider(val foo: Foo = givenOrElse { _foo })
                return given<FooProvider>().foo
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testGivenLocalFunction() = codegen(
        """
            fun invoke(foo: Foo): Foo {
                @Given fun foo() = foo
                return given<Foo>()
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testGivenSuspendFunction() = codegen(
        """
            @Given suspend fun foo() = Foo()
            fun invoke() = runBlocking { given<Foo>() }
        """
    ) {
        assertTrue(invokeSingleFile<Any>() is Foo)
    }

    @Test
    fun testGivenComposableFunction() = codegen(
        """
            @Given @Composable fun foo() = Foo()
            @Composable fun invoke() { given<Foo>() }
        """
    )

}
