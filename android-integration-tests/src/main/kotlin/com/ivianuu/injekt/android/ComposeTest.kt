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

package com.ivianuu.injekt.android

import androidx.compose.compiler.plugins.kotlin.ComposeComponentRegistrar
import com.ivianuu.injekt.compiler.InjektComponentRegistrar
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class ComposeTest {

    @Test
    fun testComposableBindingEffect() = codegen("""
        typealias AppUi = @androidx.compose.runtime.Composable () -> Unit

        @Effect
        annotation class GivenAppUi {
            companion object {
                @Given
                operator fun <T : @androidx.compose.runtime.Composable () -> Unit> invoke(): AppUi =
                    given<T>() as AppUi
            }
        }
        
        @GivenAppUi
        @androidx.compose.runtime.Composable
        fun SampleUi() {
            androidx.compose.runtime.remember {  }
        }
        
        fun invoke(): AppUi {
            return rootContext<TestComponent>().runReader { given<AppUi>() }
        }
    """,
        config = {
            compilerPlugins = listOf(InjektComponentRegistrar(), ComposeComponentRegistrar())
        }
    ) {
        invokeSingleFile()
    }

    @Test
    fun testComposableBindingEffectMulti() = multiCodegen(
        listOf(
            source(
                """
                typealias AppUi = @androidx.compose.runtime.Composable () -> Unit
        
                @Effect
                annotation class GivenAppUi {
                    companion object {
                        @Given
                        operator fun <T : @androidx.compose.runtime.Composable () -> Unit> invoke(): AppUi =
                            given<T>() as AppUi
                    }
                }
                """,
                initializeInjekt = false
            )
        ),
        listOf(
            source(
                """
                @GivenAppUi
                @androidx.compose.runtime.Composable
                fun SampleUi() {
                    androidx.compose.runtime.remember {  }
                }
                
                fun invoke(): AppUi {
                    return rootContext<TestComponent>().runReader { given<AppUi>() }
                }
                """,
                name = "File.kt"
            )
        ),
        config = {
            compilerPlugins = listOf(InjektComponentRegistrar(), ComposeComponentRegistrar())
        }
    ) {
        it.last().invokeSingleFile()
    }

    // todo @Test
    fun testComposableReaderLambda() = codegen(
        """
        @Given
        fun foo() = Foo()
        
        @Reader 
        @androidx.compose.runtime.Composable
        fun func(foo: Foo = given()): Foo {
            androidx.compose.currentComposer
            return foo
        }
        
        @Reader 
        @androidx.compose.runtime.Composable
        fun other() { 
            androidx.compose.currentComposer
        }
        
        @Reader
        @androidx.compose.runtime.Composable
        fun <R> withFoo(block: @Reader @androidx.compose.runtime.Composable (Foo) -> R): R = block(func())

        @androidx.compose.runtime.Composable
        fun invoke(): Foo {
            return runTestReader {
                withFoo { 
                    other()
                    it
                }
            }
        }
    """,
        config = {
            compilerPlugins = listOf(InjektComponentRegistrar(), ComposeComponentRegistrar())
        }
    ) {
        assertOk()
    }
}
