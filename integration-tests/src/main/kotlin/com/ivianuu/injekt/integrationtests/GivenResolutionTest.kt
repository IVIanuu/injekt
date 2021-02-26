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

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class GivenResolutionTest {

    @Test
    fun testResolvesExternalGiven() = multiCodegen(
        listOf(
            source(
                """
                    @Given val foo = Foo()
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() = given<Foo>()
                """,
                name = "File.kt"
            )
        )
    ) {
        assertTrue(it.invokeSingleFile() is Foo)
    }

    @Test
    fun testResolvesInternalGiven() = codegen(
        """
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testPrefersInternalGivenOverExternal() = multiCodegen(
        listOf(
            source(
                """
                    @Given lateinit var externalFoo: Foo
                """
            )
        ),
        listOf(
            source(
                """
                    @Given lateinit var internalFoo: Foo

                    fun invoke(internal: Foo, external: Foo): Foo {
                        externalFoo = external
                        internalFoo = internal
                        return given<Foo>()
                    }
                """,
                name = "File.kt"
            )
        )
    ) {
        val internal = Foo()
        val external = Foo()
        val result = it.invokeSingleFile(internal, external)
        assertSame(result, internal)
    }

    @Test
    fun testPrefersObjectGivenOverInternalGiven() = codegen(
        """
            @Given lateinit var internalFoo: Foo
            object MyObject {
                @Given lateinit var objectFoo: Foo
                fun resolve() = given<Foo>()
            }

            fun invoke(internal: Foo, objectFoo: Foo): Foo {
                internalFoo = internal
                MyObject.objectFoo = objectFoo
                return MyObject.resolve()
            }
        """
    ) {
        val internal = Foo()
        val objectFoo = Foo()
        val result = invokeSingleFile(internal, objectFoo)
        assertSame(objectFoo, result)
    }

    @Test
    fun testResolvesClassCompanionGiven() = codegen(
        """
            class MyClass {
                fun resolve() = given<Foo>()
                companion object {
                    @Given val foo = Foo()
                }
            }

            fun invoke() = MyClass().resolve()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testPrefersClassCompanionGivenOverInternalGiven() = codegen(
        """
            @Given lateinit var internalFoo: Foo
            class MyClass {
                fun resolve() = given<Foo>()
                companion object {
                    @Given lateinit var companionFoo: Foo
                }
            }

            fun invoke(internal: Foo, companionFoo: Foo): Foo {
                internalFoo = internal
                MyClass.companionFoo = companionFoo
                return MyClass().resolve()
            }
        """
    ) {
        val internal = Foo()
        val companionFoo = Foo()
        val result = invokeSingleFile(internal, companionFoo)
        assertSame(companionFoo, result)
    }

    @Test
    fun testResolvesClassConstructorGiven() = codegen(
        """
            class MyClass(@Given val foo: Foo = Foo()) {
                fun resolve() = given<Foo>()
            }

            fun invoke() = MyClass().resolve()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testResolvesClassGiven() = codegen(
        """
            class MyClass {
                @Given val foo = Foo()
                fun resolve() = given<Foo>()
            }

            fun invoke() = MyClass().resolve()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testPrefersClassGivenOverInternalGiven() = codegen(
        """
            @Given lateinit var internalFoo: Foo
            class MyClass(@Given val classFoo: Foo) {
                fun resolve() = given<Foo>()
            }

            fun invoke(internal: Foo, classFoo: Foo): Foo {
                internalFoo = internal
                return MyClass(classFoo).resolve()
            }
        """
    ) {
        val internal = Foo()
        val classFoo = Foo()
        val result = invokeSingleFile(internal, classFoo)
        assertSame(classFoo, result)
    }

    @Test
    fun testPrefersClassGivenOverClassCompanionGiven() = codegen(
        """
            class MyClass(@Given val classFoo: Foo) {
                fun resolve() = given<Foo>()
                companion object {
                    @Given lateinit var companionFoo: Foo
                }
            }

            fun invoke(classFoo: Foo, companionFoo: Foo): Foo {
                MyClass.companionFoo = companionFoo
                return MyClass(classFoo).resolve()
            }
        """
    ) {
        val classFoo = Foo()
        val companionFoo = Foo()
        val result = invokeSingleFile(classFoo, companionFoo)
        assertSame(classFoo, result)
    }

    // todo class constructor given in init

    @Test
    fun testPrefersFunctionParameterGivenOverInternalGiven() = codegen(
        """
            @Given lateinit var internalFoo: Foo
            fun invoke(internal: Foo, @Given functionFoo: Foo): Foo {
                internalFoo = internal
                return given()
            }
        """
    ) {
        val internal = Foo()
        val functionFoo = Foo()
        val result = invokeSingleFile(internal, functionFoo)
        assertSame(functionFoo, result)
    }

    @Test
    fun testPrefersFunctionParameterGivenOverClassGiven() = codegen(
        """
            class MyClass(@Given val classFoo: Foo) {
                fun resolve(@Given functionFoo: Foo) = given<Foo>()
            }

            fun invoke(classFoo: Foo, functionFoo: Foo): Foo {
                return MyClass(classFoo).resolve(functionFoo)
            }
        """
    ) {
        val classFoo = Foo()
        val functionFoo = Foo()
        val result = invokeSingleFile(classFoo, functionFoo)
        assertSame(functionFoo, result)
    }

    @Test
    fun testPrefersFunctionReceiverGivenOverInternalGiven() = codegen(
        """
            @Given lateinit var internalFoo: Foo
            fun @receiver:Given Foo.invoke(internal: Foo): Foo {
                internalFoo = internal
                return given()
            }
        """
    ) {
        val internal = Foo()
        val functionFoo = Foo()
        val result = invokeSingleFile(functionFoo, internal)
        assertSame(functionFoo, result)
    }

    @Test
    fun testPrefersFunctionReceiverGivenOverClassGiven() = codegen(
        """
            class MyClass(@Given val classFoo: Foo) {
                fun @receiver:Given Foo.resolve() = given<Foo>()
            }

            fun invoke(classFoo: Foo, functionFoo: Foo): Foo {
                return with(MyClass(classFoo)) {
                    functionFoo.resolve()
                }
            }
        """
    ) {
        val classFoo = Foo()
        val functionFoo = Foo()
        val result = invokeSingleFile(classFoo, functionFoo)
        assertSame(functionFoo, result)
    }

    @Test
    fun testDerivedGiven() = codegen(
        """
            @Given val foo = Foo()
            @Given val bar = Bar(given())
            fun invoke() = given<Bar>()
        """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testCannotResolveExternalInternalMarkedGiven() = multiCodegen(
        listOf(
            source(
                """
                    @Given internal val foo = Foo()
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() = given<Foo>() 
                """,
                name = "File.kt"
            )
        )
    ) {
        it.last().assertCompileError("no given argument found of type com.ivianuu.injekt.test.Foo")
    }

    @Test
    fun testUnresolvedGiven() = codegen(
        """
            fun invoke() {
                given<String>()
            }
        """
    ) {
        assertCompileError("no given argument found of type kotlin.String")
    }

    @Test
    fun testNestedUnresolvedGiven() = codegen(
        """
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            fun invoke() = given<Bar>()
        """
    ) {
        assertCompileError(" no given argument found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
    }

    @Test
    fun testNestedUnresolvedGivenMulti() = multiCodegen(
        listOf(
            source(
                """
                   @Given fun bar(@Given foo: Foo) = Bar(foo) 
                """
            )
        ),
        listOf(
            source(
                """
                    fun callee(@Given bar: Bar) = bar
                    fun invoke() = callee()
                """
            )
        )
    ) {
        it.last()
            .assertCompileError(" no given argument found of type com.ivianuu.injekt.test.Foo for parameter foo of function com.ivianuu.injekt.integrationtests.bar")
    }

    @Test
    fun testPrefersProviderArgument() = codegen(
        """
            @Given fun foo() = Foo()
            fun invoke(foo: Foo) = given<(@Given Foo) -> Foo>()(foo)
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testPrefersInnerProviderArgumentOverOuterProviderArgument() = codegen(
        """
            @Given fun foo() = Foo()
            fun invoke(foo: Foo) = given<(@Given Foo) -> (@Given Foo) -> Foo>()(Foo())(foo)
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testPrefersResolvableGiven() = codegen(
        """
            @Given fun a() = "a"
            @Given fun b(@Given long: Long) = "b"
            fun invoke() = given<String>()
        """
    ) {
        assertEquals("a", invokeSingleFile())
    }

    @Test
    fun testAmbiguousGivens() = codegen(
        """
            @Given val a = "a"
            @Given val b = "b"
            fun invoke() = given<String>()
        """
    ) {
        assertCompileError("ambiguous given arguments of type kotlin.String for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testPrefersLesserParameters() = codegen(
        """
            @Given val a = "a"
            @Given val foo = Foo()
            @Given fun b(@Given foo: Foo) = "b"
            fun invoke() = given<String>()
        """
    ) {
        assertEquals("a", invokeSingleFile())
    }

    @Test
    fun testPrefersMoreSpecificType() = codegen(
        """
            @Given fun stringList(): List<String> = listOf("a", "b", "c")
            @Given fun <T> anyList(): List<T> = emptyList()
            fun invoke() = given<List<String>>()
        """
    ) {
        assertEquals(listOf("a", "b", "c"), invokeSingleFile())
    }

    @Test
    fun testPrefersShorterTree() = codegen(
        """
            @Given val a = "a"
            @Given val foo = Foo()
            @Given fun b(@Given foo: Foo) = "b"
            fun invoke() = given<String>()
        """
    ) {
        assertEquals("a", invokeSingleFile())
    }

    @Test
    fun testGenericGiven() = codegen(
        """
            @Given val foo = Foo()
            @Given fun <T> givenList(@Given value: T): List<T> = listOf(value)
            fun invoke() = given<List<Foo>>()
        """
    ) {
        val (foo) = invokeSingleFile<List<Any>>()
        assertTrue(foo is Foo)
    }

    @Test
    fun testFunctionInvocationWithGivens() = codegen(
        """
                @Given val foo = Foo()
                fun invoke() {
                    usesFoo()
                }

                fun usesFoo(@Given foo: Foo) {
                }
            """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testLocalFunctionInvocationWithGivens() = codegen(
        """
                @Given val foo = Foo()
                fun invoke() {
                    fun usesFoo(@Given foo: Foo) {
                    }                    
                    usesFoo()
                }
            """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testConstructorInvocationWithGivens() = codegen(
        """
                @Given val foo = Foo()
                fun invoke() {
                    UsesFoo()
                }

                class UsesFoo(@Given foo: Foo)
            """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testPrimaryConstructorGivenWithReceiver() = codegen(
        """
                fun invoke(foo: Foo) = withGiven(UsesFoo(foo)) {
                    given<Foo>()
                }

                class UsesFoo(@Given val foo: Foo)
            """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testLocalConstructorInvocationWithGivens() = codegen(
        """
                @Given val foo = Foo()
                fun invoke() {
                    class UsesFoo(@Given foo: Foo)
                    UsesFoo()
                }
            """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testUsesDefaultIfNotGiven() = codegen(
        """
                fun invoke(_foo: Foo): Foo {
                    fun inner(@Given foo: Foo = _foo) = foo
                    return inner()
                }
            """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testCanResolveGivenOfGivenThisFunction() = codegen(
        """
            class Dep(@Given val foo: Foo)
            fun invoke(foo: Foo): Foo {
                return withGiven(Dep(foo)) { given<Foo>() }
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile<Any>(foo))
    }

    @Test
    fun testCanResolveGivenWhichDependsOnAssistedGivenOfTheSameType() = codegen(
        """
            typealias SpecialScope = Unit
            
            @Given fun <E> asRunnable(
                @Given factory: (@Given SpecialScope) -> List<E>
            ): List<E> = factory(Unit)
            
            @Given fun raw(@Given scope: SpecialScope): List<String> = listOf("")
            
            fun main() {
                given<List<String>>()
            } 
        """
    )

    @Test
    fun testCanResolveStarProjectedType() = codegen(
        """
            @Given fun foos() = Foo() to Foo()
            
            @Qualifier annotation class First
            @Given fun <A : @First B, B> first(@Given pair: Pair<B, *>): A = pair.first as A

            fun invoke() = given<@First Foo>()
        """
    )

    @Test
    fun testCannotResolveObjectWithoutGiven() = codegen(
        """
            object MyObject
            fun invoke() = given<MyObject>()
        """
    ) {
        assertCompileError("no given argument")
    }

    @Test
    fun testCanResolveObjectWithGiven() = codegen(
        """
            @Given object MyObject
            fun invoke() = given<MyObject>()
        """
    )
}
