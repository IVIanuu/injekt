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
        
        @Unscoped @Reader
        fun foo() = Foo()
        @Unscoped @Reader
        fun bar() = Bar(get())
        
        fun invoke(): Bar {
            buildComponents()
            val component = componentFactory<MyComponent.Factory>().create()
            return component.runReader { get<Bar>() }
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
        
        @Scoped(ParentComponent::class) @Reader
        fun foo() = Foo()
        @Unscoped @Reader
        fun bar() = Bar(get())
        
        fun invoke(): Bar {
            buildComponents()
            val childComponent = componentFactory<ParentComponent.Factory>().create().runReader {
                get<ChildComponent.Factory>().create()
            }
            return childComponent.runReader { get<Bar>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testUnscoped() = codegen(
        """
        @Unscoped @Reader
        fun foo() = Foo()
        
        val component by lazy {
            buildComponents()
            componentFactory<TestComponent.Factory>().create()
        }
        
        fun invoke() = component.runReader { get<Foo>() }
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
        @Scoped(TestComponent::class) @Reader
        fun foo() = Foo()
        
        val component by lazy {
            buildComponents()
            componentFactory<TestComponent.Factory>().create()
        }
        
        fun invoke() = component.runReader { get<Foo>() }
    """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testUnscopedProvider() = codegen(
        """
        @Unscoped @Reader
        fun foo() = Foo()
        
        val component by lazy {
            buildComponents()
            componentFactory<TestComponent.Factory>().create()
        }
        
        fun invoke() = component.runReader { get<() -> Foo>() }
    """
    ) {
        val provider = invokeSingleFile<() -> Foo>()
        assertNotSame(provider(), provider())
    }

    @Test
    fun testScopedProvider() = codegen(
        """
        @Scoped(TestComponent::class) @Reader
        fun foo() = Foo()
        
        val component by lazy {
            buildComponents()
            componentFactory<TestComponent.Factory>().create()
        }
        
        fun invoke() = component.runReader { get<() -> Foo>() }
    """
    ) {
        val provider = invokeSingleFile<() -> Foo>()
        assertSame(provider(), provider())
    }

    @Test
    fun testAnnotatedClass() = codegen(
        """
        @Unscoped class AnnotatedBar(foo: Foo)
        
        @Factory
        fun factory(): TestComponent1<AnnotatedBar> {
            unscoped<Foo>()
            return create()
        }
        
        val component = factory()
        fun invoke() = component.a
    """
    ) {
        assertNotSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testEmpty() = codegen(
        """
        interface TestComponent {
        }
        
        @Factory
        fun invoke(): TestComponent = create()
         """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testFactoryImplementationBinding() = codegen(
        """
        @Unscoped class Dep(val testComponent: TestComponent1<Dep>)
        
        @Factory
        fun factory(): TestComponent1<Dep> = create()
        
        fun invoke(): Pair<TestComponent1<Dep>, TestComponent1<Dep>> = factory().let {
            it to it.a.testComponent
        }
    """
    ) {
        val (component, dep) = invokeSingleFile<Pair<*, *>>()
        assertSame(component, dep)
    }

    @Test
    fun testGenericAnnotatedClass() = codegen(
        """
        @Unscoped class Dep<T>(val value: T)
        
        @Factory
        fun factory(): TestComponent2<Dep<String>, Dep<Int>> {
            unscoped { "hello world" }
            unscoped { 0 }
            return create()
        }
    """
    )

}
