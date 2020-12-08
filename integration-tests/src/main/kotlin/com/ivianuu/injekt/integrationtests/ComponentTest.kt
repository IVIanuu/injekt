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
    fun testSimpleInstance() = codegen(
        """
            @Binding fun foo() = Foo()
            @Binding fun bar(foo: Foo) = Bar(foo)
            
            fun invoke(): Bar {
                return create<Bar>()
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testSimpleComponent() = codegen(
        """
            @Binding fun foo() = Foo()
            @Binding fun bar(foo: Foo) = Bar(foo)

            @Component interface TestComponent { 
                val bar: Bar
            }
            
            fun invoke(): Bar {
                return create<TestComponent>().bar
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testChild() = codegen(
        """
            @Component interface ParentComponent {
                val childComponentFactory: (Foo) -> MyChildComponent
            }
            
            @Component interface MyChildComponent {
                val foo: Foo
            }

            fun invoke(foo: Foo): Foo {
                return create<ParentComponent>().childComponentFactory(foo).foo
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
            @Scoped(TestScope1::class)
            @Binding fun parentContext() = parentContext
            
            val childContext = Context()
            @Scoped(TestScope2::class)
            @Binding fun childContext() = childContext
            
            @Scoped(TestScope1::class) @Component interface ParentComponent {
                val childComponentFactory: () -> MyChildComponent
                val context: Context
            }
            
            @Scoped(TestScope2::class) @Component interface MyChildComponent {
                val context: Context
            }

            fun invoke(): List<Any> {
                val parentComponent = create<ParentComponent>()
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
            @Component interface ParentComponent {
                val childComponentFactory: (Foo) -> MyChildComponent
            }
            
            @Component interface MyChildComponent {
                val foo: Foo
            }

            fun invoke(foo: Foo): Foo {
                return create<ParentComponent>().childComponentFactory(foo).foo
            }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testUnscopedBindingReturnsDifferentInstance() = codegen(
        """
            @Binding fun foo() = Foo()

            val fooFactory = create<() -> Foo>()
        
            fun invoke() = fooFactory()
    """
    ) {
        assertNotSame(invokeSingleFile(), invokeSingleFile())
    }

    @Test
    fun testScopedBindingReturnsSameInstance() = codegen(
        """
            @Scoped @Binding fun foo() = Foo()

            val component: () -> Foo = create<() -> Foo>()
        
            fun invoke() = component()
    """
    ) {
        assertSame(invokeSingleFile(), invokeSingleFile())
    }

    @Test
    fun testBoundCreatesInstancesInTarget() = codegen(
        """
            @Bound(TestScope1::class)
            @Binding class Dep(val value: String)

            @Scoped(TestScope1::class) @Component interface ParentComponent {
                val dep: Dep
                val childFactory: (String) -> MyChildComponent
            }

            @Component interface MyChildComponent {
                val dep: Dep
            }

            fun invoke(): String {
                val parentComponent = create<ParentComponent>("parent")
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
            @Scoped(TestScope1::class)
            @Binding class Option<T>(val value: T)

            @Binding fun string() = ""
            @Binding fun int() = 0

            @Scoped(TestScope1::class) @Component interface MyComponent {
                val stringOption: Option<String>
                val intOption: Option<Int>
            }
            
            val component = create<MyComponent>()
            
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
            
            @Scoped(TestScope1::class)
            @Binding fun stringOption(value: String) = Option(value)
            @Binding fun string() = ""

            @Scoped(TestScope1::class) @Component interface MyComponent {
                val stringOption: Option<String> 
                val starOption: Option<*>
            }
            
            val component = create<MyComponent>()
            
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
            @Binding fun foo() = Foo()
                
            @Scoped(TestScope1::class)
            @Binding fun bar(foo: Foo) = Bar(foo)

            @Scoped(TestScope1::class) @Component interface MyParentComponent {
                val childFactory: () -> MyChildComponent
            }
            
            val parentComponent: MyParentComponent = create<MyParentComponent>()
            
            @Component interface MyChildComponent {
                val bar: Bar
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
            @Binding fun foo() = Foo()

            fun invoke() {
                create<AnnotatedBar>()
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

            @Binding fun foo() = Foo()

            @Component interface FooComponent {
                val annotatedBar: Outer.AnnotatedBar
            }

            fun invoke() {
                create<FooComponent>().annotatedBar
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testConstructorBinding() = codegen(
        """
            class AnnotatedBar @Binding constructor(foo: Foo)
            @Binding fun foo() = Foo()

            fun invoke() {
                create<AnnotatedBar>()
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testObjectBinding() = codegen(
        """
            @Binding object AnnotatedBar
            fun invoke() {
                create<AnnotatedBar>()
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testTopLevelFunctionBinding() = codegen(
        """
            @Binding fun foo() = Foo()
            
            @Component interface FooComponent {
                val foo: Foo
            }

            fun invoke() {
                create<FooComponent>().foo
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

            fun invoke() {
                create<Bar>()
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testTopLevelPropertyBinding() = codegen(
        """
            @Binding val foo get() = Foo()
            fun invoke() {
                create<Foo>()
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGenericBindingClass() = codegen(
        """
            @Binding class Dep<T>(val value: T)
            @Binding fun foo() = Foo()
            fun invoke() {
                create<Dep<Foo>>()
            }
    """
    )

    @Test
    fun testGenericBindingFunction() = codegen(
        """    
            class Dep<T>(val value: T)

            @Binding fun <T> dep(value: T) = Dep(value)
            @Binding fun foo() = Foo() 

            @Component interface MyComponent { 
                val fooDep: Dep<Foo>
            }

            fun invoke() {
                create<MyComponent>().fooDep
            }
    """
    )

    @Test
    fun testComplexGenericBindingFunction() = codegen(
        """    
            class Dep<A, B, C>(val value: A)
            
            @Component interface MyComponent { 
                val dep: Dep<Foo, Foo, Foo>
                @Binding fun <A, B : A, C : B> dep(a: A) = Dep<A, A, A>(a)
                @Binding fun foo() = Foo()
            }
    """
    )

    @Test
    fun testComponentFunction() = codegen(
        """
            @Component interface FunctionComponent {
                abstract fun foo(): Foo
                
                @Binding fun _foo() = Foo()
            }
        """
    )

    @Test
    fun testComponentSuspendFunction() = codegen(
        """
            @Binding suspend fun suspendFoo() = Foo()
            @Binding suspend fun suspendBar(foo: Foo) = Bar(foo)

            @Component interface SuspendFunctionComponent {
                suspend fun bar(): Bar
            }

            fun invoke() = create<SuspendFunctionComponent>()
        """
    )

    @Test
    fun testScopedSuspendFunction() = codegen(
        """
            @Scoped(TestScope1::class) 
            @Binding suspend fun _suspendFoo() = Foo()

            @Scoped(TestScope1::class) @Component interface SuspendFunctionComponent {
                suspend fun foo(): Foo
            }
            
            private val component = create<SuspendFunctionComponent>()
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
            @Component interface SuspendFunctionComponent {
                @Composable
                abstract fun bar(): Bar
                @Composable
                @Binding fun _composableFoo() = Foo()
                @Composable
                @Binding fun _composableBar(foo: Foo) = Bar(foo)
            }
        """
    )

    // todo @Test
    // todo find a way to invoke composables
    fun testScopedComposableFunction() = codegen(
        """
            @Component interface ComposableFunctionComponent {
                @Composable
                abstract fun foo(): Foo
                @Binding(ComposableFunctionComponent::class)
                @Composable
                fun _composableFoo() = Foo()
            }
            
            private val component = create<ComposableFunctionComponent>()
            fun invoke(): Pair<Foo, Foo> {
                return component.foo() to component.foo()
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testCanRequestStarProjectedType() = codegen(
        """ 
            class Store<S, A>
            
            @Binding fun stringStore() = Store<String, String>()
            
            @Component interface MyComponent {
                val storeS: Store<String, *>
                val storeA: Store<*, String>
            }
        """
    )

    @Test
    fun testStarProjectedTypeAmbiguity() = codegen(
        """ 
            class Store<S, A>
            
            @Binding fun stringStringStore() = Store<String, String>()
            
            @Binding fun stringIntStore() = Store<String, Int>()
            
            fun invoke() {
                create<Store<String, *>>()
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
            
            @Component interface MyComponent {
                val state: String
            }
        """
    )

    // todo @Test
    fun testGenericBindingWithIrrelevantTypeParameters() = codegen(
        """
            class Store<S, A>
            
            @Binding fun store() = Store<String, Int>()
            
            @Binding fun <S, A> Store<S, A>.storeState(): S = error("")
            
            @Component interface MyComponent {
                val state: String
            }
        """
    )

    @Test
    fun testBindingPerComponent() = codegen(
        """
            @Scoped(TestScope1::class) @Binding fun parentFoo() = Foo()
            
            @Scoped(TestScope1::class) @Component interface MyParentComponent {
                val childFactory: () -> MyChildComponent
                val foo: Foo
            }

            @Scoped(TestScope2::class) @Binding fun childFoo() = Foo()

            @Scoped(TestScope2::class) @Component interface MyChildComponent {
                val foo: Foo
            }

            fun invoke(): Pair<Foo, Foo> {
                val parent = create<MyParentComponent>()
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
            @Component interface SelfComponent {
                val self: SelfComponent
            }

            fun invoke(): Pair<SelfComponent, SelfComponent> {
                val component = create<SelfComponent>()
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
            @Component interface ParentComponent {
                val childComponent: () -> MyChildComponent
                @Component interface MyChildComponent {
                    val parent: ParentComponent
                }
            }

            fun invoke(): Pair<ParentComponent, ParentComponent> {
                val parent = create<ParentComponent>()
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
            
            @Component interface MyComponent {
                val compareInt: compare<Int>
                @Binding fun intComparator(): AliasComparator<Int> = error("")
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

            @Component interface MyComponent {
                val key: String
            }
        """
    )

}
