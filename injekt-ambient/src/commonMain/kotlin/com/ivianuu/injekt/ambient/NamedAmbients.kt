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
import com.ivianuu.injekt.scope.*

typealias NamedAmbients<N> = Ambients

typealias NamedProvidedValue<N, T> = ProvidedValue<T>

@Provide fun <@Spread T : NamedProvidedValue<N, S>, S, N> unwrappedNamedProvidedValue(
  providedValue: T
): S = providedValue.factory()

typealias NamedScope<N> = Scope

/**
 * Listener for scope lifecycles
 */
interface NamedScopeObserver<N> {
  /**
   * Will be called when the scope gets initialized
   */
  fun onInit() {
  }

  /**
   * Will be called when the scope gets disposed
   */
  fun onDispose() {
  }
}

@Provide fun <N> namedAmbients(
  ambients: Ambients,
  valueFactories: (@Provide NamedScope<N>, @Provide Ambients) -> Set<NamedProvidedValue<N, *>> = { _, _ -> emptySet() },
  scopeObservers: (@Provide NamedScope<N>, @Provide Ambients) -> Set<NamedScopeObserver<N>> = { _, _ -> emptySet() }
): NamedAmbients<N> {
  val parent = current<Scope>()
  @Provide val scope = DisposableScope()
  val parentDisposable = scope.bind(parent)
  parentDisposable.bind()

  val finalValues = mutableMapOf<Ambient<*>, () -> Any?>()
  val finalAmbients = Ambients(finalValues)

  val values = valueFactories(scope, finalAmbients)

  val finalObservers = scopeObservers(scope, finalAmbients)

  for (scopeObserver in finalObservers)
    invokeOnDispose { scopeObserver.onDispose() }

  for (scopeObserver in finalObservers)
    scopeObserver.onInit()

  finalValues += (ambients + provide<Scope>(scope) + values).map

  return finalAmbients
}

abstract class AbstractNamedAmbientsModule<P, T, S : T> {
  @Provide fun factoryService(factory: S): @AmbientService<P> @NamedAmbientsFactory T = factory
}

class NamedAmbientsModule0<P, C> :
  AbstractNamedAmbientsModule<P, () -> NamedAmbients<C>, () -> NamedAmbients<C>>()

class NamedAmbientsModule1<P, P1, C> : AbstractNamedAmbientsModule<P,
      (P1) -> NamedAmbients<C>,
      (@Provide @AmbientService<C> P1) -> NamedAmbients<C>>()

class NamedAmbientsModule2<P, P1, P2, C> : AbstractNamedAmbientsModule<P,
      (P1, P2) -> NamedAmbients<C>,
      (
  @Provide @AmbientService<C> P1,
  @Provide @AmbientService<C> P2
) -> NamedAmbients<C>>()

class NamedAmbientsModule3<P, P1, P2, P3, C> : AbstractNamedAmbientsModule<P,
      (P1, P2, P3) -> NamedAmbients<C>,
      (
  @Provide @AmbientService<C> P1,
  @Provide @AmbientService<C> P2,
  @Provide @AmbientService<C> P3
) -> NamedAmbients<C>>()

class NamedAmbientsModule4<P, P1, P2, P3, P4, C> :
  AbstractNamedAmbientsModule<P,
        (P1, P2, P3, P4) -> NamedAmbients<C>,
        (
    @Provide @AmbientService<C> P1,
    @Provide @AmbientService<C> P2,
    @Provide @AmbientService<C> P3,
    @Provide @AmbientService<C> P4
  ) -> NamedAmbients<C>>()

class NamedAmbientsModule5<P, P1, P2, P3, P4, P5, C> :
  AbstractNamedAmbientsModule<P,
        (P1, P2, P3, P4, P5) -> NamedAmbients<C>,
        (
    @Provide @AmbientService<C> P1,
    @Provide @AmbientService<C> P2,
    @Provide @AmbientService<C> P3,
    @Provide @AmbientService<C> P4,
    @Provide @AmbientService<C> P5
  ) -> NamedAmbients<C>>()

fun <N> namedAmbientsOf(
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@NamedAmbientsFactory () -> NamedAmbients<N>>
): Ambients = current(ambient = ambient)
  .invoke()

fun <N, P1> namedAmbientsOf(
  p1: P1,
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@NamedAmbientsFactory (P1) -> NamedAmbients<N>>
): Ambients = current(ambient = ambient)
  .invoke(p1)

fun <N, P1, P2> namedAmbientsOf(
  p1: P1,
  p2: P2,
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@NamedAmbientsFactory (P1, P2) -> NamedAmbients<N>>
): Ambients = current(ambient = ambient)
  .invoke(p1, p2)

fun <N, P1, P2, P3> namedAmbientsOf(
  p1: P1,
  p2: P2,
  p3: P3,
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@NamedAmbientsFactory (P1, P2, P3) -> NamedAmbients<N>>
): Ambients = current(ambient = ambient)
  .invoke(p1, p2, p3)

fun <N, P1, P2, P3, P4> namedAmbientsOf(
  p1: P1,
  p2: P2,
  p3: P3,
  p4: P4,
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@NamedAmbientsFactory (P1, P2, P3, P4) -> NamedAmbients<N>>
): Ambients = current(ambient = ambient)
  .invoke(p1, p2, p3, p4)

fun <N, P1, P2, P3, P4, P5> namedAmbientsOf(
  p1: P1,
  p2: P2,
  p3: P3,
  p4: P4,
  p5: P5,
  @Inject ambients: Ambients,
  @Inject ambient: Ambient<@NamedAmbientsFactory (P1, P2, P3, P4, P5) -> NamedAmbients<N>>
): Ambients = current(ambient = ambient)
  .invoke(p1, p2, p3, p4, p5)

@Tag private annotation class NamedAmbientsFactory
