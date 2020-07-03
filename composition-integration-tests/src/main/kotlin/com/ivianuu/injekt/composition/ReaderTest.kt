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

package com.ivianuu.injekt.composition

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertTrue
import org.junit.Test

class ReaderTest {

    @Test
    fun testReaderInvocationInReaderAllowed() =
        codegen(
            """
            @Reader fun a() {}
            @Reader fun b() { a() }
        """
        ) {
            assertOk()
        }

    @Test
    fun testReaderInvocationInNonReaderNotAllowed() =
        codegen(
            """
            @Reader fun a() {}
            fun b() { a() }
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testReaderInvocationInNonReaderLambdaIsNotAllowed() =
        codegen(
            """
            val lambda: () -> Unit = {
                func()
            }
            @Reader fun func() {}
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testNestedReaderInvocationInReaderAllowed() =
        codegen(
            """
            @Reader fun a() {}
            fun b(block: () -> Unit) = block()
            @Reader
            fun c() {
                b {
                    a()
                }
            }
        """
        ) {
            assertOk()
        }

    @Test
    fun testOpenReaderFails() = codegen(
        """
        open class MyClass {
            @Reader 
            open fun func() {
            }
        }
        """
    ) {
        assertCompileError("final")
    }

    @Test
    fun testSimpleReader() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Reader
        fun func(foo: Foo = get()): Foo {
            return foo
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.reader { func() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testSimpleReaderLambda() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Reader
        fun func(foo: Foo = get()): Foo {
            return foo
        }
        
        @Reader
        fun other() {
        }
        
        @Reader
        fun <R> withFoo(block: @Reader (Foo) -> R): R = block(func())
        
        fun invoke(): Foo {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.reader {
                withFoo {
                    other()
                    it
                }
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    // todo @Test
    fun testSimpleReaderLambdaProperty() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Reader
        val foo: @Reader () -> Foo = { get() }

        fun invoke(): Foo {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.reader { foo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testNestedReader() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Reader
        fun createFoo(foo: Foo = get()): Foo {
            return foo
        }
        
        fun <R> nonReader(block: () -> R) = block()
        
        @Reader
        fun <R> reader(block: @Reader () -> R) = block()
        
        fun invoke(): Foo {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.reader {
                nonReader { 
                    reader { 
                        nonReader { 
                            reader {
                                createFoo()
                            }
                        }
                    }
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
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Reader
        suspend fun func(foo: Foo = get()): Foo {
            delay(1000)
            return foo
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.reader {
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
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Reader
        suspend fun func(foo: Foo = get()): Foo {
            delay(1000)
            return foo
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return runBlocking {
                component.reader {
                    func()
                }
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testSuspendReaderLambda() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Reader
        suspend fun func(foo: Foo = get()): Foo {
            delay(1000)
            return foo
        }
        
        @Reader
        suspend fun other() { 
            delay(1000)
        }
        
        @Reader
        suspend fun <R> withFoo(block: @Reader suspend (Foo) -> R): R = block(func())
        
        fun invoke(): Foo {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return runBlocking {
                component.reader {
                    withFoo {
                        other()
                        it
                    }
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
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Reader
        suspend fun createFoo(foo: Foo = get()): Foo {
            delay(1000)
            return foo
        }
        
        fun <R> nonReader(block: () -> R) = block()
        
        @Reader
        fun <R> Reader(block: @Reader () -> R) = block()
        
        fun invoke() {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            component.reader {
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
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Reader
        fun func(foo: Foo = get()): Foo {
            return foo
        }
        
        @Reader
        fun withDefault(foo: Foo = func()): Foo = foo
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.reader { withDefault() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderCallInDefaultParameterWithCapture() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Reader
        fun withDefault(foo: Foo = get(), foo2: Foo = foo): Foo = foo
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.reader { withDefault() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    // todo @Test
    fun testAbstractReaderFunction() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        interface Super {
            @Reader
            fun func(): Foo
        }
        
        class Impl : Super {
            @Reader
            override fun func(): Foo {
                return get()
            }
        }

        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.reader { Impl().func() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    // todo @Test
    fun testOpenReaderFunction() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        open class Super {
            @Reader
            open fun func(): Foo = get()
        }
        
        class Impl : Super() {
            @Reader
            override fun func(): Foo {
                super.func()
                return get()
            }
        }

        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.reader { Impl().func() }
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
                @Reader
                fun foo(): Foo {
                    return get()
                } 
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
                    @CompositionFactory 
                    fun factory(): TestCompositionComponent {
                        transient { Foo() }
                        return create() 
                    }
        
                    @Reader
                    fun <R> withBar(block: @Reader (Bar) -> R): R = block(bar()) 
                """
            )
        ),
        listOf(
            source(
                """
                lateinit var component: TestCompositionComponent
                
                fun getFoo() = component.reader {
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
                    initializeCompositions()
                    component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
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
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Reader
        val foo: Foo get() = get()
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.reader { foo }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    // todo @Test
    fun testMultiCompileReaderProperty() = multiCodegen(
        listOf(
            source(
                """
                @CompositionFactory 
                fun factory(): TestCompositionComponent {
                    transient { Foo() }
                    return create() 
                }
        
                @Reader
                val foo: Foo get() = get()
            """
            )
        ),
        listOf(
            source(
                """
                fun invoke(): Foo { 
                    initializeCompositions()
                    val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
                    return component.reader { foo }
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
        @Reader
        fun foo(): Foo = get()
        
        @Reader
        @Transient
        class ReaderClass {
            init {
                get<Foo>()
                //foo()
            }
        }
    """
    )

}
