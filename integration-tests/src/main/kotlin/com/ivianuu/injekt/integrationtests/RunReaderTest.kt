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
    fun testRunChildReaderWithEffect() = codegen(
        """ 
            @Effect
            annotation class GivenFooFactory {
                companion object {
                    @Given
                    fun <T : () -> Foo> invoke(): FooFactoryMarker = given<T>()
                }
            }

            typealias FooFactoryMarker = () -> Foo
            
            @GivenFooFactory
            fun FooFactoryImpl() = runChildReader { given<Foo>() }
            
            fun invoke(foo: Foo): Foo {
                return runReader(foo) {
                    given<FooFactoryMarker>()()
                }
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    // todo simplify this test
    @Test
    fun testRunChildReaderWithEffectAndGenerics() = codegen(
        """
            class App
            
            class Activity {
                val app = App()
            }
            
            class Service {
                val app = App()
            }
            
            class Fragment {
                val activity = Activity()
            }
            
            inline fun <T> App.runAppReader(block: @Reader () -> T): T =
                runReader(this) { block() }
                
            inline fun <T> Activity.runActivityReader(block: @Reader () -> T): T =
                app.runAppReader {
                    runChildReader(this) { block() }
                }
                
            inline fun <T> Service.runServiceReader(block: @Reader () -> T): T =
                app.runAppReader {
                    runChildReader(this) { block() }
                }
                
            inline fun <T> Fragment.runFragmentReader(block: @Reader () -> T): T =
                activity.runActivityReader {
                    runChildReader(this) { block() }
                }
            
            @Effect
            annotation class GivenAppUi {
                companion object {
                    @Given
                    fun <T : () -> Unit> invoke(): AppUiMarker = given<T>()
                }
            }

            typealias AppUiMarker = () -> Unit
            
            @GivenAppUi
            fun AppUiImpl() {
                remember { 
                    given<App>()
                    ActionPickerPage()
                }
            }

            @Reader
            fun ActionPickerPage() {
                remember { given<Activity>() }
            }
            
            interface ActionPickerDelegate
            
            @Effect
            annotation class BindActionPickerDelegate {
                companion object {
                    @SetElements
                    fun <T : ActionPickerDelegate> invoke(): Set<ActionPickerDelegate> = setOf(given<T>())
                }
            }
            
            @BindActionPickerDelegate
            class AppActionPickerDelegate : ActionPickerDelegate {
                init {
                    remember { given<App>() }
                    AppPickerPage()
                }
            }
            
            @Given
            fun AppPickerPage() {
                remember { given<Activity>() }
            }
            
            fun invoke() { 
                Activity().runActivityReader {
                    given<AppUiMarker>()()
                }
            }
            
            @Reader
            fun <T> remember(block: @Reader () -> T): T {
                return remember(*emptyArray()) { block() }
            }
            
            @Reader
            fun <T> remember(vararg inputs: Any?, block: @Reader () -> T): T {
                return runChildReader(Foo()) { block() }
            }
        """
    ) {
        assertOk()
        invokeSingleFile()
    }

    // todo simplify this test
    @Test
    fun testRunChildReaderWithEffectAndGenerics2() = codegen(
        """
            class App
            
            class Activity {
                val app = App()
            }
            
            class Service {
                val app = App()
            }
            
            class Fragment {
                val activity = Activity()
            }
            
            inline fun <T> App.runAppReader(block: @Reader () -> T): T =
                runReader(this) { block() }
                
            inline fun <T> Activity.runActivityReader(block: @Reader () -> T): T =
                app.runAppReader {
                    runChildReader(this) { block() }
                }
                
            inline fun <T> Service.runServiceReader(block: @Reader () -> T): T =
                app.runAppReader {
                    runChildReader(this) { block() }
                }
                
            inline fun <T> Fragment.runFragmentReader(block: @Reader () -> T): T =
                activity.runActivityReader {
                    runChildReader(this) { block() }
                }
            
            @Effect
            annotation class GivenAppUi {
                companion object {
                    @Given
                    fun <T : () -> Unit> invoke(): AppUiMarker = given<T>()
                }
            }

            typealias AppUiMarker = () -> Unit
            
            @GivenAppUi
            fun AppUiImpl() {
                remember { 
                    given<App>()
                    ActionPickerPage()
                }
            }

            @Reader
            fun ActionPickerPage() {
                remember { given<Activity>() }
            }
            
            interface ActionPickerDelegate
            
            @Effect
            annotation class GivenActionPickerDelegate {
                companion object {
                    @SetElements
                    fun <T : ActionPickerDelegate> invoke(): Set<ActionPickerDelegate> = setOf(given<T>())
                }
            }
            
            @GivenActionPickerDelegate
            class AppActionPickerDelegate : ActionPickerDelegate {
                init {
                    remember { given<App>() }
                    AppPickerPage()
                }
            }
            
            @Given
            fun AppPickerPage() {
                remember { given<Activity>() }
            }
            
            fun invoke() { 
                Activity().runActivityReader {
                    given<AppUiMarker>()()
                }
            }
            
            @Reader
            fun <T> remember(block: @Reader () -> T): T {
                return runChildReader(Foo()) { block() }
            }
        """
    ) {
        assertOk()
        invokeSingleFile()
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
