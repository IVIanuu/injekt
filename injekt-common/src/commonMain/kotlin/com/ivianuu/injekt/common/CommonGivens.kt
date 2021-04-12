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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Given
import kotlin.reflect.KClass

/**
 * Allows to use a Map<K, V> for each Set<Pair<K, V>>
 */
@Given
inline fun <K, V> setOfPairsToMap(@Given pairs: Set<Pair<K, V>>): Map<K, V> = pairs.toMap()

/**
 * Allows to use a [KClass] of type [Ŧ]
 */
@Given
inline fun <reified T : Any> givenKClass(): KClass<T> = T::class

/**
 * Allows to use a [TypeKey] of type [T]
 */
@Given
inline fun <@ForTypeKey T> givenTypeKey(): TypeKey<T> = typeKeyOf()
