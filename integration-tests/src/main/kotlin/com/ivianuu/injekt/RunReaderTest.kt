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
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class RunReaderTest {

    @Test
    fun testSimple() = codegen(
        """
        @Given
        fun foo() = Foo()
        @Given
        fun bar() = Bar(given())
        
        fun invoke(): Bar {
            return runReader { given<Bar>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testWithChild() = codegen(
        """
        @Given
        fun foo() = Foo()
        @Given
        fun bar() = Bar(given())
        
        fun invoke(foo: Foo): Foo {
            return runReader(42, "hello world") { overriddingFoo(foo) }
        }
        
        fun otherInvoke() = runReader { overriddingFoo(Foo()) }
        
        @Reader
        private fun overriddingFoo(foo: Foo) = runChildReader(foo) {
            given<Bar>().foo
        }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testWithGenericChild() = codegen(
        """
        @Given
        fun foo() = Foo()
        @Given
        fun bar() = Bar(given())
        
        fun invoke(foo: Foo): Foo {
            return runReader(42, "hello world") { overriding<Bar>(foo) }
        }
        
        fun otherInvoke() = runReader { overriding<Bar>(Foo()) }
        
        @Reader
        private fun <T> overriding(value: Foo) = runChildReader(value) {
            given<Bar>().foo
        }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testComplexGenericChild() = codegen(
        """
        @Given
        fun foo() = Foo()
        @Given
        fun bar() = Bar(given())
        
        fun invoke(foo: Foo): Foo {
            return runReader(42, true) { genericA<Bar>(foo) }
        }
        
        @Reader
        fun <T> genericA(foo: Foo) = runChildReader(foo, "") {
            nonGeneric(foo)
        }
        
        @Reader
        private fun nonGeneric(foo: Foo) = genericB<String>(foo)

        @Reader
        private fun <S> genericB(foo: Foo) = runChildReader(foo, 0L) {
            given<Bar>().foo
        }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testCircular() = codegen(
        """
            @Reader
            fun a(foo: Foo) = childRunner(Foo()) { b(foo) }
            
            @Reader
            fun b(foo: Foo) = childRunner(foo) { given<Foo>() }
            
            @Reader
            fun <R> childRunner(foo: Foo, block: @Reader () -> R) = runChildReader(foo) {
                block()
            }
            
            fun invoke(foo: Foo) = runReader { a(foo) }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testCircular2() = codegen(
        """
            @Reader
            fun a(foo: Foo) = childRunner(Foo()) { b(foo) }
            
            @Reader
            fun b(foo: Foo) = c(foo)
            
            @Reader
            fun c(foo: Foo) = childRunner(foo) { given<Foo>() }
            
            @Reader
            fun <R> childRunner(foo: Foo, block: @Reader () -> R) = runChildReader(foo) {
                block()
            }
            
            fun invoke(foo: Foo) = runReader { a(foo) }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testDeeplyNested() = codegen(
        """
            fun invoke(
                string: String,
                int: Int,
                long: Long,
                boolean: Boolean
            ) = runReader {
                withString(string) {
                    withInt(int) {
                        withLong(long) {
                            withBoolean(boolean) {
                                listOf(given<String>(), given<Int>(), given<Long>(), given<Boolean>())
                            }
                        }
                    }
                }
            }
            
            @Reader
            private fun <R> withString(
                value: String,
                block: @Reader () -> R
            ) = runChildReader(value) { block() }
            
            @Reader
            private fun <R> withInt(
                value: Int,
                block: @Reader () -> R
            ) = runChildReader(value) { block() }
            
            @Reader
            private fun <R> withLong(
                value: Long,
                block: @Reader () -> R
            ) = runChildReader(value) { block() }
            
            @Reader
            private fun <R> withBoolean(
                value: Boolean,
                block: @Reader () -> R
            ) = runChildReader(value) { block() }
        """
    ) {
        val string = "hello world"
        val int = 1
        val long = 4L
        val boolean = true
        val result = invokeSingleFile(string, int, long, boolean) as List<Any>
        assertEquals(string, result[0])
        assertEquals(int, result[1])
        assertEquals(long, result[2])
        assertEquals(boolean, result[3])
    }

    @Test
    fun testAppActReader() = multiCodegen(
        listOf(
            source(
                """
                    class App
                    class Activity {
                        val app = App()
                    }
                """,
                initializeInjekt = false
            )
        ),
        listOf(
            source(
                """
                    inline fun <R> App.runApplicationReader(block: @Reader () -> R) = runReader(this) { 
                        block()
                    }
        
                    inline fun <R> Activity.runActivityReader(block: @Reader () -> R) = app.runApplicationReader {
                        runChildReader(this) {
                            block()
                        }
                    }
                """,
                initializeInjekt = false
            )
        ),
        listOf(
            source(
                """
                    interface Store<S, A> {
                    }
                    
                    interface StoreScope<S, A> {
                    }
                    
                    class CoroutineScope
                    
                    fun <S, A> CoroutineScope.store(
                        initial: S,
                        block: suspend StoreScope<S, A>.() -> Unit
                    ): Store<S, A> = StoreImpl()
                    
                    @Reader
                    inline fun <S, A> store(
                        initial: S,
                        noinline block: suspend StoreScope<S, A>.() -> Unit
                    ): Store<S, A> = given<CoroutineScope>().store(initial) {
                        block()
                    }
                    
                    internal class StoreImpl<S, A>(
                    ) : Store<S, A>, StoreScope<S, A> {
                    }
                            """,
                initializeInjekt = false
            )
        ),
        listOf(
            source(
                """
                    // todo remove overload once compiler is fixed
                    @Reader
                    fun <S, A> rememberStore(
                        init: @Reader () -> Store<S, A>
                    ): Store<S, A> = rememberStore(inputs = *emptyArray(), init = init)

                    @Reader
                    fun <S, A> rememberStore(
                        vararg inputs: Any?,
                        init: @Reader () -> Store<S, A>
                    ): Store<S, A> {
                        return runChildReader(CoroutineScope()) {
                                init()
                        }
                    } 
                """,
                initializeInjekt = false
            )
        ),
        listOf(
            source(
                """
                @Reader
                fun storeA(): Store<String, Int> = store("") {
                }
            """,
                initializeInjekt = false
            )
        ),
        listOf(
            source(
                """
                @Reader
                fun storeB(): Store<Int, String> = store(0) {
                }
            """,
                initializeInjekt = false
            )
        ),
        listOf(
            source(
                """
                fun main() {
                    App().runApplicationReader {
                        rememberStore { 
                            storeB()
                            rememberStore {
                                storeA()
                            }
                        }
                    }
                } 
            """
            )
        )
    ) {
        it.last().assertOk()
    }

    @Test
    fun testRunReaderInsideSuspend() = codegen(
        """
        @Given
        fun foo() = Foo()
        @Given
        fun bar() = Bar(given())
        
        fun invoke(): Bar {
            return runBlocking {
                runReader { 
                    delay(1)
                    given<Bar>()
                }
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testRunReaderWrapperInsideSuspend() = codegen(
        """
        @Given
        fun foo() = Foo()
        @Given
        fun bar() = Bar(given())
        
        inline fun <R> runBarReader(block: @Reader () -> R) = runReader("hello world") { block() }
        
        fun invoke(): Bar {
            return runBlocking {
                runBarReader {
                    delay(1)
                    given<Bar>()
                }
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testScoping() = codegen(
        """
        
        private var foo: Foo? = null

        @Scoping
        object Singleton {
            @Reader
            operator fun <T : Foo> invoke(key: Any, init: () -> T): T {
                foo?.let { return it as T }
                foo = init()
                return foo as T
            }
        }
        
        @Given(Singleton::class)
        fun foo() = Foo()
        
        fun invoke(): Foo {
            return runReader { given<Foo>() }
        }
    """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testRunReaderWrapper() = codegen(
        """
        fun runApplicationReader(block: @Reader () -> Foo): Foo {
            return runReader(Foo()) { block() }
        }
        
        fun invoke(): Foo {
            return runApplicationReader { given<Foo>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testRunReaderWrapperMulti() = multiCodegen(
        listOf(
            source(
                """
                    fun runApplicationReader(block: @Reader () -> Foo): Foo {
                        return runReader(Foo()) { block() }
                    }
                    """,
                initializeInjekt = false
            )
        ),
        listOf(
            source(
                """
                    fun invoke(): Foo {
                        return runApplicationReader { given<Foo>() }
                    } 
                """,
                name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

    @Test
    fun testGivenClass() = codegen(
        """
        @Given
        class AnnotatedBar {
            private val foo: Foo = given()
        }
        
        @Given
        fun foo(): Foo = Foo()

        fun invoke(): Any { 
            return runReader { given<AnnotatedBar>() }
        }
    """
    ) {
        invokeSingleFile()
    }

    // todo @Test
    fun testGivenObject() = codegen(
        """
        @Given object AnnotatedFoo

        fun invoke() = {
            return runReader { given<AnnotatedFoo>() }
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenProperty() = codegen(
        """
        @Given val foo = Foo()
        
        fun invoke(): Foo {
            return runReader { given<Foo>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testAssistedGivenFunction() = codegen(
        """ 
        @Given
        fun bar(foo: Foo) = Bar(foo)

        fun invoke(foo: Foo): Bar { 
            return runReader { given<Bar>(foo) }
        }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testAssistedGivenClass() = codegen(
        """ 
        @Given
        class AnnotatedBar(foo: Foo)

        fun invoke(foo: Foo): Any {
            return runReader { given<AnnotatedBar>(foo) }
        }
    """
    ) {
        invokeSingleFile(Foo())
    }

    // todo @Test
    fun testGenericAnnotatedClass() = codegen(
        """
        @Given @Reader class Dep<T> {
            val value: T = given()
        }
        
        @Given fun foo() = Foo() 
        
        fun invoke() {
            runReader {
                given<Dep<Foo>>()
            }
        }
    """
    )

    @Test
    fun testRunReaderInput() = codegen(
        """
        fun invoke(): Pair<Foo, Foo> {
            val foo = Foo()
            return foo to runReader(foo) { given<Foo>() }
        }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

}
