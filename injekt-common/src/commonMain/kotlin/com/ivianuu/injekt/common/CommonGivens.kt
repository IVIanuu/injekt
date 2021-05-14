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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*
import kotlin.reflect.*

/**
 * Allows to use a Map<K, V> for each Set<Pair<K, V>>
 */
@Given inline fun <K, V> givenMap(@Given pairs: Set<Pair<K, V>>): Map<K, V> = pairs.toMap()

/**
 * Allows to use a [KClass] for [T]
 */
@Given inline fun <reified T : Any> givenKClass(): KClass<T> = T::class

/**
 * Allows to use a [TypeKey] for [T]
 */
@Given inline fun <@ForTypeKey T> givenTypeKey(): TypeKey<T> = typeKeyOf()

/**
 * Allows to use a [Lazy] for [T]
 */
@Given inline fun <T> givenLazy(@Given noinline init: () -> T): Lazy<T> = lazy(init)
