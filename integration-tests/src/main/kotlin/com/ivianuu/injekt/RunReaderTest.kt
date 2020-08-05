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
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
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
    fun testAppActReader() = codegen(
        """
        class App
        class Activity {
            val app = App()
        }
         
        inline fun <R> App.runApplicationReader(block: @Reader () -> R) = runReader(this) {
            block()
        }
        
        inline fun <R> Activity.runActivityReader(block: @Reader () -> R) = app.runApplicationReader {
            runChildReader(this) {
                block()
            }
        }
    """
    ) {
        //val foo = Foo()
        //assertSame(foo, invokeSingleFile(foo))
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
