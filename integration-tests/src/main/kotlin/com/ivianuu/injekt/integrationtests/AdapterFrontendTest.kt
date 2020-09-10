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

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertTrue
import org.junit.Test

class AdapterFrontendTest {

    @Test
    fun testSimpleAdapter() = codegen(
        """
        @Adapter
        annotation class Adapter1 {
            companion object {
                @Given
                fun <T> bind() = given<T>().toString()
            }
        }
        
        @Adapter
        annotation class Adapter2 {
            companion object {
                @Given
                fun <T : Any> bind(): Any = given<T>()
            }
        }
        
        @Adapter1
        @Adapter2
        class Dep
        
        fun invoke() {
            rootContext<TestContext>().runReader { 
                given<Dep>() 
                given<String>()
                given<Any>()
            }
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testAdapterWithoutCompanion() = codegen(
        """
        @Adapter
        annotation class MyAdapter
    """
    ) {
        assertCompileError("companion")
    }

    @Test
    fun testAdapterWithoutTypeParameters() = codegen(
        """
        @Adapter
        annotation class MyAdapter {
            companion object {
                @Given
                operator fun invoke() {
                }
            }
        }
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testAdapterWithMultipleTypeParameters() = codegen(
        """
        @Adapter
        annotation class MyAdapter {
            companion object {
                @Given
                operator fun <A, B> invoke() {
                }
            }
        }
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testAdapterWithFunction() = codegen(
        """
        @Adapter
        annotation class MyAdapter {
            companion object {
                @AdapterFunction(MyAdapter::class)
                @Given
                fun <T> bind() {
                }
            }
        }
        
        @MyAdapter
        fun myFun() {
        }
    """
    ) {
        assertCompileError("function")
    }

    @Test
    fun testAdapterNotInBounds() = codegen(
        """
        @Adapter
        annotation class MyAdapter {
            companion object { 
                @Given
                fun <T : UpperBound> bind() {
                }
            }
        }
        
        interface UpperBound
        
        @MyAdapter
        class MyClass
    """
    ) {
        assertCompileError("bound")
    }

    @Test
    fun testFunctionAdapterNotInBounds() = codegen(
        """
        @Adapter
        annotation class MyAdapter {
            companion object {
                @AdapterFunction(MyAdapter::class)
                fun <T : () -> Unit> bind() {
                }
            }
        }
        @MyAdapter
        fun myFun(p0: String) {
        }
    """
    ) {
        assertCompileError("bound")
    }

    @Test
    fun testFunctionAdapter() = codegen(
        """
        typealias FooFactory = () -> Foo
        
        @Adapter
        annotation class GivenFooFactory {
            companion object {
                @Given
                operator fun <T : FooFactory> invoke(): FooFactory = given<T>()
            }
        }
        
        @GivenFooFactory
        fun fooFactory(): Foo {
            return Foo()
        }
        
        fun invoke(): Foo { 
            return rootContext<TestContext>().runReader { given<FooFactory>()() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testFunctionAdapterMulti() = multiCodegen(
        listOf(
            source(
                """
                typealias FooFactory = () -> Foo
        
                @Adapter
                annotation class GivenFooFactory {
                    companion object {
                        @Given
                        operator fun <T : FooFactory> invoke(): FooFactory = given<T>()
                    }
                }
            """
            ),
        ),
        listOf(
            source(
                """
                @GivenFooFactory
                fun fooFactory(): Foo {
                    return Foo()
                }
            """
            )
        ),
        listOf(
            source(
                """
                fun invoke(): Foo { 
                    return rootContext<TestContext>().runReader { given<FooFactory>()() }
                }
            """,
                name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

    @Test
    fun testSuspendFunctionAdapter() = codegen(
        """
        typealias FooFactory = suspend () -> Foo
        
        @Adapter
        annotation class GivenFooFactory {
            companion object {
                @Given
                operator fun <T : FooFactory> invoke(): FooFactory = given<T>()
            }
        }
        
        @GivenFooFactory
        suspend fun fooFactory(): Foo {
            return Foo()
        }
        
        fun invoke(): Foo { 
            return rootContext<TestContext>().runReader { 
                runBlocking { 
                    delay(1)
                    given<FooFactory>()() 
                }
            }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

}
