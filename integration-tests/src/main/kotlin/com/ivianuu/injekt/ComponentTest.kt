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

package com.ivianuu.injekt

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class ComponentTest {

    @Test
    fun testSimple() = codegen(
        """
        @Component
        interface MyComponent {
            @Component.Factory
            interface Factory {
                fun create(): MyComponent
            }
        }
        
        @Given @Reader
        fun foo() = Foo()
        @Given @Reader
        fun bar() = Bar(given())
        
        fun invoke(): Bar {
            initializeComponents()
            val component = componentFactory<MyComponent.Factory>().create()
            return component.runReader { given<Bar>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testSimpleWithChild() = codegen(
        """
        @Component
        interface ParentComponent {
            @Component.Factory
            interface Factory {
                fun create(): ParentComponent
            }
        }
        
        @Component(parent = ParentComponent::class)
        interface ChildComponent {
            @Component.Factory
            interface Factory {
                fun create(): ChildComponent
            }
        }
        
        @Given(ParentComponent::class) @Reader
        fun foo() = Foo()
        @Given @Reader
        fun bar() = Bar(given())
        
        fun invoke(): Bar {
            initializeComponents()
            val childComponent = componentFactory<ParentComponent.Factory>().create().runReader {
                given<ChildComponent.Factory>().create()
            }
            return childComponent.runReader { given<Bar>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testUnscoped() = codegen(
        """
        @Given @Reader
        fun foo() = Foo()
        
        val component by lazy {
            initializeComponents()
            componentFactory<TestComponent.Factory>().create()
        }
        
        fun invoke() = component.runReader { given<Foo>() }
    """
    ) {
        assertNotSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testScoped() = codegen(
        """
        @Given(TestComponent::class) @Reader
        fun foo() = Foo()
        
        val component by lazy {
            initializeComponents()
            componentFactory<TestComponent.Factory>().create()
        }
        
        fun invoke() = component.runReader { given<Foo>() }
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
        @Reader 
        class AnnotatedBar {
            private val foo: Foo = given()
        }
        
        @Given
        fun foo(): Foo = Foo()

        val component by lazy {
            initializeComponents()
            componentFactory<TestComponent.Factory>().create()
        }

        fun invoke() = component.runReader { given<AnnotatedBar>() }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenProperty() = codegen(
        """
        @Given
        val foo = Foo()
        
        val component by lazy {
            initializeComponents()
            componentFactory<TestComponent.Factory>().create()
        }

        fun invoke() = component.runReader { given<Foo>() }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testAssistedGivenFunction() = codegen(
        """ 
        @Given
        fun bar(foo: Foo) = Bar(foo)

        val component by lazy {
            initializeComponents()
            componentFactory<TestComponent.Factory>().create()
        }

        fun invoke() = component.runReader { given<(Foo) -> Bar>() }
    """
    ) {
        val barFactory = invokeSingleFile<(Foo) -> Bar>()
        barFactory(Foo())
    }

    @Test
    fun testAssistedGivenClass() = codegen(
        """ 
        @Given
        class AnnotatedBar(foo: Foo)

        val component by lazy {
            initializeComponents()
            componentFactory<TestComponent.Factory>().create()
        }

        fun invoke() = component.runReader { given<(Foo) -> AnnotatedBar>() }
    """
    ) {
        val barFactory = invokeSingleFile<(Foo) -> Any>()
        barFactory(Foo())
    }

    @Test
    fun testComponentBinding() = codegen(
        """
        fun invoke(): Pair<TestComponent, TestComponent> {
            initializeComponents()
            val component = componentFactory<TestComponent.Factory>().create()
            return component.runReader { 
                component to given<TestComponent>()
            }
        }
    """
    ) {
        val (component, dep) = invokeSingleFile<Pair<*, *>>()
        assertSame(component, dep)
    }

    @Test
    fun testGenericAnnotatedClass() = codegen(
        """
        @Given @Reader class Dep<T> {
            val value: T = given()
        }
        
        @Given fun foo() = Foo() 
        
        fun invoke() {
            initializeComponents()
            componentFactory<TestComponent.Factory>().create().runReader {
                given<Dep<Foo>>()
            }
        }
    """
    )

    @Test
    fun testGenericProvider() = codegen(
        """
        @Given class Dep<T>(val value: T)
        
        @Factory
        fun factory(): TestComponent2<Dep<String>, Dep<Int>> {
            unscoped { "hello world" }
            unscoped { 0 }
            return create()
        }
    """
    )

    @Test
    fun testComponentInput() = codegen(
        """
        @Component
        interface MyComponent {
            @Component.Factory
            interface Factory {
                fun create(foo: Foo): MyComponent
            }
        }
        
        fun invoke(): Pair<Foo, Foo> {
            initializeComponents()
            val foo = Foo()
            val component = componentFactory<MyComponent.Factory>().create(foo)
            return foo to component.runReader { given<Foo>() }
        }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

}
