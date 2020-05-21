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
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ComposeTest {

    private val composeSource = """
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
                generateCompositions()
                val foo = inject<Foo>()
            }
        """

    @Test
    fun testGetInComposableWithCompilingAfterCompose() =
        codegen(
            composeSource,
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
            composeSource,
            config = {
                val other = compilerPlugins.toList()
                compilerPlugins = other + listOf(ComposeComponentRegistrar())
            }
        ) {
            assertOk()
        }


}
