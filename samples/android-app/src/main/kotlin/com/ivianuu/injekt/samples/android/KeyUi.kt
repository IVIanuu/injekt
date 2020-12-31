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

package com.ivianuu.injekt.samples.android

import androidx.compose.runtime.Composable
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.Macro
import com.ivianuu.injekt.Qualifier
import kotlin.reflect.KClass

typealias KeyUiElement = Pair<KClass<*>, @Composable () -> Unit>

@Qualifier annotation class KeyUiBinding<K : Any>

@Macro @GivenSetElement inline fun <
        reified T : @KeyUiBinding<K> @Composable () -> Unit,
        reified K : Any
        > keyUiBinding(@Given instance: T): KeyUiElement = K::class to instance
