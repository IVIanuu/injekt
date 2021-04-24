/*
 * Copyright 2021 Manuel Wrage
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

package com.ivianuu.injekt.compose

import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.*
import androidx.test.ext.junit.runners.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.scope.*
import io.kotest.matchers.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.*

@RunWith(AndroidJUnit4::class)
class ComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testElement() {
        @Given val element: @InstallElement<TestGivenScope1> String = "value"
        val scope = given<TestGivenScope1>()
        composeRule.setContent {
            CompositionLocalProvider(LocalGivenScope provides scope) {
                element<String>() shouldBe "value"
            }
        }
    }

    @Test
    fun testInitialRememberScoped() {
        val scope = given<TestGivenScope1>()
        composeRule.setContent {
            CompositionLocalProvider(LocalGivenScope provides scope) {
                val value = rememberScoped(key = "key") { "a" }
                value shouldBe "a"
                DisposableEffect(Unit) {
                    scope.getScopedValueOrNull<String>("key")
                        .shouldBe("a")
                    onDispose {  }
                }
            }
        }
    }

    @Test
    fun testExistingInitialRememberScoped() {
        val scope = given<TestGivenScope1>()
        scope.setScopedValue("key", "b")
        composeRule.setContent {
            CompositionLocalProvider(LocalGivenScope provides scope) {
                val value = rememberScoped(key = "key") { "a" }
                value shouldBe "b"
                DisposableEffect(Unit) {
                    scope.getScopedValueOrNull<String>("key")
                        .shouldBe("b")
                    onDispose {  }
                }
            }
        }
    }

    /*@Test
    fun testRememberScopedAcrossRecompositions() {
        val scope = given<TestGivenScope1>()
        val composed = MutableStateFlow(0)
        composeRule.setContent {
            CompositionLocalProvider(LocalGivenScope provides scope) {
                var firstComposition by remember { mutableStateOf(true) }
                val value = rememberScoped(key = "key") { if (firstComposition) "a" else "b" }
                composed.value++
                value shouldBe "a"
                DisposableEffect(firstComposition) {
                    if (firstComposition) firstComposition = false
                    onDispose {
                    }
                }
            }
        }
        runBlocking { composed.first { it == 2 } }
    }     */
}

typealias TestGivenScope1 = GivenScope
