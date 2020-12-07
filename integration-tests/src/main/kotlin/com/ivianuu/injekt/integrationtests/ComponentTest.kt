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
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class ComponentTest {

    @Test
    fun testSimple() = codegen(
        """
            @Binding fun foo() = Foo()
            @Binding fun bar(foo: Foo) = Bar(foo)

            @Component abstract class TestComponent { 
                abstract val bar: Bar
            }
            
            fun invoke(): Bar {
                return component<TestComponent>().bar
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testChild() = codegen(
        """
            @Component abstract class ParentComponent {
                abstract val childComponentFactory: (Foo) -> MyChildComponent
            }
            
            @ChildComponent
            abstract class MyChildComponent(@Binding protected val _foo: Foo) {
                abstract val foo: Foo
            }

            fun invoke(foo: Foo): Foo {
                return component<ParentComponent>().childComponentFactory(foo).foo
            }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testChildOverridesParentBinding() = codegen(
        """
            class Context
            
            val parentContext = Context()
            @Scoped(ParentComponent::class)
            @Binding fun parentContext() = parentContext
            
            val childContext = Context()
            @Scoped(MyChildComponent::class)
            @Binding fun childContext() = childContext
            
            @Component abstract class ParentComponent {
                abstract val childComponentFactory: () -> MyChildComponent
                abstract val context: Context
            }
            
            @ChildComponent
            abstract class MyChildComponent {
                abstract val context: Context
            }

            fun invoke(): List<Any> {
                val parentComponent = component<ParentComponent>()
                val childComponent = parentComponent.childComponentFactory()
                
                return listOf(
                    parentComponent.context,
                    parentComponent.context,
                    childComponent.context,
                    childComponent.context
                )
            }
    """
    ) {
        val (a1, a2, b1, b2) = invokeSingleFile<List<Any>>()
        assertSame(a1, a2)
        assertSame(b1, b2)
        assertNotSame(a1, b1)
    }

    @Test
    fun testChildWithAdditionalArguments() = codegen(
        """
            @Component abstract class ParentComponent {
                abstract val childComponentFactory: (Foo) -> MyChildComponent
            }
            
            @ChildComponent
            abstract class MyChildComponent {
                abstract val foo: Foo
            }

            fun invoke(foo: Foo): Foo {
                return component<ParentComponent>().childComponentFactory(foo).foo
            }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testUnscopedBindingReturnsDifferentInstance() = codegen(
        """
            @Component abstract class MyComponent { 
                abstract val foo: Foo
                @Binding 
                protected fun foo() = Foo()
            }
        
            val component: MyComponent = component<MyComponent>()
        
            fun invoke() = component.foo
    """
    ) {
        assertNotSame(invokeSingleFile(), invokeSingleFile())
    }

    @Test
    fun testScopedBindingReturnsSameInstance() = codegen(
        """
            @Component abstract class MyComponent { 
                abstract val foo: Foo
                @Scoped(MyComponent::class)
                @Binding protected fun foo() = Foo()
            }
        
            val component: MyComponent = component<MyComponent>()
        
            fun invoke() = component.foo
    """
    ) {
        assertSame(invokeSingleFile(), invokeSingleFile())
    }

    @Test
    fun testBoundCreatesInstancesInTarget() = codegen(
        """
            @Bound(ParentComponent::class)
            @Binding class Dep(val value: String)

            @Component abstract class ParentComponent(@Binding protected val string: String) {
                abstract val dep: Dep
                abstract val childFactory: (String) -> MyChildComponent
            }

            @ChildComponent
            abstract class MyChildComponent(@Binding protected val string: String) {
                abstract val dep: Dep
            }

            fun invoke(): String {
                val parentComponent = component<ParentComponent>("parent")
                val childComponent = parentComponent.childFactory("child")
                return childComponent.dep.value
            }
        """
    ) {
        assertEquals("parent", invokeSingleFile<String>())
    }

    @Test
    fun testGenericBindingsWithDifferentArgumentsHasDifferentIdentity() = codegen(
        """
            @Scoped(MyComponent::class)
            @Binding class Option<T>(val value: T)
            
            @Component abstract class MyComponent {
                abstract val stringOption: Option<String>
                abstract val intOption: Option<Int>
                
                @Binding protected fun string() = ""
                @Binding protected fun int() = 0
            }
            
            val component = component<MyComponent>()
            
            fun invoke(): List<Any> {
                return listOf(
                    component.stringOption,
                    component.stringOption,
                    component.intOption,
                    component.intOption
                )
            }
            
        """
    ) {
        val (a1, a2, b1, b2) = invokeSingleFile<List<Any>>()
        assertSame(a1, a2)
        assertSame(b1, b2)
        assertNotSame(a1, b1)
    }

    @Test
    fun testStarProjectedBindingsHasSharedIdentity() = codegen(
        """
            class Option<T>(val value: T)
            
            @Scoped(MyComponent::class)
            @Binding fun stringOption(value: String) = Option(value)
            
            @Component abstract class MyComponent {
                abstract val stringOption: Option<String> 
                abstract val starOption: Option<*>
                @Binding protected fun string() = ""
            }
            
            val component = component<MyComponent>()
            
            fun invoke(): Pair<Any, Any> {
                return component.stringOption to component.starOption
            }
        """
    ) {
        val (a1, a2) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a1, a2)
    }

    @Test
    fun testParentScopedBinding() = codegen(
        """
            @Component abstract class MyParentComponent {
                abstract val childFactory: () -> MyChildComponent
            
                @Binding protected fun foo() = Foo()
                
                @Scoped(MyParentComponent::class)
                @Binding protected fun bar(foo: Foo) = Bar(foo)
            }
            
            val parentComponent: MyParentComponent = component<MyParentComponent>()
            
            @ChildComponent
            abstract class MyChildComponent {
                abstract val bar: Bar
            }

            val childComponent = parentComponent.childFactory()
         
            fun invoke(): Bar {
                return childComponent.bar
            }
    """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testClassBinding() = codegen(
        """
            @Binding class AnnotatedBar(foo: Foo)
            
            @Component abstract class FooComponent {
                abstract val annotatedBar: AnnotatedBar
                
                @Binding protected fun foo() = Foo()
            }

            fun invoke() {
                component<FooComponent>().annotatedBar
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testNestedClassBinding() = codegen(
        """
            class Outer {
                @Binding class AnnotatedBar(foo: Foo)
            }
            @Component abstract class FooComponent {
                abstract val annotatedBar: Outer.AnnotatedBar
                
                @Binding protected fun foo() = Foo()
            }

            fun invoke() {
                component<FooComponent>().annotatedBar
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testConstructorBinding() = codegen(
        """
            class AnnotatedBar @Binding constructor(foo: Foo)
            
            @Component abstract class FooComponent {
                abstract val annotatedBar: AnnotatedBar
                
                @Binding protected fun foo() = Foo()
            }

            fun invoke() {
                component<FooComponent>().annotatedBar
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testObjectBinding() = codegen(
        """
            @Binding
            object AnnotatedBar
            
            @Component abstract class MyComponent {
                abstract val annotationBar: AnnotatedBar
            }
            
            fun invoke() {
                component<MyComponent>().annotationBar
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testPropertyBinding() = codegen(
        """
            @Component abstract class FooComponent {
                abstract val foo: Foo
                @Binding protected val _foo = Foo()
            }
            
            fun invoke(): Foo {
                return component<FooComponent>().foo
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testTopLevelFunctionBinding() = codegen(
        """
            @Binding fun foo() = Foo()
            
            @Component abstract class FooComponent {
                abstract val foo: Foo
            }

            fun invoke() {
                component<FooComponent>().foo
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testFunctionBindingInObject() = codegen(
        """
            object BarDeps {
                @Binding fun Foo.bar() = Bar(this)
                
                @Binding fun foo() = Foo()
            }
            
            @Component abstract class BarComponent {
                abstract val bar: Bar
            }

            fun invoke() {
                component<BarComponent>().bar
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testTopLevelPropertyBinding() = codegen(
        """
            @Binding val foo get() = Foo()
            
            @Component abstract class FooComponent {
                abstract val foo: Foo
            }

            fun invoke() {
                component<FooComponent>().foo
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGenericBindingClass() = codegen(
        """
            @Binding class Dep<T>(val value: T)
            
            @Component abstract class FooComponent {
                abstract val fooDep: Dep<Foo>
                @Binding protected fun foo() = Foo()
            }
            
            fun invoke() {
                component<FooComponent>().fooDep
            }
    """
    )

    @Test
    fun testGenericBindingFunction() = codegen(
        """    
            class Dep<T>(val value: T)
            
            @Component abstract class MyComponent { 
                abstract val fooDep: Dep<Foo>
                @Binding protected fun <T> dep(value: T) = Dep(value)
                @Binding protected fun foo() = Foo() 
            }

            fun invoke() {
                component<MyComponent>().fooDep
            }
    """
    )

    @Test
    fun testComplexGenericBindingFunction() = codegen(
        """    
            class Dep<A, B, C>(val value: A)
            
            @Component abstract class MyComponent { 
                abstract val dep: Dep<Foo, Foo, Foo>
                @Binding protected fun <A, B : A, C : B> dep(a: A) = Dep<A, A, A>(a)
                @Binding protected fun foo() = Foo()
            }
    """
    )

    @Test
    fun testComponentFunction() = codegen(
        """
            @Component abstract class FunctionComponent {
                abstract fun foo(): Foo
                
                @Binding protected fun _foo() = Foo()
            }
        """
    )

    @Test
    fun testComponentSuspendFunction() = codegen(
        """
            @Component abstract class SuspendFunctionComponent {
                abstract suspend fun bar(): Bar
                @Binding protected suspend fun _suspendFoo() = Foo()
                @Binding protected suspend fun _suspendBar(foo: Foo) = Bar(foo)
            }
        """
    )

    @Test
    fun testScopedSuspendFunction() = codegen(
        """
            @Component abstract class SuspendFunctionComponent {
                abstract suspend fun foo(): Foo
                @Scoped(SuspendFunctionComponent::class) 
                @Binding protected suspend fun _suspendFoo() = Foo()
            }
            
            private val component = component<SuspendFunctionComponent>()
            fun invoke(): Pair<Foo, Foo> {
                return runBlocking { component.foo() to component.foo() }
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testComponentComposableFunction() = codegen(
        """
            @Component abstract class SuspendFunctionComponent {
                @Composable
                abstract fun bar(): Bar
                @Composable
                @Binding protected fun _composableFoo() = Foo()
                @Composable
                @Binding protected fun _composableBar(foo: Foo) = Bar(foo)
            }
        """
    )

    // todo @Test
    // todo find a way to invoke composables
    fun testScopedComposableFunction() = codegen(
        """
            @Component abstract class ComposableFunctionComponent {
                @Composable
                abstract fun foo(): Foo
                @Binding(ComposableFunctionComponent::class)
                @Composable
                protected fun _composableFoo() = Foo()
            }
            
            private val component = component<ComposableFunctionComponent>()
            fun invoke(): Pair<Foo, Foo> {
                return component.foo() to component.foo()
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testComponentWithConstructorParameters() = codegen(
        """
            @Component abstract class MyComponent(@Binding protected val _foo: Foo) {
                abstract val foo: Foo
            }
            fun invoke(): Pair<Foo, Foo> {
                val foo = Foo()
                return foo to component<MyComponent>(foo).foo
            }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testNestedComponent() = codegen(
        """
            @Component abstract class BarComponent {
                abstract val bar: Bar
            
                @Binding protected fun foo() = Foo()
                
                @Module
                protected val nested = NestedModule()

                class NestedModule {
                    @Binding fun bar(foo: Foo) = Bar(foo)
                }
            }
            
            fun invoke(): Bar {
                return component<BarComponent>().bar
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testGenericNestedComponent() = codegen(
        """
            @Component abstract class MyComponent {
                abstract val foo: Foo
            
                @Module
                protected val fooModule = InstanceModule<Foo>(Foo())

                class InstanceModule<T>(@Binding val instance: T)
            }

            fun invoke(): Foo {
                return component<MyComponent>().foo
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testTypeWithStarProjectedArg() = codegen(
        """ 
            @Component abstract class MyComponent(@Binding protected val _list: List<*>) {
                abstract val list: List<*>
            }
        """
    )

    @Test
    fun testCanRequestStarProjectedType() = codegen(
        """ 
            class Store<S, A>
            
            @Binding fun stringStore() = Store<String, String>()
            
            @Component abstract class MyComponent {
                abstract val storeS: Store<String, *>
                abstract val storeA: Store<*, String>
            }
        """
    )

    @Test
    fun testStarProjectedTypeAmbiguity() = codegen(
        """ 
            class Store<S, A>
            
            @Binding fun stringStringStore() = Store<String, String>()
            
            @Binding fun stringIntStore() = Store<String, Int>()
            
            @Component abstract class MyComponent {
                abstract val store: Store<String, *>
            }
        """
    ) {
        assertInternalError("multiple")
    }

    // todo @Test
    fun testGenericBindingWithStarProjection() = codegen(
        """
            class Store<S, A>
            
            @Binding fun store() = Store<String, Int>()
            
            @Binding fun <S> Store<S, *>.storeState(): S = error("")
            
            @Component abstract class MyComponent {
                abstract val state: String
            }
        """
    )

    // todo @Test
    fun testGenericBindingWithIrrelevantTypeParameters() = codegen(
        """
            class Store<S, A>
            
            @Binding fun store() = Store<String, Int>()
            
            @Binding fun <S, A> Store<S, A>.storeState(): S = error("")
            
            @Component abstract class MyComponent {
                abstract val state: String
            }
        """
    )

    @Test
    fun testBindingPerComponent() = codegen(
        """
            @Component abstract class MyParentComponent {
                abstract val childFactory: () -> MyChildComponent
                abstract val foo: Foo
                @Scoped(MyParentComponent::class) @Binding protected fun parentFoo() = Foo()
            }

            @ChildComponent
            abstract class MyChildComponent {
                abstract val foo: Foo
                @Scoped(MyChildComponent::class) @Binding protected fun childFoo() = Foo()
            }

            fun invoke(): Pair<Foo, Foo> {
                val parent = component<MyParentComponent>()
                val child = parent.childFactory()
                return parent.foo to child.foo
            }
        """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testInjectingComponent() = codegen(
        """ 
            @Component abstract class SelfComponent {
                abstract val self: SelfComponent
            }

            fun invoke(): Pair<SelfComponent, SelfComponent> {
                val component = component<SelfComponent>()
                return component to component.self
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testInjectingParentComponent() = codegen(
        """ 
            @Component abstract class ParentComponent {
                abstract val childComponent: () -> MyChildComponent
                @ChildComponent
                abstract class MyChildComponent {
                    abstract val parent: ParentComponent
                }
            }

            fun invoke(): Pair<ParentComponent, ParentComponent> {
                val parent = component<ParentComponent>()
                val child = parent.childComponent()
                return parent to child.parent
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testGenericTypeAlias() = codegen(
        """
            interface Comparator<T> {
                fun compare(a: T, b: T): Int
            }
            typealias AliasComparator<T> = Comparator<T>
            
            @Component abstract class MyComponent {
                abstract val compareInt: compare<Int>
                @Binding protected fun intComparator(): AliasComparator<Int> = error("")
            }

            typealias compare<T> = (T, T) -> Int
            @Binding fun <T> provideCompare(comparator: AliasComparator<T>): compare<T> = { a, b ->
                comparator.compare(a, b)
            }
        """
    )

    @Test
    fun testBindingTypeParameterInference() = codegen(
        """
            @Binding fun map() = mapOf("a" to 0)
            
            @Binding fun <M : Map<K, V>, K : CharSequence, V> firstKey(map: M): K = map.keys.first()

            @Component abstract class MyComponent {
                abstract val key: String
            }
        """
    )

    @Test
    fun testComponentDoesNotImplementFinalFunction() = codegen(
        """
            @Component abstract class MyComponent {
                fun string() = ""
            }
        """
    )

}
