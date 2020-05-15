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

import junit.framework.Assert.assertTrue
import org.junit.Test

class ObjectGraphFunctionsTest {

    @Test
    fun testInlineGetWithGenericComponentAndGenericInstance() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <C : Any, T> getInstance(component: C): T {
            return component.get<T>()
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            return getInstance<TestComponent, Foo>(component)
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testInlineGetWithGenericInstance() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <T> getInstance(component: TestComponent): T {
            return component.get<T>()
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            return getInstance<Foo>(component)
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testInlineGetWithGenericComponent() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <C : Any> getInstance(component: C): Foo {
            return component.get<Foo>()
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            return getInstance(component)
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedInlineGetWithGenericComponentAndGenericInstance() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <C : Any, T> getInstance(component: C): T {
            return component.get<T>()
        }
        
        fun <C : Any, T> nestedGetInstance(component: C): T {
            return getInstance<C, T>(component)
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            return nestedGetInstance<TestComponent, Foo>(component)
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedInlineGetWithGenericInstance() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <C : Any, T> getInstance(component: C): T {
            return component.get<T>()
        }
        
        fun <T> nestedGetInstance(component: TestComponent): T {
            return getInstance<TestComponent, T>(component)
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            return nestedGetInstance<Foo>(component)
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedInlineGetWithGenericComponent() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <C : Any, T> getInstance(component: C): T {
            return component.get<T>()
        }
        
        fun <C : Any> nestedGetInstance(component: C): Foo {
            return getInstance<C, Foo>(component)
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            return getInstance(component)
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testInlineInjectWithGenericComponentAndGenericInstance() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <C : Any, T> injectInstance(component: C, instance: T) { 
            component.inject(instance)
        }
        
        class MyClass {
            val foo: Foo by inject()
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            val myClass = MyClass()
            injectInstance(component, myClass)
            return myClass.foo
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testInlineInjectWithGenericInstance() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <T> injectInstance(component: TestComponent, instance: T) {
            component.inject(instance)
        }
        
        class MyClass {
            val foo: Foo by inject()
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            val myClass = MyClass()
            injectInstance(component, myClass)
            return myClass.foo
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testInlineInjectWithGenericComponent() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <C : Any> injectInstance(component: C, instance: MyClass) {
            component.inject(instance)
        }
        
        class MyClass {
            val foo: Foo by inject()
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            val myClass = MyClass()
            injectInstance(component, myClass)
            return myClass.foo
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedInlineInjectWithGenericComponentAndGenericInstance() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <C : Any, T> injectInstance(component: C, instance: T) { 
            component.inject(instance)
        }
        
        fun <C : Any, T> nestedInjectInstance(component: C, instance: T) { 
            injectInstance(component, instance)
        }
        
        class MyClass {
            val foo: Foo by inject()
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            val myClass = MyClass()
            injectInstance(component, myClass)
            return myClass.foo
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedInlineInjectWithGenericInstance() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <C : Any, T> injectInstance(component: C, instance: T) {
            component.inject(instance)
        }
        
        fun <T> nestedInjectInstance(component: TestComponent, instance: T) {
            injectInstance(component, instance)
        }
        
        class MyClass {
            val foo: Foo by inject()
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            val myClass = MyClass()
            injectInstance(component, myClass)
            return myClass.foo
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedInlineInjectWithGenericComponent() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestComponent {
            transient { Foo() }
            return createImpl() 
        }
        
        fun <C : Any, T> injectInstance(component: C, instance: T) { 
            component.inject(instance)
        }
        
        fun <C : Any> nestedInjectInstance(component: C, instance: MyClass) { 
            injectInstance(component, instance)
        }
        
        class MyClass {
            val foo: Foo by inject()
        }
        
        fun invoke(): Foo { 
            generateCompositions()
            val component = compositionFactoryOf<TestComponent, () -> TestComponent>()()
            val myClass = MyClass()
            injectInstance(component, myClass)
            return myClass.foo
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

}
