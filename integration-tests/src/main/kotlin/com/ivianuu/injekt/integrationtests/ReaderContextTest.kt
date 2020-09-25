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
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class ReaderContextTest {

    @Test
    fun testSimple() = codegen(
        """
            @Given
            fun foo() = Foo()
            @Given
            fun bar() = Bar(given())
            
            fun invoke(): Bar {
                return rootContext<TestContext>().runReader { given<Bar>() }
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
    fun testGenericRequestInChildContext() = codegen(
        """
            val parentContext = rootContext<TestParentContext>()
            
            @Given
            fun foo() = Foo()
            
            fun invoke() {
                parentContext.runReader {
                    val foo = diyGiven<Foo>()
                }
            }
            
            @Reader
            fun <T> diyGiven() = childContext<TestChildContext>().runReader { given<T>() }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testEssentialsRememberStore() = codegen(
        """
            interface CoroutineScope
            interface Store<S, A>
            
            @Reader
            fun <S, A> rememberStore(): Store<S, A> = rememberStore {
                given(this)
            }

            @Reader
            fun <S, A> rememberStore(init: CoroutineScope.() -> Store<S, A>): Store<S, A> {
                val scope: CoroutineScope = error("")
                return init(scope)
            }
        """
    ) {
        assertOk()
    }

    // todo @Test
    fun testGenericRequestInNestedChildContext() = codegen(
        """
            val parentContext = rootContext<TestParentContext>()
            
            @Given
            fun foo() = Foo()
            
            fun invoke() {
                parentContext.runReader {
                    val foo = diyGiven<Foo>()
                }
            }
            
            @Reader
            fun <T> diyGiven() = childContext<TestChildContext>().runReader { 
                childContext<TestChildContext2>().runReader { 
                    given<T>() 
                }
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testRunChildReaderWithEffect() = codegen(
        """ 
            @Effect
            annotation class GivenFooFactory {
                @GivenSet
                companion object {
                    @Given
                    fun <T : () -> Foo> invoke(): FooFactoryMarker = given<T>()
                }
            }

            typealias FooFactoryMarker = () -> Foo
            
            @GivenFooFactory
            fun FooFactoryImpl() = childContext<TestChildContext>().runReader { given<Foo>() }
            
            fun invoke(foo: Foo): Foo {
                return rootContext<TestParentContext>(foo).runReader {
                    given<FooFactoryMarker>()()
                }
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testRunReaderInsideSuspend() = codegen(
        """
        @Given
        fun foo() = Foo()
        @Given
        fun bar() = Bar(given())
        
        fun invoke(): Bar {
            return runBlocking {
                rootContext<TestContext>().runReader { 
                    delay(1)
                    given<Bar>()
                }
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
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
    fun testScopedGivenClass() = codegen(
        """
        @Given(TestContext::class)
        class Dep
        
        val context = rootContext<TestContext>()
        
        fun invoke(): Dep {
            return context.runReader { given<Dep>() }
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
    fun testParentScopedGiven2() = codegen(
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
    fun testParentScopedGiven3() = codegen(
        """
        @Given
        fun foo() = Foo()

        @Given(TestParentContext::class)
        fun bar() = Bar(given())
        
        @Reader
        fun barGetter() = given<Bar>()
        
        val parentContext = rootContext<TestParentContext>()
        val childContext = parentContext.runReader {
            childContext<TestChildContext>()
        }
        
        fun invoke(): Bar {
            return childContext.runReader { barGetter() }
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

    // todo @Test
    fun testGenericGivenClass() = codegen(
        """
        @Given class Dep<T> {
            val value: T = given()
        }
        
        @Given fun foo() = Foo() 
        
        fun invoke() {
            runReader {
                given<Dep<Foo>>()
            }
        }
    """
    )

    // todo @Test
    fun testGenericGivenFunction() = codegen(
        """    
        @Given class Dep<T> { val value: T = given() }
        
        @Given fun <T> dep() = Dep<T>()
        
        @Given fun foo() = Foo() 

        fun invoke() {
            runReader {
                given<Dep<Foo>>()
            }
        }
    """
    )

    @Test
    fun testRunReaderInput() = codegen(
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
    fun testGivenSet() = codegen(
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
    fun testNestedGivenSets() = codegen(
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
