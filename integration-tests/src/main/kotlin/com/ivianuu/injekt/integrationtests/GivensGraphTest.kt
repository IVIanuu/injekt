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

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.TestComponent
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertSame
import org.junit.Test

class GivensGraphTest {

    @Test
    fun testMissingGivenFails() = codegen(
        """
        class Dep()
        fun invoke() {
            rootContext<TestContext>().runReader { given<Dep>() }
        }
        """
    ) {
        assertInternalError("no given")
    }

    @Test
    fun testDeeplyMissingGivenFails() = codegen(
        """
    
        @Given
        fun bar() = Bar(given())
    
        @Given
        fun baz() = Baz(given(), given())

        fun invoke() {
            rootContext<TestContext>().runReader { given<Baz>() }
        }
        """
    ) {
        assertInternalError("no given")
    }

    @Test
    fun testMissingGivenFails2() = codegen(
        """
        @Reader
        fun a() {
            b()
        }
        
        @Reader
        fun b() {
            c()
        }
        
        @Reader
        fun c() {
            given<Any>()
        }
        
        fun invoke() {
            rootContext<TestContext>().runReader { a() }
        }
        """
    ) {
        assertInternalError("no given")
    }

    @Test
    fun testDistinctTypeParameter() = codegen(
        """
        @GivenSetElements fun setA() = setOf("a")
        @GivenSetElements fun setB() = setOf(0)
        
        fun invoke(): Pair<Set<String>, Set<Int>> {
            return rootContext<TestContext>().runReader { given<Set<String>>() to given<Set<Int>>() }
        }
    """
    ) {
        val (setA, setB) = invokeSingleFile<Pair<Set<String>, Set<Int>>>()
        assertNotSame(setA, setB)
    }

    @Test
    fun testDistinctTypeAlias() = codegen(
        """
        typealias Foo1 = Foo
        typealias Foo2 = Foo
        
        @Given fun foo1(): Foo1 = Foo()
        @Given fun foo2(): Foo2 = Foo()
        
        fun invoke(): Pair<Foo, Foo> {
            return rootContext<TestContext>().runReader { given<Foo1>() to given<Foo2>() }
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctTypeAliasMulti() = multiCodegen(
        listOf(
            source(
                """
                typealias Foo1 = Foo
                @Given fun foo1(): Foo1 = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                typealias Foo2 = Foo
                @Given fun foo2(): Foo2 = Foo() 
            """
            )
        ),
        listOf(
            source(
                """
                fun invoke(): Pair<Foo, Foo> {
                    return rootContext<TestContext>().runReader { given<Foo1>() to given<Foo2>() }
                } 
            """, name = "File.kt"
            )
        )
    ) {
        val (foo1, foo2) = it.last().invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testIgnoresNullability() = codegen(
        """
        @Given fun foo(): Foo = Foo()
        @Given fun nullableFoo(): Foo? = null

        fun invoke() { 
            rootContext<TestContext>().runReader { given<Foo>() to given<Foo?>() }
        }
    """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testReturnsInstanceForNullableGiven() = codegen(
        """
        @Given fun foo(): Foo = Foo()

        fun invoke(): Foo? { 
            return rootContext<TestContext>().runReader { given<Foo?>() }
        }
        """
    ) {
        assertNotNull(invokeSingleFile())
    }

    @Test
    fun testReturnsNullOnMissingNullableGiven() = codegen(
        """
        fun invoke(): Foo? { 
            return rootContext<TestContext>().runReader { given<Foo?>() }
        }
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testTypeWithStarProjectedArg() = codegen(
        """
        @Given fun list(): List<*> = emptyList<Any?>()
        
        fun invoke() { 
            rootContext<TestContext>().runReader { given<List<*>>() }
        }
    """
    )

    @Test
    fun testPrefersInputsOverGiven() = codegen(
        """
            @Given
            fun provideFoo() = Foo()
            
            fun invoke(foo: Foo): Foo {
                return rootContext<TestContext>(foo).runReader { given() }
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testPrefersInternalOverExternal() = multiCodegen(
        listOf(
            source(
                """
                    var externalFooField: Foo? = null
                    @Given
                    val externalFoo: Foo get() = externalFooField!!
                """
            )
        ),
        listOf(
            source(
                """
                    var internalFooField: Foo? = null
                    @Given
                    val internalFoo: Foo get() = internalFooField!!
                    
                    fun invoke(
                        internalFoo: Foo,
                        externalFoo: Foo
                    ): Foo {
                        externalFooField = externalFoo
                        internalFooField = internalFoo
                        return rootContext<TestContext>().runReader { given<Foo>() }
                    }
                """,
                name = "File.kt"
            )
        )
    ) {
        val externalFoo = Foo()
        val internalFoo = Foo()
        assertSame(internalFoo, it.last().invokeSingleFile(internalFoo, externalFoo))
    }

    @Test
    fun testDuplicatedInputsFails() = codegen(
        """
        fun invoke() {
            rootContext<TestContext>(Foo(), Foo()).runReader { given<Foo>() }
        }
        """
    ) {
        assertInternalError("multiple givens found in inputs")
    }

    @Test
    fun testDuplicatedInternalGivensFails() = codegen(
        """
        @Given fun foo1() = Foo()
        @Given fun foo2() = Foo()
        
        fun invoke() {
            rootContext<TestContext>().runReader { given<Foo>() }
        }
        """
    ) {
        assertInternalError("multiple internal givens")
    }

    @Test
    fun testDuplicatedExternalGivensFails() = multiCodegen(
        listOf(
            source(
                """
                    @Given fun foo1() = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                    @Given fun foo2() = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() { 
                        rootContext<TestContext>().runReader { given<Foo>() }
                    }
                """
            )
        )
    ) {
        it.last().assertInternalError("multiple external givens")
    }

    @Test
    fun testGivenPerContext() = codegen(
        """
        @Given(TestParentContext::class) fun parentFoo() = Foo()
        @Given(TestChildContext::class) fun childFoo() = Foo()
        fun invoke(): Pair<Foo, Foo> {
            return rootContext<TestParentContext>().runReader {
                given<Foo>() to childContext<TestChildContext>().runReader {
                    given<Foo>()
                }
            }
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testInjectingContext() = codegen(
        """
            fun invoke(): Pair<TestContext, TestContext> {
                val context = rootContext<TestContext>()
                return context to context.runReader { given<TestContext>() }
            }
        """
    ) {
        val (context1, context2) = invokeSingleFile<Pair<TestComponent, TestComponent>>()
        assertSame(context1, context2)
    }

    @Test
    fun testPrefersExactType() = codegen(
        """
            class Dep<T>(val value: T)
            
            @Given
            fun <T> genericDep(): Dep<T> = Dep(given())
            
            @Given
            fun fooDep(): Dep<Foo> = Dep(given())
            
            @Given
            fun foo() = Foo()
            
            fun invoke() {
                runReader {
                    given<Dep<Foo>>()
                }
            }
        """
    )

}
