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

class EffectTest {

    @Test
    fun testSimpleEffect() = codegen(
        """
        @Effect
        annotation class Effect1 {
            companion object {
                @Given
                fun <T> bind() = given<T>().toString()
            }
        }
        
        @Effect
        annotation class Effect2 {
            companion object {
                @Given
                fun <T : Any> bind(): Any = given<T>()
            }
        }
        
        @Effect1
        @Effect2
        @Given
        class Dep
        
        fun invoke() {
            runReader { 
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
    fun testEffectWithoutCompanion() = codegen(
        """
        @Effect
        annotation class MyEffect
    """
    ) {
        assertCompileError("companion")
    }

    @Test
    fun testEffectWithoutTypeParameters() = codegen(
        """
        @Effect
        annotation class MyEffect {
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
    fun testEffectWithMultipleTypeParameters() = codegen(
        """
        @Effect
        annotation class MyEffect {
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
    fun testEffectWithFunction() = codegen(
        """
        @Effect
        annotation class MyEffect {
            companion object {
                @EffectFunction(MyEffect::class)
                @Given
                fun <T> bind() {
                }
            }
        }
        
        @MyEffect
        fun myFun() {
        }
    """
    ) {
        assertCompileError("function")
    }

    @Test
    fun testEffectNotInBounds() = codegen(
        """
        @Effect
        annotation class MyEffect {
            companion object { 
                @Given
                fun <T : UpperBound> bind() {
                }
            }
        }
        
        interface UpperBound
        
        @MyEffect
        class MyClass
    """
    ) {
        assertCompileError("bound")
    }

    @Test
    fun testFunctionEffectNotInBounds() = codegen(
        """
        @Effect
        annotation class MyEffect {
            companion object {
                @EffectFunction(MyEffect::class)
                @Given
                fun <T : () -> Unit> bind() {
                }
            }
        }
        @MyEffect
        fun myFun(p0: String) {
        }
    """
    ) {
        assertCompileError("bound")
    }

    @Test
    fun testFunctionEffect() = codegen(
        """
        typealias FooFactory = () -> Foo
        
        @Effect
        annotation class BindFooFactory {
            companion object {
                @Given
                operator fun <T : FooFactory> invoke(): FooFactory = given<T>()
            }
        }
        
        @BindFooFactory
        @Reader
        fun fooFactory(): Foo {
            return Foo()
        }
        
        fun invoke(): Foo { 
            return runReader { given<FooFactory>()() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testFunctionEffectMulti() = multiCodegen(
        listOf(
            source(
                """
                typealias FooFactory = () -> Foo
        
                @Effect
                annotation class BindFooFactory {
                    companion object {
                        @Given
                        operator fun <T : FooFactory> invoke(): FooFactory = given<T>()
                    }
                }
            """,
                initializeInjekt = false
            ),
        ),
        listOf(
            source(
                """
                @BindFooFactory
                @Reader
                fun fooFactory(): Foo {
                    return Foo()
                }
            """,
                initializeInjekt = false
            )
        ),
        listOf(
            source(
                """
                fun invoke(): Foo { 
                    return runReader { given<FooFactory>()() }
                }
            """,
                name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

    @Test
    fun testSuspendFunctionEffect() = codegen(
        """
        typealias FooFactory = suspend () -> Foo
        
        @Effect
        annotation class BindFooFactory {
            companion object {
                @Given
                operator fun <T : FooFactory> invoke(): FooFactory = given<T>()
            }
        }
        
        @BindFooFactory
        @Reader
        suspend fun fooFactory(): Foo {
            return Foo()
        }
        
        fun invoke(): Foo { 
            return runReader { 
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
