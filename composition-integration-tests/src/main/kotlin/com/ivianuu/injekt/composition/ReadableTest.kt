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

class ReadableTest {

    @Test
    fun testReadableInvocationInReadableAllowed() =
        codegen(
            """
            @Readable fun a() {}
            @Readable fun b() { a() }
        """
        ) {
            assertOk()
        }

    @Test
    fun testReadableInvocationInNonReadableNotAllowed() =
        codegen(
            """
            @Readable fun a() {}
            fun b() { a() }
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testReadableInvocationInNonReadableLambdaIsNotAllowed() =
        codegen(
            """
            val lambda: () -> Unit = {
                func()
            }
            @Readable fun func() {}
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testNestedReadableInvocationInReadableAllowed() =
        codegen(
            """
            @Readable fun a() {}
            fun b(block: () -> Unit) = block()
            @Readable
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
    fun testOpenReadableFails() = codegen(
        """
        open class MyClass {
            @Readable 
            open fun func() {
            }
        }
        """
    ) {
        assertCompileError("final")
    }

    @Test
    fun testSimpleReadable() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Readable
        fun func(foo: Foo = get()): Foo {
            return foo
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReading { func() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testSimpleReadableLambda() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Readable
        fun func(foo: Foo = get()): Foo {
            return foo
        }
        
        @Readable
        fun other() {
        }
        
        @Readable
        fun <R> withFoo(block: @Readable (Foo) -> R): R = block(func())
        
        fun invoke(): Foo {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReading {
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

    @Test
    fun testNestedReadable() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Readable
        fun createFoo(foo: Foo = get()): Foo {
            return foo
        }
        
        fun <R> nonReadable(block: () -> R) = block()
        
        @Readable
        fun <R> readable(block: @Readable () -> R) = block()
        
        fun invoke(): Foo {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReading {
                nonReadable { 
                    readable { 
                        nonReadable { 
                            readable {
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
        
        @Readable
        suspend fun func(foo: Foo = get()): Foo {
            delay(1000)
            return foo
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReading {
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
        
        @Readable
        suspend fun func(foo: Foo = get()): Foo {
            delay(1000)
            return foo
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return runBlocking {
                component.runReading {
                    func()
                }
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testSuspendingReadableLambda() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Readable
        suspend fun func(foo: Foo = get()): Foo {
            delay(1000)
            return foo
        }
        
        @Readable
        suspend fun other() { 
            delay(1000)
        }
        
        @Readable
        suspend fun <R> withFoo(block: @Readable suspend (Foo) -> R): R = block(func())
        
        fun invoke(): Foo {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return runBlocking {
                component.runReading {
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
    fun testSuspendNestedReadable() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Readable
        suspend fun createFoo(foo: Foo = get()): Foo {
            delay(1000)
            return foo
        }
        
        fun <R> nonReadable(block: () -> R) = block()
        
        @Readable
        fun <R> readable(block: @Readable () -> R) = block()
        
        fun invoke() {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            component.runReading {
                nonReadable { 
                    readable { 
                        nonReadable { 
                            readable {
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
    fun testReadableCallInDefaultParameter() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Readable
        fun func(foo: Foo = get()): Foo {
            return foo
        }
        
        @Readable
        fun withDefault(foo: Foo = func()): Foo = foo
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReading { withDefault() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    // todo @Test
    fun testAbstractReadableFunction() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        interface Super {
            @Readable
            fun func(): Foo
        }
        
        class Impl : Super {
            @Readable
            override fun func(): Foo {
                return get()
            }
        }

        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReading { Impl().func() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    // todo @Test
    fun testOpenReadableFunction() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        open class Super {
            @Readable
            open fun func(): Foo = get()
        }
        
        class Impl : Super() {
            @Readable
            override fun func(): Foo {
                super.func()
                return get()
            }
        }

        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReading { Impl().func() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun multiCompileReadable() = multiCodegen(
        listOf(
            source(
                """
                @Readable
                fun foo(): Foo {
                    return get()
                } 
            """
            ),
        ),
        listOf(
            source(
                """
                
                @Readable
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
        
                @Readable
                fun <R> withBar(block: @Readable (Bar) -> R): R = block(bar())
        
                fun invoke(): Foo {
                    initializeCompositions()
                    val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
                    return component.runReading {
                        withBar {
                            foo()
                        }
                    }
                }
                """,
                name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

}
