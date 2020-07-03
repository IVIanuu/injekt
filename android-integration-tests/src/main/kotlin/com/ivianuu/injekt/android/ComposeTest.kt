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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.ui.test.createComposeRule
import com.ivianuu.injekt.compiler.InjektComponentRegistrar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

class ComposeTest {

    @Test
    fun testComposableReadableLambda() = codegen(
        """
        @CompositionComponent 
        interface TestCompositionComponent

        @CompositionFactory 
        fun factory(): TestCompositionComponent {
            transient { Foo() }
            return create() 
        }
        
        @Readable 
        @androidx.compose.Composable
        fun func(foo: Foo = given()): Foo {
            androidx.compose.currentComposer
            return foo
        }
        
        @Readable 
        @androidx.compose.Composable
        fun other() { 
            androidx.compose.currentComposer
        }
        
        @Readable
        @androidx.compose.Composable
        fun <R> withFoo(block: @Readable @androidx.compose.Composable (Foo) -> R): R = block(func())

        @androidx.compose.Composable
        fun cool(): Foo {
            initializeCompositions()
            val component = compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
            return component.runReading {
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
                transient<Foo>()
            }
            
            @androidx.compose.Composable
            fun <T> inject(): T { 
                val component = ComponentAmbient.current
                return androidx.compose.remember(component) { component.get() }
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
                transient<Foo>()
            }
            
            @androidx.compose.Composable
            fun <T> inject(): T { 
                val component = ComponentAmbient.current
                return androidx.compose.remember(component) { component.get() }
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
                    transient<Foo>()
                }
                
                @androidx.compose.Composable 
                fun <T> inject(): T { 
                    val component = ComponentAmbient.current
                    return androidx.compose.remember(component) { component.get() }
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
                    transient<Foo>()
                }
                
                @androidx.compose.Composable 
                fun <T> inject(): T { 
                    val component = ComponentAmbient.current
                    return androidx.compose.remember(component) { component.get() }
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
