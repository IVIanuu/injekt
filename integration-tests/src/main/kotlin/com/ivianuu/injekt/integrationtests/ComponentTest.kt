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

class ComponentTest {

    @Test
    fun testMissingGivenFails() = codegen(
        """
            class Dep
            
            @RootFactory
            typealias MyFactory = () -> TestComponent1<Dep>
        """
    ) {
        assertInternalError("no given")
    }

    @Test
    fun testDeeplyMissingGivenFails() = codegen(
        """
            @Module
            object MyModule {
                @Given
                fun bar(foo: Foo) = Bar(foo)
        
                @Given
                fun baz(bar: Bar, foo: Foo) = Baz(bar, foo)
            }
            
            @RootFactory
            typealias MyFactory = (MyModule) -> TestComponent1<Baz>
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
                return rootFactory<TestContext>().runReader { given<Set<String>>() to given<Set<Int>>() }
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
                return rootFactory<TestContext>().runReader { given<Foo1>() to given<Foo2>() }
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
                    @Module
                    object Foo1Module {
                        @Given fun foo1(): Foo1 = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    typealias Foo2 = Foo
                    @Module
                    object Foo2Module {
                        @Given fun foo2(): Foo2 = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    @RootFactory
                    typealias MyFactory = 
        
                    fun invoke(): Pair<Foo, Foo> {
                        return rootFactory<TestContext>().runReader { given<Foo1>() to given<Foo2>() }
                    }
            """,
                name = "File.kt"
            )
        )
    ) {
        val (foo1, foo2) = it.last().invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testIgnoresNullability() = codegen(
        """
            @Module
            object FooModule { 
                @Given fun foo(): Foo = Foo()
                @Given fun nullableFoo(): Foo? = null
            }
            
            @RootFactory
            typealias MyFactory = (FooModule) -> TestComponent1<Foo>
            """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testReturnsInstanceForNullableGiven() = codegen(
        """
            @Given fun foo(): Foo = Foo()
    
            fun invoke(): Foo? { 
                return rootFactory<TestContext>().runReader { given<Foo?>() }
            }
        """
    ) {
        assertNotNull(invokeSingleFile())
    }

    @Test
    fun testReturnsNullOnMissingNullableGiven() = codegen(
        """
            fun invoke(): Foo? { 
                return rootFactory<TestContext>().runReader { given<Foo?>() }
            }
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testTypeWithStarProjectedArg() = codegen(
        """ 
            @RootFactory
            typealias MyFactory = (List<*>) -> TestComponent1<List<*>>
        """
    )

    @Test
    fun testPrefersInputsOverGiven() = codegen(
        """
            @Given
            fun provideFoo() = Foo()
            
            fun invoke(foo: Foo): Foo {
                return rootFactory<TestContext>(foo).runReader { given() }
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testDuplicatedInputsFails() = codegen(
        """
            fun invoke() {
                rootFactory<TestContext>(Foo(), Foo()).runReader { given<Foo>() }
            }
        """
    ) {
        assertInternalError("multiple givens found in inputs")
    }

    @Test
    fun testDuplicatedGivensFails() = codegen(
        """
            @Module
            object FooModule {
                @Given fun foo1() = Foo()
                @Given fun foo2() = Foo()
            }
            
            @RootFactory
            typealias MyFactory = (FooModule) -> TestComponent1<Foo>
        """
    ) {
        assertInternalError("multiple internal givens")
    }

    @Test
    fun testGivenPerContext() = codegen(
        """
            @Given(TestParentContext::class) fun parentFoo() = Foo()
            @Given(TestChildContext::class) fun childFoo() = Foo()
            fun invoke(): Pair<Foo, Foo> {
                return rootFactory<TestParentContext>().runReader {
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

    interface SelfComponent {
        val self: SelfComponent
    }

    @Test
    fun testInjectingComponent() = codegen(
        """
            import com.ivianuu.injekt.integrationtests.ComponentTest.SelfComponent
            
            @RootFactory
            typealias MyFactory = () -> SelfComponent
            fun invoke(): Pair<SelfComponent, SelfComponent> {
                val component = rootFactory<MyFactory>()()
                return component to component.self
            }
        """
    ) {
        val (context1, context2) = invokeSingleFile<Pair<SelfComponent, SelfComponent>>()
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

    @Test
    fun testGenericTypeAlias() = codegen(
        """
            interface Comparator<T> {
                fun compare(a: T, b: T): Int
            }
            typealias AliasComparator<T> = Comparator<T>
            @Reader
            fun callMax() {
                compare(1, 2)
            }

            @Reader
            fun <T> compare(a: T, b: T): Int = given<AliasComparator<T>>()
                .compare(a, b)

        """
    )
}
