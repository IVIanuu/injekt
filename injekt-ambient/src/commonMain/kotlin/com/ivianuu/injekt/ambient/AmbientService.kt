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

package com.ivianuu.injekt.ambient

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.scope.*

@Tag annotation class AmbientService<N> {
  companion object {
    @Provide fun <@Spread T : @AmbientService<N> S, S, N> ambient(key: TypeKey<S>): Ambient<S> =
      serviceAmbientOf()

    @Provide fun <@Spread T : @AmbientService<N> S, S, N> providedServiceValue(
      factory: () -> T,
      key: TypeKey<S>
    ): NamedProvidedValue<N, S> = provide(ambient = serviceAmbientOf(), factory = factory)
  }
}

@OptIn(InternalScopeApi::class)
@Suppress("UNCHECKED_CAST")
fun <T> serviceAmbientOf(@Inject key: TypeKey<T>): ProvidableAmbient<T> =
  scoped(scope = SingletonScope) {
    ambientOf { error("No service provided for ${key.value}") }
  }
