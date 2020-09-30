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
    fun testProvider() = codegen(
        """
            @Module
            object FooModule {
                @Given
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
            typealias MyFactory = () -> TestComponent1<(Foo) -> AnnotatedBar>

            fun invoke(foo: Foo): AnnotatedBar = rootFactory<MyFactory>()().a(foo)
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testGenericGivenClass() = codegen(
        """
        @Given class Dep<T>(val value: T)
        
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
            
            @Module
            object MyModule { 
                @Given fun <T> dep(value: T) = Dep<T>(value)
                @Given fun foo() = Foo() 
            }
            
            @RootFactory
            typealias MyFactory = (MyModule) -> TestComponent1<Dep<Foo>>
    
            fun invoke() {
                rootFactory<MyFactory>()(MyModule).a
            }
    """
    )

    @Test
    fun testComplexGenericGivenFunction() = codegen(
        """    
        class Dep<A, B, C>(val value: A)
        
        @Module
        object MyModule { 
            @Given fun <A, B : A, C : B> dep(a: A) = Dep<A, A, A>(a)
            @Given fun foo() = Foo() 
        }
        
        @RootFactory
        typealias MyFactory = (MyModule) -> TestComponent1<Dep<Foo, Foo, Foo>>
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
            @Given
            fun foo() = Foo()
            
            @Module
            val barModule = BarModule()
            
            @Module
            class BarModule {
                @Given
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
            @Module
            object MyModule {
                @GivenSetElements fun setA() = setOf("a")
                @GivenSetElements fun setB() = setOf(0)
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
                @Given fun foo1(): Foo1 = Foo()
                @Given fun foo2(): Foo2 = Foo()
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
                    typealias MyFactory = (Foo1Module, Foo2Module) -> TestComponent2<Foo1, Foo2>
        
                    fun invoke(): Pair<Foo1, Foo2> {
                        val component = rootFactory<MyFactory>()(Foo1Module, Foo2Module)
                        return component.a to component.b
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
            @Module
            object FooModule {
                @Given fun foo(): Foo = Foo()
            }
            
            @RootFactory
            typealias MyFactory = (FooModule) -> TestComponent1<Foo?>
    
            fun invoke(): Foo? {
                return rootFactory<MyFactory>()(FooModule).a
            }
        """
    ) {
        assertNotNull(invokeSingleFile())
    }

    @Test
    fun testReturnsNullOnMissingNullableGiven() = codegen(
        """
            @RootFactory
            typealias MyFactory = () -> TestComponent1<Foo?>
            fun invoke(): Foo? { 
                return rootFactory<MyFactory>()().a
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
            class Dep
            
            @RootFactory
            typealias MyFactory = (Dep) -> TestComponent1<Dep>
            
            fun invoke(): Pair<Any, Any> {
                val dep = Dep()
                return dep to rootFactory<MyFactory>()(dep).a
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testDuplicatedInputsFails() = codegen(
        """
            @RootFactory
            typealias MyFactory = (Foo, Foo) -> TestComponent1<Foo>
        """
    ) {
        assertInternalError("multiple givens")
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
        assertInternalError("multiple givens")
    }

    @Test
    fun testGivenPerComponent() = codegen(
        """
            @Module
            object ParentModule {
                @Given(TestParentComponent2::class) fun parentFoo() = Foo()
            }
            
            @RootFactory
            typealias MyParentFactory = (ParentModule) -> TestParentComponent2<Foo, MyChildFactory>
            
            @Module
            object ChildModule {
                @Given(TestChildComponent1::class) fun childFoo() = Foo()
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
        val (component1, component2) = invokeSingleFile<Pair<SelfComponent, SelfComponent>>()
        assertSame(component1, component2)
    }

    @Test
    fun testPrefersExactType() = codegen(
        """
            class Dep<T>(val value: T)
            
            @Module
            object FooModule {
                @Given
                fun <T> genericDep(t: T): Dep<T> = Dep(t)
                
                @Given
                fun fooDep(foo: Foo): Dep<Foo> = Dep(foo)
                
                @Given
                fun foo() = Foo()
            }
            
            @RootFactory
            typealias MyFactory = (FooModule) -> TestComponent1<Dep<Foo>>
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
                @Given
                fun intComparator(): AliasComparator<Int> = error("")
            }
            
            @RootFactory
            typealias MyFactory = (MyModule) -> TestComponent1<compare<Int>>

            @Given
            fun <T> compare(@Assisted a: T, @Assisted b: T, comparator: AliasComparator<T>): Int = comparator
                .compare(a, b)

        """
    )

    @Test
    fun testCircularDependencyFails() = codegen(
        """
            @Given class A(b: B)
            @Given class B(a: A)
            
            @RootFactory
            typealias MyFactory = () -> TestComponent1<B>
        """
    ) {
        assertInternalError("circular")
    }

    @Test
    fun testProviderCanBreaksCircularDependency() = codegen(
        """
            @Given class A(b: B)
            @Given(TestComponent1::class) class B(a: () -> A)
            
            @RootFactory
            typealias MyFactory = () -> TestComponent1<B>
        """
    ) {
        assertOk()
    }

    @Test
    fun testIrrelevantProviderInChainDoesNotBreakCircularDependecy() = codegen(
        """
            @Given class A(b: () -> B)
            @Given class B(b: C)
            @Given class C(b: B)
            
            @RootFactory
            typealias MyFactory = () -> TestComponent1<C>
        """
    ) {
        assertInternalError("circular")
    }

}
