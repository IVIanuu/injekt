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
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class ReaderContextTest {

    @Test
    fun testSimple() = codegen(
        """
            @Module
            object TestModule {
                @Given
                fun foo() = Foo()
                
                @Given
                fun bar() = Bar(given())
            }
            
            interface TestComponent {
                val bar: Bar
            }

            typealias TestComponentFactory = (TestModule) -> TestComponent
            
            fun invoke(): Bar {
                return rootFactory<TestComponentFactory>()(TestModule)
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testWithChild() = codegen(
        """
            @Given
            fun foo() = Foo()
            @Given
            fun bar() = Bar(given())
            
            fun invoke(foo: Foo): Foo {
                return rootContext<TestParentContext>(42, "hello world").runReader { overriddingFoo(foo) }
            }
            
            fun otherInvoke() = rootContext<TestParentContext>().runReader { overriddingFoo(Foo()) }
            
            @Reader
            private fun overriddingFoo(foo: Foo) = childContext<TestChildContext>(foo).runReader {
                given<Bar>().foo
            }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testMultiChild() = codegen(
        """
            @Given
            fun foo() = Foo()
            @Given
            fun bar() = Bar(given())
            
            fun invoke(foo: Foo): Foo {
                return rootContext<TestParentContext>(42, "hello world", foo).runReader { overriddingFoo(foo) }
            }
            
            fun otherInvoke() = rootContext<TestParentContext>().runReader { overriddingFoo(Foo()) }
            
            @Reader
            private fun overriddingFoo(foo: Foo) = childContext<TestChildContext>(Foo()).runReader {
                childContext<TestContext>(foo).runReader {
                    given<Bar>().foo
                }
            }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testWithGenericChild() = codegen(
        """
        @Given
        fun foo() = Foo()
        @Given
        fun bar() = Bar(given())
        
        fun invoke(foo: Foo): Foo {
            return rootContext<TestContext>(42, "hello world").runReader { overriding<Bar>(foo) }
        }

        @Reader
        private fun <T> overriding(value: Foo) = childContext<TestChildContext>(value).runReader {
            given<Bar>().foo
        }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testComplexGenericChild() = codegen(
        """
        @Given
        fun foo() = Foo()
        @Given
        fun bar() = Bar(given())
        
        fun invoke(foo: Foo): Foo {
            return rootContext<TestParentContext>(42, true).runReader { genericA<Bar>(foo) }
        }
        
        @Reader
        fun <T> genericA(foo: Foo) = childContext<TestChildContext>(foo, "").runReader {
            nonGeneric(foo)
        }
        
        @Reader
        private fun nonGeneric(foo: Foo) = genericB<String>(foo)

        @Reader
        private fun <S> genericB(foo: Foo) = childContext<TestChildContext2>(foo, 0L).runReader {
            given<Bar>().foo
        }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testScopedGiven() = codegen(
        """
        @Given(TestContext::class)
        fun foo() = Foo()
        
        val context = rootContext<TestContext>()
        
        fun invoke(): Foo {
            return context.runReader { given<Foo>() }
        }
    """
    ) {
        assertSame(invokeSingleFile(), invokeSingleFile())
    }

    @Test
    fun testParentScopedGiven() = codegen(
        """
        @Given
        fun foo() = Foo()

        @Given(TestParentContext::class)
        fun bar() = Bar(given())
        
        val parentContext = rootContext<TestParentContext>()
        val childContext = parentContext.runReader {
            childContext<TestChildContext>()
        }
        
        fun invoke(): Bar {
            return childContext.runReader { given<Bar>() }
        }
    """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testGivenClass() = codegen(
        """
        @Given
        class AnnotatedBar {
            private val foo: Foo = given()
        }
        
        @Given
        fun foo(): Foo = Foo()

        fun invoke(): Any { 
            return rootContext<TestContext>().runReader { given<AnnotatedBar>() }
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenObject() = codegen(
        """
        @Given
        object AnnotatedBar

        fun invoke(): Any { 
            return rootContext<TestContext>().runReader { given<AnnotatedBar>() }
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenProperty() = codegen(
        """
        @Given val foo get() =  Foo()
        
        fun invoke(): Foo {
            return rootContext<TestContext>().runReader { given<Foo>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testAssistedGivenFunction() = codegen(
        """ 
        @Given
        fun bar(foo: Foo) = Bar(foo)

        fun invoke(foo: Foo): Bar { 
            return rootContext<TestContext>().runReader { given<Bar>(foo) }
        }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testAssistedGivenClass() = codegen(
        """ 
        @Given
        class AnnotatedBar(foo: Foo)

        fun invoke(foo: Foo): Any {
            return rootContext<TestContext>().runReader { given<AnnotatedBar>(foo) }
        }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testGenericGivenClass() = codegen(
        """
        @Given class Dep<T> {
            val value: T = given()
        }
        
        @Given fun foo() = Foo() 
        
        fun invoke() {
            rootContext<TestContext>().runReader {
                given<Dep<Foo>>()
            }
        }
    """
    )

    @Test
    fun testGenericGivenFunction() = codegen(
        """    
        class Dep<T>(val value: T)
        
        @Given fun <T> dep() = Dep<T>(given())
        
        @Given fun foo() = Foo() 

        fun invoke() {
            rootContext<TestContext>().runReader {
                given<Dep<Foo>>()
            }
        }
    """
    )

    @Test
    fun testComplexGenericGivenFunction() = codegen(
        """    
        class Dep<A, B, C>(val value: A)
        
        @Given fun <A, B : A, C : B> dep() = Dep<A, A, A>(given())
        
        @Given fun foo() = Foo() 

        fun invoke() {
            rootContext<TestContext>().runReader {
                given<Dep<Foo, Foo, Foo>>()
            }
        }
    """
    )

    @Test
    fun testInput() = codegen(
        """
        fun invoke(): Pair<Foo, Foo> {
            val foo = Foo()
            return foo to rootContext<TestContext>(foo).runReader { given<Foo>() }
        }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testModule() = codegen(
        """
        @GivenSet
        class FooGivens {
            @Given
            fun foo() = Foo()
        }
        
        @Given
        fun bar() = Bar(given())
        
        fun invoke(): Bar {
            return rootContext<TestContext>(FooGivens()).runReader { given<Bar>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testNestedModule() = codegen(
        """
        @GivenSet
        class FooGivens {
            @Given
            fun foo() = Foo()
            
            @GivenSet
            val barGivens = BarGivens()
            
            @GivenSet
            class BarGivens {
                @Given
                fun bar() = Bar(given())
            }
        }
        
        fun invoke(): Bar {
            return rootContext<TestContext>(FooGivens()).runReader { given<Bar>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

}
