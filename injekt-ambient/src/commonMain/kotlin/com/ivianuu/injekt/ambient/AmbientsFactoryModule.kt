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

@Tag private annotation class AmbientsFactoryMarker

abstract class AbstractAmbientsFactoryModule<P, T, S : T> {
  @Provide fun factoryService(factory: S): @AmbientService<P> @AmbientsFactoryMarker T = factory
}

class AmbientsFactoryModule0<P, C> :
  AbstractAmbientsFactoryModule<P, () -> AmbientsFactory<C>, () -> AmbientsFactory<C>>()

class AmbientsFactoryModule1<P, P1, C> : AbstractAmbientsFactoryModule<P,
      (P1) -> AmbientsFactory<C>,
      (@Provide @AmbientService<C> P1) -> AmbientsFactory<C>>()

class AmbientsFactoryModule2<P, P1, P2, C> : AbstractAmbientsFactoryModule<P,
      (P1, P2) -> AmbientsFactory<C>,
      (
  @Provide @AmbientService<C> P1,
  @Provide @AmbientService<C> P2
) -> AmbientsFactory<C>>()

class AmbientsFactoryModule3<P, P1, P2, P3, C> : AbstractAmbientsFactoryModule<P,
      (P1, P2, P3) -> AmbientsFactory<C>,
      (
  @Provide @AmbientService<C> P1,
  @Provide @AmbientService<C> P2,
  @Provide @AmbientService<C> P3
) -> AmbientsFactory<C>>()

class AmbientsFactoryModule4<P, P1, P2, P3, P4, C> :
  AbstractAmbientsFactoryModule<P,
        (P1, P2, P3, P4) -> AmbientsFactory<C>,
        (
    @Provide @AmbientService<C> P1,
    @Provide @AmbientService<C> P2,
    @Provide @AmbientService<C> P3,
    @Provide @AmbientService<C> P4
  ) -> AmbientsFactory<C>>()

class AmbientsFactoryModule5<P, P1, P2, P3, P4, P5, C> :
  AbstractAmbientsFactoryModule<P,
        (P1, P2, P3, P4, P5) -> AmbientsFactory<C>,
        (
    @Provide @AmbientService<C> P1,
    @Provide @AmbientService<C> P2,
    @Provide @AmbientService<C> P3,
    @Provide @AmbientService<C> P4,
    @Provide @AmbientService<C> P5
  ) -> AmbientsFactory<C>>()

fun <N> ambientsFromFactoryOf(
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@AmbientsFactoryMarker () -> AmbientsFactory<N>>
): Ambients = current(ambient = ambient)
  .invoke()
  .create()

fun <N, P1> ambientsFromFactoryOf(
  p1: P1,
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@AmbientsFactoryMarker (P1) -> AmbientsFactory<N>>
): Ambients = current(ambient = ambient)
  .invoke(p1)
  .create()

fun <N, P1, P2> ambientsFromFactoryOf(
  p1: P1,
  p2: P2,
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@AmbientsFactoryMarker (P1, P2) -> AmbientsFactory<N>>
): Ambients = current(ambient = ambient)
  .invoke(p1, p2)
  .create()

fun <N, P1, P2, P3> ambientsFromFactoryOf(
  p1: P1,
  p2: P2,
  p3: P3,
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@AmbientsFactoryMarker (P1, P2, P3) -> AmbientsFactory<N>>
): Ambients = current(ambient = ambient)
  .invoke(p1, p2, p3)
  .create()

fun <N, P1, P2, P3, P4> ambientsFromFactoryOf(
  p1: P1,
  p2: P2,
  p3: P3,
  p4: P4,
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@AmbientsFactoryMarker (P1, P2, P3, P4) -> AmbientsFactory<N>>
): Ambients = current(ambient = ambient)
  .invoke(p1, p2, p3, p4)
  .create()

fun <N, P1, P2, P3, P4, P5> ambientsFromFactoryOf(
  p1: P1,
  p2: P2,
  p3: P3,
  p4: P4,
  p5: P5,
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@AmbientsFactoryMarker (P1, P2, P3, P4, P5) -> AmbientsFactory<N>>
): Ambients = current(ambient = ambient)
  .invoke(p1, p2, p3, p4, p5)
  .create()
