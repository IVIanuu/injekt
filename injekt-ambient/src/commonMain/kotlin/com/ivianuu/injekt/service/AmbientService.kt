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

package com.ivianuu.injekt.service

import com.ivianuu.injekt.*
import com.ivianuu.injekt.ambient.*
import com.ivianuu.injekt.ambient.synchronized
import com.ivianuu.injekt.common.*
import kotlin.Any
import kotlin.PublishedApi
import kotlin.String
import kotlin.Suppress
import kotlin.error
import kotlin.let

@Tag annotation class AmbientService<N> {
  companion object {
    @Suppress("NOTHING_TO_INLINE")
    inline fun <T> current(@Inject key: TypeKey<T>, @Inject ambients: Ambients): T =
      serviceAmbientOf<T>().current().invoke()

    @Provide class Module<@Spread T : @AmbientService<N> U, U : Any, N> {
      @Provide inline fun providedServiceValue(
        noinline factory: () -> T,
        key: TypeKey<U>
      ): NamedProvidedValue<N, () -> U> = serviceAmbientOf<U>() provides factory
    }
  }
}

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T> serviceAmbientOf(@Inject key: TypeKey<T>): ProvidableAmbient<() -> T> {
  serviceAmbients[key.value]?.let { return it as ProvidableAmbient<() -> T> }
  synchronized(serviceAmbients) {
    serviceAmbients[key.value]?.let { return it as ProvidableAmbient<() -> T> }
    val ambient = ambientOf<() -> T> { error("No service provided for ${key.value}") }
    serviceAmbients[key.value] = ambient
    return ambient
  }
}

private val serviceAmbients = mutableMapOf<String, ProvidableAmbient<*>>()
