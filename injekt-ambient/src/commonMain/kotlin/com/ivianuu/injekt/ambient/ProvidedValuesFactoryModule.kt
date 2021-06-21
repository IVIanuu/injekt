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
import com.ivianuu.injekt.common.*

@Tag private annotation class ProvidedValuesFactory

abstract class AbstractProvidedValuesFactoryModule<P, T, S : T> {
  @Provide fun factoryService(factory: S): @AmbientService<P> @ProvidedValuesFactory T = factory
}

class ProvidedValuesFactoryModule0<P, C> :
  AbstractProvidedValuesFactoryModule<P, () -> ProvidedValues<C>, () -> ProvidedValues<C>>()

class ProvidedValuesFactoryModule1<P, P1, C> : AbstractProvidedValuesFactoryModule<P,
      (P1) -> ProvidedValues<C>,
      (@Provide @AmbientService<C> P1) -> ProvidedValues<C>>()

class ProvidedValuesFactoryModule2<P, P1, P2, C> : AbstractProvidedValuesFactoryModule<P,
      (P1, P2) -> ProvidedValues<C>,
      (
  @Provide @AmbientService<C> P1,
  @Provide @AmbientService<C> P2
) -> ProvidedValues<C>>()

class ProvidedValuesFactoryModule3<P, P1, P2, P3, C> : AbstractProvidedValuesFactoryModule<P,
      (P1, P2, P3) -> ProvidedValues<C>,
      (
  @Provide @AmbientService<C> P1,
  @Provide @AmbientService<C> P2,
  @Provide @AmbientService<C> P3
) -> ProvidedValues<C>>()

class ProvidedValuesFactoryModule4<P, P1, P2, P3, P4, C> :
  AbstractProvidedValuesFactoryModule<P,
        (P1, P2, P3, P4) -> ProvidedValues<C>,
        (
    @Provide @AmbientService<C> P1,
    @Provide @AmbientService<C> P2,
    @Provide @AmbientService<C> P3,
    @Provide @AmbientService<C> P4
  ) -> ProvidedValues<C>>()

class ProvidedValuesFactoryModule5<P, P1, P2, P3, P4, P5, C> :
  AbstractProvidedValuesFactoryModule<P,
        (P1, P2, P3, P4, P5) -> ProvidedValues<C>,
        (
    @Provide @AmbientService<C> P1,
    @Provide @AmbientService<C> P2,
    @Provide @AmbientService<C> P3,
    @Provide @AmbientService<C> P4,
    @Provide @AmbientService<C> P5
  ) -> ProvidedValues<C>>()

fun <N> createAmbientsFromProvidedValues(
  @Inject ambients: Ambients,
  @Inject nameKey: TypeKey<N>
): Ambients = AmbientService.current<@ProvidedValuesFactory () -> ProvidedValues<N>>()
  .invoke()
  .createAmbients()

fun <N, P1> createAmbientsFromProvidedValues(
  p1: P1,
  @Inject ambients: Ambients,
  @Inject nameKey: TypeKey<N>,
  @Inject p1Key: TypeKey<P1>
): Ambients = AmbientService.current<@ProvidedValuesFactory (P1) -> ProvidedValues<N>>()
  .invoke(p1)
  .createAmbients()

fun <N, P1, P2> createAmbientsFromProvidedValues(
  p1: P1,
  p2: P2,
  @Inject ambients: Ambients,
  @Inject nameKey: TypeKey<N>,
  @Inject p1Key: TypeKey<P1>,
  @Inject p2Key: TypeKey<P2>
): Ambients = AmbientService.current<@ProvidedValuesFactory (P1, P2) -> ProvidedValues<N>>()
  .invoke(p1, p2)
  .createAmbients()

fun <N, P1, P2, P3> createAmbientsFromProvidedValues(
  p1: P1,
  p2: P2,
  p3: P3,
  @Inject ambients: Ambients,
  @Inject nameKey: TypeKey<N>,
  @Inject p1Key: TypeKey<P1>,
  @Inject p2Key: TypeKey<P2>,
  @Inject p3Key: TypeKey<P3>
): Ambients = AmbientService.current<@ProvidedValuesFactory (P1, P2, P3) -> ProvidedValues<N>>()
  .invoke(p1, p2, p3)
  .createAmbients()

fun <N, P1, P2, P3, P4> createAmbientsFromProvidedValues(
  p1: P1,
  p2: P2,
  p3: P3,
  p4: P4,
  @Inject ambients: Ambients,
  @Inject nameKey: TypeKey<N>,
  @Inject p1Key: TypeKey<P1>,
  @Inject p2Key: TypeKey<P2>,
  @Inject p3Key: TypeKey<P3>,
  @Inject p4Key: TypeKey<P4>
): Ambients = AmbientService.current<@ProvidedValuesFactory (P1, P2, P3, P4) -> ProvidedValues<N>>()
  .invoke(p1, p2, p3, p4)
  .createAmbients()

fun <N, P1, P2, P3, P4, P5> createAmbientsFromProvidedValues(
  p1: P1,
  p2: P2,
  p3: P3,
  p4: P4,
  p5: P5,
  @Inject ambients: Ambients,
  @Inject nameKey: TypeKey<N>,
  @Inject p1Key: TypeKey<P1>,
  @Inject p2Key: TypeKey<P2>,
  @Inject p3Key: TypeKey<P3>,
  @Inject p4Key: TypeKey<P4>,
  @Inject p5Key: TypeKey<P5>
): Ambients = AmbientService.current<@ProvidedValuesFactory (P1, P2, P3, P4, P5) -> ProvidedValues<N>>()
  .invoke(p1, p2, p3, p4, p5)
  .createAmbients()
