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

class ReaderContextTest {

    @Test
    fun testSimple() = codegen(
        """
            @Given
            fun foo() = Foo()
            @Given
            fun bar() = Bar(given())
            
            fun invoke(): Bar {
                return rootContext<TestContext>().runReader { given<Bar>() }
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
                return rootContext<TestParentContext>(42, "hello world").runReader { overriddingFoo(foo) }
            }
            
            fun otherInvoke() = rootContext<TestParentContext>().runReader { overriddingFoo(Foo()) }
            
            @Reader
            private fun overriddingFoo(foo: Foo) = childContext<TestChildContext>(foo).runReader {
                given<Bar>().foo
            }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testWithMultiChild() = codegen(
        """
            @Given
            fun foo() = Foo()
            @Given
            fun bar() = Bar(given())
            
            fun invoke(foo: Foo): Foo {
                return rootContext<TestParentContext>(42, "hello world", foo).runReader { overriddingFoo(foo) }
            }
            
            fun otherInvoke() = rootContext<TestParentContext>().runReader { overriddingFoo(Foo()) }
            
            @Reader
            private fun overriddingFoo(foo: Foo) = childContext<TestChildContext>(Foo()).runReader {
                childContext<TestContext>(foo).runReader {
                    given<Bar>().foo
                }
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
            return rootContext<TestContext>(42, "hello world").runReader { overriding<Bar>(foo) }
        }
        
        fun otherInvoke() = rootContext<TestContext2>().runReader { overriding<Bar>(Foo()) }
        
        @Reader
        private fun <T> overriding(value: Foo) = childContext<TestChildContext>(value).runReader {
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
            return rootContext<TestParentContext>(42, true).runReader { genericA<Bar>(foo) }
        }
        
        @Reader
        fun <T> genericA(foo: Foo) = childContext<TestChildContext>(foo, "").runReader {
            nonGeneric(foo)
        }
        
        @Reader
        private fun nonGeneric(foo: Foo) = genericB<String>(foo)

        @Reader
        private fun <S> genericB(foo: Foo) = childContext<TestChildContext2>(foo, 0L).runReader {
            given<Bar>().foo
        }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testGenericRequestInChildContext() = codegen(
        """
            val parentContext = rootContext<TestParentContext>()
            
            fun invoke() {
                parentContext.runReader {
                    val foo = diyGiven<Foo>()
                }
            }
            
            @Reader
            fun <T> diyGiven() = childContext<TestChildContext>().runReader { given<T>() }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGenericChildContextInput() = codegen(
        """
            val parentContext = rootContext<TestParentContext>()
            
            fun invoke() {
                parentContext.runReader {
                    val foo = childContextBuilder<Foo>(Foo()) { given<Foo>() }
                }
            }
            
            @Reader
            fun <T> childContextBuilder(
                value: T,
                block: @Reader () -> T
            ) = childContext<TestChildContext>(value).runReader { block() }
        """
    ) {
        invokeSingleFile()
    }

    // todo @Test
    fun testCircular() = codegen(
        """
            @Reader
            fun a(foo: Foo) = childRunner(Foo()) { b(foo) }
            
            @Reader
            fun b(foo: Foo) = childRunner(foo) { given<Foo>() }
            
            @Reader
            fun <R> childRunner(foo: Foo, block: @Reader () -> R) = childContext<TestChildContext>(foo).runReader(block = block)
            
            fun invoke(foo: Foo) = rootContext<TestParentContext>().runReader { a(foo) }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    // todo @Test
    fun testCircular2() = codegen(
        """
            @Reader
            fun a(foo: Foo) = childRunner(Foo()) { b(foo) }
            
            @Reader
            fun b(foo: Foo) = c(foo)
            
            @Reader
            fun c(foo: Foo) = childRunner(foo) { given<Foo>() }
            
            @Reader
            fun <R> childRunner(foo: Foo, block: @Reader () -> R) = childContext<TestChildContext>(foo).runReader(block = block)
            
            fun invoke(foo: Foo) = rootContext<TestParentContext>().runReader { a(foo) }
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
            ) = rootContext<TestContext>().runReader {
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

            @Context
            interface StringContext
            @Reader
            private fun <R> withString(
                value: String,
                block: @Reader () -> R
            ) = childContext<StringContext>(value).runReader(block = block)
            
            @Context
            interface IntContext
            @Reader
            private fun <R> withInt(
                value: Int,
                block: @Reader () -> R
            ) = childContext<IntContext>(value).runReader(block = block)
            
            @Context
            interface LongContext
            @Reader
            private fun <R> withLong(
                value: Long,
                block: @Reader () -> R
            ) = childContext<LongContext>(value).runReader(block = block)
            
            @Context
            interface BooleanContext
            @Reader
            private fun <R> withBoolean(
                value: Boolean,
                block: @Reader () -> R
            ) = childContext<BooleanContext>(value).runReader(block = block)
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
            fun FooFactoryImpl() = childContext<TestChildContext>().runReader { given<Foo>() }
            
            fun invoke(foo: Foo): Foo {
                return rootContext<TestParentContext>(foo).runReader {
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
            
            @Context
            interface AppContext
            inline fun <T> App.runAppReader(block: @Reader () -> T): T =
                rootContext<AppContext>(this).runReader(block = block)

            @Context
            interface ActivityContext
            inline fun <T> Activity.runActivityReader(block: @Reader () -> T): T =
                app.runAppReader {
                    childContext<ActivityContext>(this).runReader(block = block)
                }
                
            @Context
            interface ServiceContext
            inline fun <T> Service.runServiceReader(block: @Reader () -> T): T =
                app.runAppReader {
                    childContext<ServiceContext>(this).runReader(block = block)
                }
           
            @Context
            interface FragmentContext
            inline fun <T> Fragment.runFragmentReader(block: @Reader () -> T): T =
                activity.runActivityReader {
                    childContext<FragmentContext>(this).runReader(block = block)
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
                    @GivenSetElements
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
                return remember(*emptyArray(), block = block)
            }
            
            @Context
            interface RememberContext
            
            @Reader
            fun <T> remember(vararg inputs: Any?, block: @Reader () -> T): T {
                return childContext<RememberContext>(Foo()).runReader(block = block)
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
            
            @Context
            interface AppContext
            inline fun <T> App.runAppReader(block: @Reader () -> T): T =
                rootContext<AppContext>(this).runReader(block = block)

            @Context
            interface ActivityContext
            inline fun <T> Activity.runActivityReader(block: @Reader () -> T): T =
                app.runAppReader {
                    childContext<ActivityContext>(this).runReader(block = block)
                }
                
            @Context
            interface ServiceContext
            inline fun <T> Service.runServiceReader(block: @Reader () -> T): T =
                app.runAppReader {
                    childContext<ServiceContext>(this).runReader(block = block)
                }
           
            @Context
            interface FragmentContext
            inline fun <T> Fragment.runFragmentReader(block: @Reader () -> T): T =
                activity.runActivityReader {
                    childContext<FragmentContext>(this).runReader(block = block)
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
                    @GivenSetElements
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
            
            @Context
            interface RememberContext
            
            @Reader
            fun <T> remember(block: @Reader () -> T): T {
                return childContext<RememberContext>(Foo()).runReader(block = block)
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
                rootContext<TestContext>().runReader { 
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
        
        inline fun <R> runBarReader(block: @Reader () -> R) = rootContext<TestContext>("hello world").runReader(block = block)
        
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
    fun testScopedBinding() = codegen(
        """
        @Given(TestContext::class)
        fun foo() = Foo()
        
        val context = rootContext<TestContext>()
        
        fun invoke(): Foo {
            return context.runReader { given<Foo>() }
        }
    """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testParentScopedBinding() = codegen(
        """
        @Given
        fun foo() = Foo()

        @Given(TestParentContext::class)
        fun bar() = Bar(given())
        
        val parentContext = rootContext<TestParentContext>()
        val childContext = parentContext.runReader {
            childContext<TestChildContext>()
        }
        
        fun invoke(): Bar {
            return childContext.runReader { given<Bar>() }
        }
    """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testParentScopedBinding2() = codegen(
        """
        @Given
        fun foo() = Foo()

        @Given(TestParentContext::class)
        fun bar() = Bar(given())
        
        val parentContext = rootContext<TestParentContext>()
        val childContext = parentContext.runReader {
            childContext<TestChildContext>()
        }
        
        fun invoke(): Bar {
            return childContext.runReader { given<Bar>() }
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
            return rootContext<TestContext>(Foo()).runReader(block = block)
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
                        return rootContext<TestContext>(Foo()).runReader(block = block)
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
            return rootContext<TestContext>().runReader { given<AnnotatedBar>() }
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
            return rootContext<TestContext>().runReader { given<Foo>() }
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
            return rootContext<TestContext>().runReader { given<Bar>(foo) }
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
            return rootContext<TestContext>().runReader { given<AnnotatedBar>(foo) }
        }
    """
    ) {
        invokeSingleFile(Foo())
    }

    // todo @Test
    fun testGenericGivenClass() = codegen(
        """
        @Given class Dep<T> {
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

    // todo @Test
    fun testGenericGivenFunction() = codegen(
        """    
        @Given class Dep<T> { val value: T = given() }
        
        @Given fun <T> dep() = Dep<T>()
        
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
            return foo to rootContext<TestContext>(foo).runReader { given<Foo>() }
        }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testInlinedRunReaderLambda() = codegen(
        """
        @Given
        fun foo() = Foo()
        @Given
        fun bar() = Bar(given())
        
        fun invoke(): Bar {
            return runBlock { given<Bar>() }
        }
        
        fun <R> runBlock(block: @Reader () -> R): R {
            return rootContext<TestContext>().runReader(block = block)
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testWithGivenSet() = codegen(
        """
        @GivenSet
        class FooGivens {
            @Given
            fun foo() = Foo()
        }
        
        @Given
        fun bar() = Bar(given())
        
        fun invoke(): Bar {
            return rootContext<TestContext>(FooGivens()).runReader { given<Bar>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testWithNestedGivenSets() = codegen(
        """
        @GivenSet
        class FooGivens {
            @Given
            fun foo() = Foo()
            
            @GivenSet
            val barGivens = BarGivens()
            
            @GivenSet
            class BarGivens {
                @Given
                fun bar() = Bar(given())
            }
        }
        
        fun invoke(): Bar {
            return rootContext<TestContext>(FooGivens()).runReader { given<Bar>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    // todo @Test
    fun testWithGivenRef() = codegen(
        """
        @Module
        class FooBarGivens {
            @Given
            fun foo() = Foo()
        }
        
        @Given
        fun bar() = Bar(given())
        
        fun invoke(): Bar {
            return rootContext<TestContext>(FooBarModule()).runReader { given<Bar>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

}
