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
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class ImplicitTest {

    @Test
    fun testSimpleReader() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        fun func(): Foo = given<Foo>()
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { func() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedReader() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        fun createFoo() = given<Foo>()
        
        fun <R> nonReader(block: () -> R) = block()
        
        fun invoke(): Foo {
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader {
                nonReader { 
                    createFoo()
                }
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testSuspendBlockInReadingBlock() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        suspend fun func(): Foo {
            delay(1000)
            return given()
        }
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader {
                runBlocking { 
                    func()
                }
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReadingBlockInSuspendBlock() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        suspend fun func(): Foo {
            delay(1000)
            return given()
        }
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return runBlocking {
                component.runReader {
                    func()
                }
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testSuspendNestedReader() = codegen(
        """
        @Given @Reader
        fun foo() = Foo()
        
        @Reader
        suspend fun createFoo(foo: Foo): Foo {
            delay(1000)
            return given()
        }
        
        fun <R> nonReader(block: () -> R) = block()
        
        @Reader
        fun <R> Reader(block: @Reader () -> R) = block()
        
        fun invoke() {
            initializeComponents()
            val component = rootComponent<TestComponent>()
            component.runReader {
                nonReader { 
                    Reader { 
                        nonReader { 
                            Reader {
                                GlobalScope.launch {
                                    createFoo()
                                }
                            }
                        }
                    }
                }
            }
        }
    """
    ) {
        //assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderCallInDefaultParameter() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        fun func() = given<Foo>()
        
        @Reader
        fun withDefault(foo: Foo = func()): Foo = foo
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { withDefault() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderCallInDefaultParameterWithCapture() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        fun withDefault(foo: Foo = given(), foo2: Foo = foo): Foo = foo
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { withDefault() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun multiCompileReader() = multiCodegen(
        listOf(
            source(
                """
                @Given
                fun foo() = Foo()
            """
            ),
        ),
        listOf(
            source(
                """
                @Reader
                fun bar(): Bar {
                    return Bar(foo())
                }
            """
            )
        ),
        listOf(
            source(
                """
                    @Reader
                    fun <R> withBar(block: (Bar) -> R): R = block(bar()) 
                """
            )
        ),
        listOf(
            source(
                """
                lateinit var component: TestComponent
                
                fun getFoo() = component.runReader {
                    withBar {
                        foo()
                    }
                }
            """
            )
        ),
        listOf(
            source(
                """
                fun invoke(): Foo {
                    initializeComponents()
                    component = rootComponent<TestComponent>()
                    return getFoo()
                }
                """,
                name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderProperty() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        val foo: Foo get() = given()
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { foo }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testMultiCompileReaderProperty() = multiCodegen(
        listOf(
            source(
                """
                @Given 
                fun foo() = Foo()
        
                @Reader
                val foo: Foo get() = given()
            """
            )
        ),
        listOf(
            source(
                """
                fun invoke(): Foo { 
                    initializeComponents()
                    val component = rootComponent<TestComponent>()
                    return component.runReader { foo }
                }
                """,
                name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderClass() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        class FooFactory {
            fun getFoo() = given<Foo>()
        }
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { FooFactory().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderClassMulti() = multiCodegen(
        listOf(
            source(
                """
                @Given
                fun foo() = Foo()
        
                @Reader
                class FooFactory {
                    fun getFoo() = given<Foo>()
                } 
            """
            )
        ),
        listOf(
            source(
                """ 
                fun invoke(): Foo { 
                    initializeComponents()
                    val component = rootComponent<TestComponent>()
                    return component.runReader { FooFactory().getFoo() }
                }
            """, name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderClassWithAnnotatedConstructor() = codegen(
        """
        @Given fun foo() = Foo()
        
        class FooFactory @Reader constructor() {
            fun getFoo() = given<Foo>()
        }
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { FooFactory().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testInjectReaderClass() = codegen(
        """
        @Given fun foo() = Foo()
 
        @Given
        class FooFactory {
            fun getFoo() = given<Foo>()
        }
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { given<FooFactory>().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderOpenSubclass() = codegen(
        """
        @Given fun foo() = Foo()

        @Reader
        open class SuperClass {
            fun getFoo() = given<Foo>()
        }
        
        @Reader
        class FooFactory : SuperClass()
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { FooFactory().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderAbstractSubclass() = codegen(
        """
        @Given fun foo() = Foo()
        
        @Reader
        abstract class SuperClass {
            fun getFoo() = given<Foo>()
        }
        
        @Reader
        class FooFactory : SuperClass()
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { FooFactory().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testGenericSuperClass() = codegen(
        """
        @Given fun foo() = Foo()
        
        @Reader
        open class SuperClass<T>(val value: T) {
            fun getFoo() = given<Foo>()
        }
        
        @Reader
        class FooFactory : SuperClass<String>("hello")
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { FooFactory().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderClassAccessesReaderFunctionInInit() = codegen(
        """
        @Given fun foo() = Foo()
        
        @Given
        class FooFactory {
            val foo: Foo = given()
        }
        
        fun invoke(): Foo {
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { given<FooFactory>().foo }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderWithSameName() = codegen(
        """
        @Reader
        fun func(foo: Foo) {
        }
        
        @Reader
        fun func(foo: Foo, bar: Bar) {
        }
    """
    )

    @Test
    fun testGenericReader() = codegen(
        """
        @Given fun foo() = Foo()
        
        @Reader
        fun <T> provide() = given<T>()
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { provide() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testGenericReaderMulti() = multiCodegen(
        listOf(
            source(
                """
                @Given fun foo() = Foo()

                @Reader 
                fun <T> provide() = given<T>()
                """
            )
        ),
        listOf(
            source(
                """
                fun invoke(): Foo { 
                    initializeComponents()
                    val component = rootComponent<TestComponent>()
                    return component.runReader { provide() }
                }
            """, name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedRunReader() = codegen(
        """
        @Given(TestParentComponent::class)
        fun foo() = Foo()
        
        @Given(TestChildComponent::class)
        fun bar() = Bar(given())
        
        fun invoke(): Bar { 
            initializeComponents()
            val parentComponent = rootComponent<TestParentComponent>()
            val childComponent = parentComponent.runReader {
                childComponent<TestChildComponent>()
            }
            return parentComponent.runReader {
                childComponent.runReader {
                    given<Bar>()
                }
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testGivenInDefaultParameter() = codegen(
        """
        @Given fun foo() = Foo()
        
        @Reader
        fun createFoo(foo: Foo = given()): Foo = foo
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { createFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testGivenWithDistinctType() = codegen(
        """
        @Distinct 
        typealias Foo2 = Foo
        
        @Given fun foo(): Foo2 = Foo()
        
        @Reader
        fun createFoo(foo: Foo2 = given()): Foo2 = foo
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { createFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testGivenClassInDefaultParameter() = codegen(
        """
        @Given fun foo() = Foo()
        
        @Reader
        class CreateFoo(val foo: Foo = given())
        
        fun invoke(): Foo { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { CreateFoo().foo }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testGenericReaderDependencyOfSameType() = codegen(
        """
        @Given
        class MyClass {
            val set1: Set<String> = given()
            val set2: Set<Int> = given()
        }
        
        @SetElements(TestComponent::class)
        fun set1() = emptySet<String>()
        
        @SetElements(TestComponent::class)
        fun set2() = emptySet<Int>()
        
        fun invoke() { 
            initializeComponents()
            val component = rootComponent<TestComponent>()
            component.runReader { given<MyClass>() }
        }
    """
    ) {
        invokeSingleFile()
    }

    // todo @Test
    fun testReaderCycle() = codegen(
        """
        @Reader
        fun a() {
            given<Int>()
            b()
        }
        
        @Reader
        fun b() {
            given<String>()
            a()
        }
    """
    )

    // todo @Test
    fun testIntermediateReaderCycle() = codegen(
        """
        @Reader
        fun a() {
            given<Int>()
            b()
        }
        
        @Reader
        fun b() {
            given<String>()
            c()
        }
        
        @Reader
        fun c() {
            given<Double>()
            a()
        }
    """
    )

    @Test
    fun testDoesNotRequireExplicitOverriddenParameters() = codegen(
        """
        @Reader 
        fun createFoo(foo: Foo = given()) = foo
        
        fun invoke() {
            initializeComponents()
            val component = rootComponent<TestComponent>()
            component.runReader { createFoo(Foo()) }
        }
    """
    )

    @Test
    fun testGivenCallInComplexDefaultExpressionCreatesAnAdditionalValueParameter() = codegen(
        """
        @Reader 
        fun createFoo(foo: Foo = "lol".run { given() }) = foo
        
        fun invoke() {
            initializeComponents()
            val component = rootComponent<TestComponent>()
            component.runReader { createFoo(Foo()) }
        }
    """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testLazyGiven() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        val component by lazy {
            initializeComponents()
            rootComponent<TestComponent>()
        }
        
        fun invoke() = component.runReader {
            given<Foo>(lazy = true) to given<Foo>(lazy = true)
        }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(a, b)
    }

    @Test
    fun testAssistedGiven() = codegen(
        """
        @Given
        fun bar(foo: Foo) = Bar(foo)
        
        fun invoke(): Bar {
            initializeComponents()
            val component = rootComponent<TestComponent>()
            return component.runReader { given<Bar>(Foo()) }
        }
        """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

}
