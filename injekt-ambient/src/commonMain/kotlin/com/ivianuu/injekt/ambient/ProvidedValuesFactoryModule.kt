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

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt.ambient

import com.ivianuu.injekt.*
import com.ivianuu.injekt.service.*

@Tag annotation class ProvidedValuesFactory

abstract class AbstractProvidedValuesFactoryModule<P, T, S : T> {
  @Provide fun factoryService(factory: S): @AmbientService<P> @ProvidedValuesFactory T = factory
}

class ProvidedValuesFactoryModule0<P, C> :
  AbstractProvidedValuesFactoryModule<P, () -> NamedProvidedValues<C>, () -> NamedProvidedValues<C>>()

class ProvidedValuesFactoryModule1<P, P1, C> : AbstractProvidedValuesFactoryModule<P,
      (P1) -> NamedProvidedValues<C>,
      (@Provide @AmbientService<C> P1) -> NamedProvidedValues<C>>()

class ProvidedValuesFactoryModule2<P, P1, P2, C> : AbstractProvidedValuesFactoryModule<P,
      (P1, P2) -> NamedProvidedValues<C>,
      (
  @Provide @AmbientService<C> P1,
  @Provide @AmbientService<C> P2
) -> NamedProvidedValues<C>>()

class ProvidedValuesFactoryModule3<P, P1, P2, P3, C> : AbstractProvidedValuesFactoryModule<P,
      (P1, P2, P3) -> NamedProvidedValues<C>,
      (
  @Provide @AmbientService<C> P1,
  @Provide @AmbientService<C> P2,
  @Provide @AmbientService<C> P3
) -> NamedProvidedValues<C>>()

class ProvidedValuesFactoryModule4<P, P1, P2, P3, P4, C> :
  AbstractProvidedValuesFactoryModule<P,
        (P1, P2, P3, P4) -> NamedProvidedValues<C>,
        (
    @Provide @AmbientService<C> P1,
    @Provide @AmbientService<C> P2,
    @Provide @AmbientService<C> P3,
    @Provide @AmbientService<C> P4
  ) -> NamedProvidedValues<C>>()

class ProvidedValuesFactoryModule5<P, P1, P2, P3, P4, P5, C> :
  AbstractProvidedValuesFactoryModule<P,
        (P1, P2, P3, P4, P5) -> NamedProvidedValues<C>,
        (
    @Provide @AmbientService<C> P1,
    @Provide @AmbientService<C> P2,
    @Provide @AmbientService<C> P3,
    @Provide @AmbientService<C> P4,
    @Provide @AmbientService<C> P5
  ) -> NamedProvidedValues<C>>()
