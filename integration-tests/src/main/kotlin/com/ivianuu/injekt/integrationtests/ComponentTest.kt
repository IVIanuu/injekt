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
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class ComponentTest {

    @Test
    fun testSimple() = codegen(
        """
            @Component
            abstract class TestComponent { 
                abstract val bar: Bar
                @Binding protected fun foo() = Foo()
                @Binding protected fun bar(foo: Foo) = Bar(foo)
            }
            
            fun invoke(): Bar {
                return TestComponentImpl().bar
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testWithChild() = codegen(
        """
            @Component
            abstract class ParentComponent {
                abstract val childComponentFactory: (Foo) -> MyChildComponent
            }
            
            @ChildComponent
            abstract class MyChildComponent(@Binding protected val _foo: Foo) {
                abstract val foo: Foo
            }

            fun invoke(foo: Foo): Foo {
                return ParentComponentImpl().childComponentFactory(foo).foo
            }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testScopedBinding() = codegen(
        """
            @Module
            object MyModule {
                @Binding(TestComponent1::class)
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
    fun testParentScopedBinding() = codegen(
        """
            @Module
            object MyModule {
                @Binding
                fun foo() = Foo()
                
                @Binding(TestParentComponent1::class)
                fun bar(foo: Foo) = Bar(foo)
            }
            
            @RootFactory
            typealias MyParentFactory = (MyModule) -> TestParentComponent1<MyChildFactory>
            val parentComponent = rootFactory<MyParentFactory>()(MyModule)
            
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
    fun testBindingClass() = codegen(
        """
            @Binding
            class AnnotatedBar(foo: Foo)
            
            @Module
            object FooModule {
                @Binding
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
    fun testBindingObject() = codegen(
        """
            @Binding
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
    fun testBindingProperty() = codegen(
        """
            @Module
            object FooModule {
                @Binding val foo = Foo()
            }
    
            @RootFactory
            typealias MyFactory = (FooModule) -> TestComponent1<Foo>
            
            fun invoke() = rootFactory<MyFactory>()(FooModule).a
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testProvider() = codegen(
        """
            @Module
            object FooModule {
                @Binding
                fun foo() = Foo()
            }
            
            @RootFactory
            typealias MyFactory = (FooModule) -> TestComponent1<() -> Foo>
            
            fun invoke() {
                rootFactory<MyFactory>()(FooModule).a()
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testAssistedBindingFunction() = codegen(
        """
            @Module
            object BarModule {
                @Binding
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
    fun testAssistedBindingClass() = codegen(
        """
            @Binding
            class AnnotatedBar(@Assisted foo: Foo)
            
            @RootFactory
            typealias MyFactory = () -> TestComponent1<(Foo) -> AnnotatedBar>

            fun invoke(foo: Foo): AnnotatedBar = rootFactory<MyFactory>()().a(foo)
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testGenericBindingClass() = codegen(
        """
            @Binding class Dep<T>(val value: T)
            
            @Module
            object FooModule {
                @Binding
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
    fun testGenericBindingFunction() = codegen(
        """    
            class Dep<T>(val value: T)
            
            @Module
            object MyModule { 
                @Binding fun <T> dep(value: T) = Dep<T>(value)
                @Binding fun foo() = Foo() 
            }
            
            @RootFactory
            typealias MyFactory = (MyModule) -> TestComponent1<Dep<Foo>>
    
            fun invoke() {
                rootFactory<MyFactory>()(MyModule).a
            }
    """
    )

    @Test
    fun testComplexGenericBindingFunction() = codegen(
        """    
            class Dep<A, B, C>(val value: A)
            
            @Module
            object MyModule { 
                @Binding fun <A, B : A, C : B> dep(a: A) = Dep<A, A, A>(a)
                @Binding fun foo() = Foo() 
            }
            
            @RootFactory
            typealias MyFactory = (MyModule) -> TestComponent1<Dep<Foo, Foo, Foo>>
    """
    )

    @Test
    fun testComponentFunction() = codegen(
        """
            @Module
            object FunctionModule {
                @Binding
                fun foo() = Foo()
            }

            interface FunctionComponent { 
                fun foo() = Foo()
            }
            
            @RootFactory
            typealias MyFactory = (FunctionModule) -> FunctionComponent
        """
    )

    @Test
    fun testComponentSuspendFunction() = codegen(
        """
            @Module
            object SuspendFunctionModule {
                @Binding
                suspend fun suspendFoo() = Foo()
            }

            interface SuspendComponent {
                suspend fun foo() = Foo()
            }
            
            @RootFactory
            typealias MyFactory = (SuspendFunctionModule) -> SuspendComponent
        """
    )

    @Test
    fun testInput() = codegen(
        """
            @RootFactory
            typealias MyFactory = (Foo) -> TestComponent1<Foo>
            fun invoke(): Pair<Foo, Foo> {
                val foo = Foo()
                return foo to rootFactory<MyFactory>()(foo).a
            }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testNestedModule() = codegen(
        """
            @Module
            class FooModule {
                @Binding
                fun foo() = Foo()
                
                @Module
                val barModule = BarModule()
                
                @Module
                class BarModule {
                    @Binding
                    fun bar(foo: Foo) = Bar(foo)
                }
            }
            
            @RootFactory
            typealias MyFactory = (FooModule) -> TestComponent1<Bar>
            
            fun invoke(): Bar {
                return rootFactory<MyFactory>()(FooModule()).a
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testGenericNestedModule() = codegen(
        """
            @Module
            class OuterModule {
                @Module
                val fooModule = InstanceModule<Foo>(Foo())
                
                @Module
                class InstanceModule<T>(@Binding val instance: T)
            }
            
            @RootFactory
            typealias MyFactory = (OuterModule) -> TestComponent1<Foo>
            
            fun invoke(): Foo {
                return rootFactory<MyFactory>()(OuterModule()).a
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testMissingBindingFails() = codegen(
        """
            class Dep
            
            @RootFactory
            typealias MyFactory = () -> TestComponent1<Dep>
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDeeplyMissingBindingFails() = codegen(
        """
            @Module
            object MyModule {
                @Binding
                fun bar(foo: Foo) = Bar(foo)
        
                @Binding
                fun baz(bar: Bar, foo: Foo) = Baz(bar, foo)
            }
            
            @RootFactory
            typealias MyFactory = (MyModule) -> TestComponent1<Baz>
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDistinctTypeParameter() = codegen(
        """
            @Module
            object MyModule {
                @SetElements fun setA() = setOf("a")
                @SetElements fun setB() = setOf(0)
            }
            
            @RootFactory
            typealias MyFactory = (MyModule) -> TestComponent2<Set<String>, Set<Int>>
            
            fun invoke(): Pair<Set<String>, Set<Int>> {
                val component = rootFactory<MyFactory>()(MyModule)
                return component.a to component.b
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
            
            @Module
            object FooModule {
                @Binding fun foo1(): Foo1 = Foo()
                @Binding fun foo2(): Foo2 = Foo()
            }
            
            @RootFactory
            typealias MyFactory = (FooModule) -> TestComponent2<Foo1, Foo2>
            
            fun invoke(): Pair<Foo, Foo> {
                val component = rootFactory<MyFactory>()(FooModule)
                return component.a to component.b
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
                        @Binding fun foo1(): Foo1 = Foo()
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
                        @Binding fun foo2(): Foo2 = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    @Component
                    abstract class MyComponent {
                        abstract val foo1: Foo1
                        abstract val foo2: Foo2
                        
                        @Module protected val foo1Module = Foo1Module 
                        @Module protected val foo2Module = Foo2Module
                    }
                    fun invoke(): Pair<Foo1, Foo2> {
                        val component = MyComponentImpl()
                        return component.foo1 to component.foo2
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
                @Binding fun foo(): Foo = Foo()
                @Binding fun nullableFoo(): Foo? = null
            }
            
            @RootFactory
            typealias MyFactory = (FooModule) -> TestComponent1<Foo>
            """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testReturnsInstanceForNullableBinding() = codegen(
        """
            @Component
            abstract class FooComponent {
                abstract val foo: Foo?
                @Binding protected fun foo(): Foo = Foo()
            }
            
            fun invoke(): Foo? {
                return FooComponentImpl().foo
            }
        """
    ) {
        assertNotNull(invokeSingleFile())
    }

    @Test
    fun testReturnsNullOnMissingNullableBinding() = codegen(
        """
            @Component
            abstract class FooComponent {
                abstract val foo: Foo?
            }
            fun invoke(): Foo? { 
                return FooComponentImpl().foo
            }
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testTypeWithStarProjectedArg() = codegen(
        """ 
            @Component
            abstract class MyComponent(@Binding protected val _list: List<*>) {
                abstract val list: List<*>
            }
        """
    )

    @Test
    fun testPrefersExplicitOverImplicitBinding() = codegen(
        """
            @Binding
            class Dep
            
            @Component
            abstract class MyComponent(@Binding protected val _dep: Dep) { 
                abstract val dep: Dep
            }
            
            fun invoke(): Pair<Any, Any> {
                val dep = Dep()
                return dep to MyComponentImpl(dep).dep
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testDuplicatedBindingsFails() = codegen(
        """
            @Component
            abstract class MyComponent(
                @Binding protected val foo1: Foo,
                @Binding protected val foo2: Foo
            ) {
                abstract val foo: Foo
            }
        """
    ) {
        assertInternalError("multiple bindings")
    }

    @Test
    fun testBindingPerComponent() = codegen(
        """
            @Module
            object ParentModule {
                @Binding(TestParentComponent2::class) fun parentFoo() = Foo()
            }
            
            @RootFactory
            typealias MyParentFactory = (ParentModule) -> TestParentComponent2<Foo, MyChildFactory>
            
            @Module
            object ChildModule {
                @Binding(TestChildComponent1::class) fun childFoo() = Foo()
            }
            
            @ChildFactory
            typealias MyChildFactory = (ChildModule) -> TestChildComponent1<Foo>
            
            fun invoke(): Pair<Foo, Foo> {
                val parent = rootFactory<MyParentFactory>()(ParentModule)
                val child = parent.b(ChildModule)
                return parent.a to child.a
            }
        """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testInjectingComponent() = codegen(
        """ 
            @Component
            abstract class SelfComponent {
                abstract val self: SelfComponent
            }

            fun invoke(): Pair<SelfComponent, SelfComponent> {
                val component = SelfComponentImpl()
                return component to component.self
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testPrefersExactType() = codegen(
        """
            class Dep<T>(val value: T)
            
            @Component
            abstract class FooComponent {
                abstract val fooDep: Dep<Foo>
                
                @Binding
                protected fun <T> genericDep(t: T): Dep<T> = Dep(t)
                
                @Binding
                protected fun fooDep(foo: Foo): Dep<Foo> = Dep(foo)
                
                @Binding
                protected fun foo() = Foo()
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
            
            @Module
            object MyModule {
                @Binding
                fun intComparator(): AliasComparator<Int> = error("")
            }
            
            @RootFactory
            typealias MyFactory = (MyModule) -> TestComponent1<compare<Int>>

            @Binding
            fun <T> compare(@Assisted a: T, @Assisted b: T, comparator: AliasComparator<T>): Int = comparator
                .compare(a, b)

        """
    )

    @Test
    fun testCircularDependencyFails() = codegen(
        """
            @Binding class A(b: B)
            @Binding class B(a: A)
            
            @RootFactory
            typealias MyFactory = () -> TestComponent1<B>
        """
    ) {
        assertInternalError("circular")
    }

    @Test
    fun testProviderBreaksCircularDependency() = codegen(
        """
            @Binding class A(b: B)
            @Binding(TestComponent1::class) class B(a: () -> A)
            
            @RootFactory
            typealias MyFactory = () -> TestComponent1<B>
        """
    ) {
        assertOk()
    }

    @Test
    fun testIrrelevantProviderInChainDoesNotBreakCircularDependecy() = codegen(
        """
            @Binding class A(b: () -> B)
            @Binding class B(b: C)
            @Binding class C(b: B)
            
            @RootFactory
            typealias MyFactory = () -> TestComponent1<C>
        """
    ) {
        assertInternalError("circular")
    }

    @Test
    fun testAssistedBreaksCircularDependency() = codegen(
        """
            @Binding class A(@Assisted b: B)
            @Binding(TestComponent1::class) class B(a: (B) -> A)
            
            @RootFactory
            typealias MyFactory = () -> TestComponent1<B>
        """
    ) {
        assertOk()
    }

}
