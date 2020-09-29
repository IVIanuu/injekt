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
                fun bar(foo: Foo) = Bar(foo)
            }
            
            @RootFactory
            typealias TestComponentFactory = (TestModule) -> TestComponent1<Bar>
            
            fun invoke(): Bar {
                return rootFactory<TestComponentFactory>()(TestModule).a
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testWithChild() = codegen(
        """
            @RootFactory
            typealias MyParentFactory = () -> TestParentComponent1<MyChildFactory>
            
            @ChildFactory
            typealias MyChildFactory = (Foo) -> TestChildComponent1<Foo>
            
            fun invoke(foo: Foo): Foo {
                return rootFactory<MyParentFactory>()().a(foo).a
            }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testScopedGiven() = codegen(
        """
            @Module
            object MyModule {
                @Given(TestComponent1::class)
                fun foo() = Foo()
            }
            @RootFactory
            typealias MyFactory = (MyModule) -> TestComponent1<Foo>
        
            val component = rootFactory<MyFactory>()(MyModule)
        
            fun invoke() = component.a
    """
    ) {
        assertSame(invokeSingleFile(), invokeSingleFile())
    }

    @Test
    fun testParentScopedGiven() = codegen(
        """
            @Module
            object MyModule {
                @Given
                fun foo() = Foo()
                
                @Given(TestParentComponent1::class)
                fun bar() = Bar(given())
            }
            
            @RootFactory
            typealias MyParentFactory = (MyModule) -> TestParentComponent1<Foo>
            val parentComponent = rootFactory<MyFactory>()(MyModule)
            
            @ChildFactory
            typealias MyChildFactory = () -> TestChildComponent1<Bar>
            val childComponent = parentComponent.a()
         
            fun invoke(): Bar {
                return childComponent.a
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
            class AnnotatedBar(foo: Foo)
            
            @Module
            object FooModule {
                @Given
                fun foo() = Foo()
            }
    
            @RootFactory
            typealias MyFactory = (FooModule) -> TestComponent1<AnnotatedBar>
            
            fun invoke() {
                rootFactory<MyFactory>()(FooModule).a
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
    
            @RootFactory
            typealias MyFactory = () -> TestComponent1<AnnotatedBar>
            
            fun invoke() {
                rootFactory<MyFactory>()().a
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenProperty() = codegen(
        """
            @Module
            object FooModule {
                @Given val foo = Foo()
            }
    
            @RootFactory
            typealias MyFactory = (FooModule) -> TestComponent1<Foo>
            
            fun invoke() = rootFactory<MyFactory>()(FooModule).a
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testAssistedGivenFunction() = codegen(
        """
            @Module
            object BarModule {
                @Given
                fun bar(@Assisted foo: Foo) = Bar(foo)
            }
            
            @RootFactory
            typealias MyFactory = (BarModule) -> TestComponent1<(Foo) -> Bar>

            fun invoke(foo: Foo): Bar { 
                return rootFactory<MyFactory>()(BarModule).a(foo)
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testAssistedGivenClass() = codegen(
        """
            @Given
            class AnnotatedBar(@Assisted foo: Foo)
            
            @RootFactory
            typealias MyFactory = (BarModule) -> TestComponent1<(Foo) -> AnnotatedBar>

            fun invoke(foo: Foo): Bar = rootFactory<MyFactory>()().a(foo)
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
        
        @Module
        object FooModule {
            @Given
            fun foo() = Foo()
        }
        
        @RootFactory
        typealias MyFactory = (FooModule) -> TestComponent1<Dep<Foo>>
        
        fun invoke() {
            rootFactory<MyFactory>()(FooModule).a
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
