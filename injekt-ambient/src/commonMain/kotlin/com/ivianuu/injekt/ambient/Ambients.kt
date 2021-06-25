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

@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")

package com.ivianuu.injekt.ambient

import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class Ambients(val map: Map<Ambient<*>, () -> Any?>)

operator fun Ambients.plus(vararg values: ProvidedValue<*>): Ambients {
  val newMap = map.toMutableMap()

  for (providedValue in values) {
    val oldFactory = newMap[providedValue.ambient]
    newMap[providedValue.ambient] = when {
      oldFactory == null -> providedValue.factory
      providedValue.canOverride -> {
        {
          (providedValue.ambient as Ambient<Any?>)
            .merge(oldFactory.invoke(), providedValue.factory)
        }
      }
      else -> continue
    }
  }

  return Ambients(newMap)
}

operator fun Ambients.plus(values: Iterable<ProvidedValue<*>>): Ambients =
  plus(*values.toList().toTypedArray())

operator fun Ambients.plus(values: AmbientsFactory<*>): Ambients =
  values.create()

operator fun Ambients.minus(ambient: Ambient<*>): Ambients =
  if (ambient !in map) this
  else Ambients(map.toMutableMap().also { it.remove(ambient) })

operator fun Ambients.minus(vararg ambients: Ambient<*>): Ambients =
  Ambients(map.filterKeys { it !in ambients })

fun ambientsOf(): Ambients = Ambients(emptyMap())

@OptIn(ExperimentalStdlibApi::class)
fun ambientsOf(value: ProvidedValue<*>): Ambients =
  Ambients(HashMap<Ambient<*>, () -> Any?>(1).also {
    it[value.ambient] = value.factory
  })

@OptIn(ExperimentalStdlibApi::class)
fun ambientsOf(values: Iterable<ProvidedValue<*>>): Ambients =
  ambientsOf(*values.toList().toTypedArray())

@OptIn(ExperimentalStdlibApi::class)
fun ambientsOf(vararg values: ProvidedValue<*>): Ambients {
  val map: MutableMap<Ambient<*>, () -> Any?> = HashMap(values.size)

  for (providedValue in values)
    map[providedValue.ambient] = providedValue.factory

  return Ambients(map)
}

@OptIn(ExperimentalStdlibApi::class)
fun <N> ambientsOf(
  @Inject ambients: Ambients,
  @Inject values: AmbientsFactory<N>
): Ambients = values.create()

class ProvidedValue<T> internal constructor(
  val ambient: Ambient<T>,
  val factory: () -> T,
  val canOverride: Boolean
)

typealias NamedProvidedValue<N, T> = ProvidedValue<T>

@Provide fun <@Spread T : NamedProvidedValue<N, S>, S, N> unwrappedNamedProvidedValue(
  providedValue: T
): S = providedValue.factory()

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@Provide
class AmbientsFactory<N>(
  valueFactories: (@Provide NamedScope<N>) -> Set<NamedProvidedValue<N, *>> = { emptySet() },
  scopeObservers: (@Provide NamedScope<N>) -> Set<ScopeObserver<N>> = { emptySet() }
) {
  // todo move to constructor once fixed
  private val valueFactories = valueFactories
  private val scopeObservers = scopeObservers

  fun create(@Inject ambients: Ambients): Ambients {
    val parent = AmbientScope.current()
    @Provide val scope = DisposableScope()
    val parentDisposable = scope.bind(parent)
    parentDisposable.bind()
    val values = valueFactories(scope)

    val finalObservers = scopeObservers(scope)

    for (scopeObserver in finalObservers)
      invokeOnDispose { scopeObserver.onDispose() }

    for (scopeObserver in finalObservers)
      scopeObserver.onInit()

    return ambients + (AmbientScope provides scope) + values
  }
}

interface Ambient<T> {
  fun default(): T

  fun merge(oldValue: T?, newValue: T): T = newValue
}

fun <T> Ambient<T>.current(@Inject ambients: Ambients): T =
  ambients.map[this]?.invoke() as? T ?: default()

interface ProvidableAmbient<T> : Ambient<T> {
  infix fun provides(factory: () -> T) = ProvidedValue(this, factory, true)

  infix fun provides(value: T) = ProvidedValue(this, { value }, true)

  infix fun providesDefault(factory: () -> T) = ProvidedValue(this, factory, false)

  infix fun providesDefault(value: T) = ProvidedValue(this, { value }, false)
}

private class ProvidableAmbientImpl<T>(
  private val merge: ((T?, T) -> T)?,
  private val defaultFactory: () -> T
) : ProvidableAmbient<T> {
  override fun default(): T = defaultFactory.invoke()

  override fun merge(oldValue: T?, newValue: T): T = merge?.invoke(oldValue, newValue)
    ?: super.merge(oldValue, newValue)
}

fun <T> ambientOf(
  merge: ((T?, T) -> T)? = null,
  defaultFactory: () -> T
): ProvidableAmbient<T> = ProvidableAmbientImpl(merge, defaultFactory)

inline fun <R> withAmbients(
  vararg values: ProvidedValue<*>,
  @Inject ambients: Ambients,
  block: (@Provide Ambients) -> R
): R = block(ambients.plus(*values))
