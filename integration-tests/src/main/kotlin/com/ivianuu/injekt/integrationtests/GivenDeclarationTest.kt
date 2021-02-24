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

import com.ivianuu.injekt.test.*
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class GivenDeclarationTest {

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
    fun testGivenProperty() = codegen(
        """
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        assertTrue(invokeSingleFile<Any>() is Foo)
    }

    @Test
    fun testGivenClass() = codegen(
        """
            @Given val foo = Foo()
            @Given class Dep(@Given val foo: Foo)
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
            class Dep @Given constructor(@Given val foo: Foo)
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
                @Given constructor(@Given foo: Foo)
            }
            fun invoke() = given<Dep>()
        """
    ) {
        assertEquals("com.ivianuu.injekt.integrationtests.Dep",
            invokeSingleFile<Any>().javaClass.name)
    }

    @Test
    fun testGivenSuperTypeClass() = codegen(
        """
            interface Dep<T>
            @Given class DepImpl<T>(@Given value: T) : @Given Dep<T>

            @Given val foo = Foo()
            fun invoke() = given<Dep<Foo>>()
        """
    ) {
        assertEquals("com.ivianuu.injekt.integrationtests.DepImpl",
            invokeSingleFile<Any>().javaClass.name)
    }

    @Test
    fun testGivenSuperTypeClassWithGivenConstructor() = codegen(
        """
            interface Dep<T>
            class DepImpl<T> : @Given Dep<T> {
                @Given
                constructor(@Given value: T)
            }

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
    fun testGivenCompanionObject() = codegen(
        """
            @Given val foo = Foo()
            class Dep {
                @Given
                companion object {
                    init {
                        given<Foo>()
                    }
                }
            }
            fun invoke() = given<Dep.Companion>()
        """
    ) {
        assertEquals("com.ivianuu.injekt.integrationtests.Dep\$Companion",
            invokeSingleFile<Any>().javaClass.name)
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
    fun testGivenValueParameter() = codegen(
        """
            fun invoke(@Given foo: Foo) = given<Foo>()
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
    fun testCanLeaveOutGivenLambdaParameters() = codegen(
        """
            val lambda: (@Given Foo) -> Foo = { given<Foo>() }
            fun invoke(@Given foo: Foo): Foo {
                return lambda()
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile<Any>(foo))
    }

    @Test
    fun testCanLeaveOutGivenLambdaParametersWithTypeAlias() = codegen(
        """
            typealias LambdaType = (@Given Foo) -> Foo
            val lambda: LambdaType = { given<Foo>() }
            fun invoke(@Given foo: Foo): Foo {
                return lambda()
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
                @Given class FooProvider(@Given val foo: Foo = _foo)
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

    @Test
    fun testMultipleContributionAnnotationsFails() = codegen(
        """
            @Given @GivenSetElement fun foo() = Foo()
        """
    ) {
        assertCompileError("Declaration may be only annotated with one contribution annotation")
    }

}
