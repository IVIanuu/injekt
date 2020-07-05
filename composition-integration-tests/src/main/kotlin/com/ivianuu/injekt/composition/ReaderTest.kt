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
    fun testSimpleReader() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        fun func(foo: Foo = get()): Foo {
            return foo
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReader { func() }
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
            unscoped { Foo() }
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
            return component.runReader {
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
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        val foo: @Reader () -> Foo = { get() }

        fun invoke(): Foo {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReader { foo() }
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
            unscoped { Foo() }
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
            return component.runReader {
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
            unscoped { Foo() }
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
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
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
    fun testSuspendReaderLambda() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
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
                component.runReader {
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
            unscoped { Foo() }
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
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
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
            return component.runReader { withDefault() }
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
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        fun withDefault(foo: Foo = get(), foo2: Foo = foo): Foo = foo
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
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
                        unscoped { Foo() }
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
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        val foo: Foo get() = get()
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
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
                @CompositionFactory 
                fun factory(): TestCompositionComponent {
                    unscoped { Foo() }
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
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        class FooFactory {
            fun getFoo() = get<Foo>()
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
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
                @CompositionFactory 
                fun factory(): TestCompositionComponent {
                    unscoped { Foo() }
                return create() 
            }
        
            @Reader
            class FooFactory {
                fun getFoo() = get<Foo>()
            } 
            """
            )
        ),
        listOf(
            source(
                """ 
                fun invoke(): Foo { 
                    initializeCompositions()
                    val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
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
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
            return create() 
        }
        
        class FooFactory @Reader constructor() {
            fun getFoo() = get<Foo>()
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReader { FooFactory().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testInjectReaderClass() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        @Unscoped
        class FooFactory {
            fun getFoo() = get<Foo>()
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReader { get<FooFactory>().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderOpenSubclass() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        open class SuperClass {
            fun getFoo() = get<Foo>()
        }
        
        @Reader
        class FooFactory : SuperClass()
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReader { FooFactory().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderAbstractSubclass() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        abstract class SuperClass {
            fun getFoo() = get<Foo>()
        }
        
        @Reader
        class FooFactory : SuperClass()
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReader { FooFactory().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testGenericSuperClass() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        open class SuperClass<T>(val value: T) {
            fun getFoo() = get<Foo>()
        }
        
        @Reader
        class FooFactory : SuperClass<String>("hello")
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReader { FooFactory().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderClassWithAssistedParameters() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        @Unscoped
        class FooFactory(val assisted: @Assisted String) {
            fun getFoo() = get<Foo>()
        }
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReader { get<@Provider (String) -> FooFactory>()("hello").getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderClassAccessesReaderFunctionInInit() = codegen(
        """
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        @Unscoped
        class FooFactory {
            val foo: Foo = get()
        }
        
        fun invoke(): Foo {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReader { get<FooFactory>().foo }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testReaderClassWithAssistedParametersMulti() = multiCodegen(
        listOf(
            source(
                """
                @CompositionFactory 
                fun factory(): TestCompositionComponent {
                    unscoped { Foo() }
                    return create()
                }
        
                @Reader
                @Unscoped
                class FooFactory(val assisted: @Assisted String) {
                    fun getFoo() = get<Foo>()
                }
            """
            )
        ),
        listOf(
            source(
                """
                    fun invoke(): Foo { 
                        initializeCompositions()
                        val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
                        return component.runReader { get<@Provider (String) -> FooFactory>()("hello").getFoo() }
                    }
            """, name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
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
        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
            return create() 
        }
        
        @Reader
        fun <T> provide() = get<T>()
        
        fun invoke(): Foo { 
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
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
                @CompositionFactory 
                fun factory(): TestCompositionComponent {
                    unscoped { Foo() }
                    return create() 
                }

                @Reader 
                fun <T> provide() = get<T>()
                """
            )
        ),
        listOf(
            source(
                """
                fun invoke(): Foo { 
                    initializeCompositions()
                    val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
                    return component.runReader { provide() }
                }
            """, name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

}
