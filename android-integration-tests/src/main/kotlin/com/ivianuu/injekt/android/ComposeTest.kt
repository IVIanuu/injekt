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

import androidx.compose.plugins.kotlin.ComposeComponentRegistrar
import com.ivianuu.injekt.compiler.InjektComponentRegistrar
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class ComposeTest {

    @Test
    fun testComposableBindingEffect() = codegen("""
        @CompositionComponent 
        interface TestCompositionComponent
        
        @Target(AnnotationTarget.TYPE)
        @Qualifier
        annotation class AppUi

        @BindingEffect(TestCompositionComponent::class)
        annotation class BindAppUi {
            companion object {
                @Module
                operator fun <T : @androidx.compose.Composable () -> Unit> invoke() {
                    alias<T, @AppUi @androidx.compose.Composable () -> Unit>()
                }
            }
        }
        
        @BindAppUi
        @Reader
        @androidx.compose.Composable
        fun SampleUi() {
            androidx.compose.remember {  }
        }
    """,
        config = {
            compilerPlugins = listOf(InjektComponentRegistrar(), ComposeComponentRegistrar())
        }
    )

    @Test
    fun testComposableBindingEffectMulti() = multiCodegen(
        listOf(
            source(
                """
                @CompositionComponent 
                interface TestCompositionComponent
        
                @Target(AnnotationTarget.TYPE)
                @Qualifier
                annotation class AppUi

                @BindingEffect(TestCompositionComponent::class)
                annotation class BindAppUi {
                    companion object {
                        @Module
                        operator fun <T : @androidx.compose.Composable () -> Unit> invoke() {
                            alias<T, @AppUi @androidx.compose.Composable () -> Unit>()
                        }
                    }
                }
                """
            )
        ),
        listOf(
            source(
                """
                @BindAppUi
                @Reader
                @androidx.compose.Composable
                fun SampleUi() {
                    androidx.compose.remember {  }
                }
                """
            )
        ),
        config = {
            compilerPlugins = listOf(InjektComponentRegistrar(), ComposeComponentRegistrar())
        }
    )

    @Test
    fun testComposableReaderLambda() = codegen(
        """
        @CompositionComponent 
        interface TestCompositionComponent

        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            unscoped { Foo() }
            return create() 
        }
        
        @Reader 
        @androidx.compose.Composable
        fun func(foo: Foo = given()): Foo {
            androidx.compose.currentComposer
            return foo
        }
        
        @Reader 
        @androidx.compose.Composable
        fun other() { 
            androidx.compose.currentComposer
        }
        
        @Reader
        @androidx.compose.Composable
        fun <R> withFoo(block: @Reader @androidx.compose.Composable (Foo) -> R): R = block(func())

        @androidx.compose.Composable
        fun cool(): Foo {
            initializeCompositions()
            val component = componentFactory<TestComponent.Factory>().create()
            return component.runReader {
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
        /*composeTestRule.setContent {
            assertTrue(invokeSingleFile() is Foo)
        }*/
    }

    @Test
    fun testGetInComposableWithCompilingAfterCompose() =
        codegen(
            """
            val ComponentAmbient = androidx.compose.staticAmbientOf<TestCompositionComponent>()
            
            @Module
            fun module() {
                installIn<TestCompositionComponent>()
                unscoped<Foo>()
            }
            
            @androidx.compose.Composable
            fun <T> inject(): T { 
                val component = ComponentAmbient.current
                return androidx.compose.remember(component) { component.given() }
            }
            
            @androidx.compose.Composable
            fun caller() {
                initializeCompositions()
                val foo = inject<Foo>()
            }
        """,
            config = {
                val other = compilerPlugins.toList()
                compilerPlugins = listOf(ComposeComponentRegistrar()) + other
            }
        ) {
            assertOk()
        }

    @Test
    fun testGetInComposableWithCompilingBeforeCompose() =
        codegen(
            """
            val ComponentAmbient = androidx.compose.staticAmbientOf<TestCompositionComponent>()
            
            @Module
            fun module() {
                installIn<TestCompositionComponent>()
                unscoped<Foo>()
            }
            
            @androidx.compose.Composable
            fun <T> inject(): T { 
                val component = ComponentAmbient.current
                return androidx.compose.remember(component) { component.given() }
            }
            
            @androidx.compose.Composable
            fun caller() {
                initializeCompositions()
                val foo = inject<Foo>()
            }
        """,
            config = {
                val other = compilerPlugins.toList()
                compilerPlugins = other + listOf(ComposeComponentRegistrar())
            }
        ) {
            assertOk()
        }

    @Test
    fun testGetInComposableWithCompilingAfterComposeMulti() = multiCodegen(
        listOf(
            source(
                """
                val ComponentAmbient = androidx.compose.staticAmbientOf<TestCompositionComponent>()
                
                @Module 
                fun module() {
                    installIn<TestCompositionComponent>()
                    unscoped<Foo>()
                }
                
                @androidx.compose.Composable 
                fun <T> inject(): T { 
                    val component = ComponentAmbient.current
                    return androidx.compose.remember(component) { component.given() }
                }
        """
            )
        ),
        listOf(
            source(
                """
                @androidx.compose.Composable 
                fun caller() {
                    initializeCompositions()
                    val foo = inject<Foo>()
                } 
            """
            )
        )
        ,
        config = {
            val other = compilerPlugins.toList()
            compilerPlugins = listOf(ComposeComponentRegistrar()) + other
        }
    ) { results ->
        results.forEach { it.assertOk() }
    }

    @Test
    fun testGetInComposableWithCompilingBeforeComposeMulti() = multiCodegen(
        listOf(
            source(
                """
                val ComponentAmbient = androidx.compose.staticAmbientOf<TestCompositionComponent>()
                
                @Module 
                fun module() {
                    installIn<TestCompositionComponent>()
                    unscoped<Foo>()
                }
                
                @androidx.compose.Composable 
                fun <T> inject(): T { 
                    val component = ComponentAmbient.current
                    return androidx.compose.remember(component) { component.given() }
                }
        """
            )
        ),
        listOf(
            source(
                """
                @androidx.compose.Composable 
                fun caller() {
                    initializeCompositions()
                    val foo = inject<Foo>()
                } 
            """
            )
        )
        ,
        config = {
            val other = compilerPlugins.toList()
            compilerPlugins = other + listOf(ComposeComponentRegistrar())
        }
    ) { results ->
        results.forEach { it.assertOk() }
    }

}
