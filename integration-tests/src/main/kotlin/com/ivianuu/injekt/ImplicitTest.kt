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
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
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
            initializeInjekt()
            return runReader { func() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testSimpleReaderLambda() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        fun func(foo: Foo = given()): Foo {
            return foo
        }
        
        @Reader
        fun other() {
        }
        
        @Reader
        fun withFoo(block: @Reader (Foo) -> Unit) = block(func())
        
        fun invoke(): Foo {
            initializeInjekt()
            return runReader {
                withFoo {
                    other()
                    it
                }
                Foo()
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testSimpleReaderLambdaMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Given
                    fun foo() = Foo()
                    
                    @Reader
                    fun func(foo: Foo = given()): Foo {
                        return foo
                    }
                    
                    @Reader
                    fun other() {
                    }
                    
                    @Reader
                    fun withFoo(block: @Reader (Foo) -> Unit) = block(func())
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke(): Foo {
                        initializeInjekt()
                        return runReader {
                            withFoo {
                                other()
                                it
                            }
                            Foo()
                        }
                    } 
                """,
                name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

    @Test
    fun testGenericReaderLambda() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        fun func(foo: Foo = given()): Foo {
            return foo
        }
        
        @Reader
        fun other() {
        }
        
        @Reader
        fun <R> withFoo(block: @Reader (Foo) -> R) = block(func())
        
        fun invoke(): Foo {
            initializeInjekt()
            return runReader {
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
    fun testGenericReaderLambdaMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Given
                    fun foo() = Foo()
                    
                    @Reader
                    fun func(foo: Foo = given()): Foo {
                        return foo
                    }
                    
                    @Reader
                    fun other() {
                    }
                    
                    @Reader
                    fun <R> withFoo(block: @Reader (Foo) -> R) = block(func())
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke(): Foo {
                        initializeInjekt()
                        return runReader {
                            withFoo {
                                other()
                                it
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

    @Test
    fun testNestedReader() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        fun createFoo() = given<Foo>()
        
        fun <R> nonReader(block: () -> R) = block()
        
        fun invoke(): Foo {
            initializeInjekt()
            return runReader {
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
            initializeInjekt()
            return runReader {
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
            initializeInjekt()
            return runBlocking {
                runReader {
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
            initializeInjekt()
            runReader {
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
    fun testSuspendReaderLambda() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        suspend fun func(foo: Foo = given()): Foo {
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
            initializeInjekt()
            return runBlocking {
                runReader {
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
    fun testReaderCallInDefaultParameter() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader
        fun func() = given<Foo>()
        
        @Reader
        fun withDefault(foo: Foo = func()): Foo = foo
        
        fun invoke(): Foo { 
            initializeInjekt()
            return runReader { withDefault() }
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
            initializeInjekt()
            return runReader { withDefault() }
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
                fun getFoo() = runReader {
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
                    initializeInjekt()
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
            initializeInjekt()
            return runReader { foo }
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
                    initializeInjekt()
                    return runReader { foo }
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
            initializeInjekt()
            return runReader { FooFactory().getFoo() }
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
                    initializeInjekt()
                    return runReader { FooFactory().getFoo() }
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
            initializeInjekt()
            return runReader { FooFactory().getFoo() }
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
            initializeInjekt()
            return runReader { given<FooFactory>().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    // todo @Test
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
            initializeInjekt()
            return runReader { FooFactory().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    // todo @Test
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
            initializeInjekt()
            return runReader { FooFactory().getFoo() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    // todo @Test
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
            initializeInjekt()
            return runReader { FooFactory().getFoo() }
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
            initializeInjekt()
            return runReader { given<FooFactory>().foo }
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
            initializeInjekt()
            return runReader { provide() }
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
                    initializeInjekt()
                    return runReader { provide() }
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
        @Given
        fun foo() = Foo()
        
        @Given
        fun bar() = Bar(given())
        
        fun invoke(): Bar { 
            initializeInjekt()
            return runReader {
                runReader {
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
            initializeInjekt()
            return runReader { createFoo() }
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
            initializeInjekt()
            return runReader { createFoo() }
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
        
        @SetElements
        fun set1() = emptySet<String>()
        
        @SetElements
        fun set2() = emptySet<Int>()
        
        fun invoke() { 
            initializeInjekt()
            runReader { given<MyClass>() }
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
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

    @Test
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
    fun testGivenCallInComplexDefaultExpressionCreatesAnAdditionalValueParameter() = codegen(
        """
        @Reader 
        fun createFoo(foo: Foo = "lol".run { given() }) = foo
        
        fun invoke() {
            initializeInjekt()
            runReader { createFoo(Foo()) }
        }
    """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testAssistedGiven() = codegen(
        """
        @Given
        fun bar(foo: Foo) = Bar(foo)
        
        fun invoke(): Bar {
            initializeInjekt()
            return runReader { given<Bar>(Foo()) }
        }
        """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testAbstractReaderFunction() = codegen(
        """
        @Given
        fun foo() = Foo()

        @Given
        fun bar() = Bar(given())
        
        interface Action {
            @Reader
            fun execute()
        }
        
        open class FooAction : Action {
            @Reader
            override fun execute() {
                given<Foo>()
            }
        }
        
        class BarAction : FooAction() {
            @Reader
            override fun execute() {
                super.execute()
                given<Bar>()
            }
        }
        
        fun invoke() {
            initializeInjekt()
            runReader {
                val actions = listOf(
                    FooAction(),
                    BarAction()
                )
                actions.forEach { it.execute() }
            }
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testAbstractReaderFunctionMulti() = multiCodegen(
        listOf(
            source(
                """ 
                    @Given
                    fun foo() = Foo()
            
                    @Given
                    fun bar() = Bar(given())
                    
                    interface Action {
                        @Reader
                        fun execute()
                    }
                """
            )
        ),
        listOf(
            source(
                """ 
                    open class FooAction : Action {
                        @Reader
                        override fun execute() {
                            given<Foo>()
                        }
                    }
                """
            )
        ),
        listOf(
            source(
                """ 
                    class BarAction : FooAction() {
                        @Reader
                        override fun execute() {
                            super.execute()
                            given<Bar>()
                        }
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() {
                        initializeInjekt()
                        runReader {
                            val actions = listOf(
                                FooAction(),
                                BarAction()
                            )
                            actions.forEach { it.execute() }
                        }
                    }
                    """,
                name = "File.kt"
            )
        )
    ) {
        it.last().invokeSingleFile()
    }

    @Test
    fun testAnonymousAbstractReaderFunction() = codegen(
        """
        @Given
        fun foo() = Foo()

        @Given
        fun bar() = Bar(given())
        
        interface Action {
            @Reader
            fun execute()
        }
        
        fun invoke() {
            initializeInjekt()
            runReader {
                val actions = listOf(
                    object : Action { 
                        @Reader
                        override fun execute() {
                            given<Foo>()
                        }
                    },
                    object : Action {
                        @Reader
                        override fun execute() {
                            given<Bar>()
                        }
                    }
                )
                actions.forEach { it.execute() }
            }
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testLambdaTracking() = codegen(
        """
        val property: @Reader () -> Unit = {  }
        
        @Reader
        fun invoke(block: @Reader () -> Unit) {
            initializeInjekt()
            block()
            val block2: @Reader () -> Unit = {  }
            block2()
            property()
            runReader { 
                block()
                block2()
                property()
            }
        }
    """
    ) {
        //invokeSingleFile()
    }

    @Test
    fun testReaderTracking() = codegen(
        """
        val lambdaProperty: @Reader () -> Unit = {}
        
        @Reader
        fun createLambda(delegate: @Reader () -> Unit): @Reader () -> Unit {
            initializeInjekt()
            val block: @Reader () -> Unit = {
                
            }
            
            delegate()
            block()
            runReader {
                block()
                delegate()
            }
            
            return block
        }
        
        @Reader
        fun invoke() = createLambda {}
    """
    ) {
        assertOk()
    }

}
