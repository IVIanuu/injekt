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
inline class Ambients(val map: Map<Ambient<*>, Any?>)

operator fun <T> Ambients.getOrNull(ambient: Ambient<T>): T? =
  map[ambient] as? T

fun <T> Ambients.getOrElse(ambient: Ambient<T>, defaultValue: () -> T): T =
  map[ambient] as? T ?: defaultValue()

operator fun <T> Ambients.get(ambient: Ambient<T>): T =
  map[ambient] as? T ?: ambient.default()

operator fun Ambients.plus(vararg values: ProvidedValue<*>): Ambients {
  val newMap = map.toMutableMap()

  for (providedValue in values) {
    val oldValue = newMap[providedValue.ambient]
    if (oldValue == null || providedValue.canOverride)
      newMap[providedValue.ambient] = (providedValue.ambient as Ambient<Any?>)
        .merge(oldValue, providedValue.value)
  }

  return Ambients(newMap)
}

operator fun Ambients.plus(values: Iterable<ProvidedValue<*>>): Ambients =
  plus(*values.toList().toTypedArray())

operator fun Ambients.plus(values: NamedProvidedValues<*>): Ambients =
  values.createAmbients()

@Provide fun ambientsOf(): Ambients = Ambients(emptyMap())

@OptIn(ExperimentalStdlibApi::class)
fun ambientsOf(value: ProvidedValue<*>): Ambients =
  Ambients(HashMap<Ambient<*>, Any?>(1).also {
    it[value.ambient] = value.value
  })

@OptIn(ExperimentalStdlibApi::class)
fun ambientsOf(values: Iterable<ProvidedValue<*>>): Ambients =
  ambientsOf(*values.toList().toTypedArray())

@OptIn(ExperimentalStdlibApi::class)
fun ambientsOf(vararg values: ProvidedValue<*>): Ambients {
  val map: MutableMap<Ambient<*>, Any?> = HashMap(values.size)

  for (providedValue in values)
    map[providedValue.ambient] = providedValue.value

  return Ambients(map)
}

@OptIn(ExperimentalStdlibApi::class)
fun <N> ambientsOf(@Inject values: NamedProvidedValues<N>, @Inject ambients: Ambients): Ambients =
  values.createAmbients()

class ProvidedValue<T> internal constructor(
  val ambient: Ambient<T>,
  val value: T,
  val canOverride: Boolean
)

typealias NamedProvidedValue<N, T> = ProvidedValue<T>

@Provide fun <@Spread T : NamedProvidedValue<N, S>, S, N> unwrappedNamedProvidedValue(
  providedValue: T
): S = providedValue.value

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@Provide
class NamedProvidedValues<N>(
  valueFactories: (
    @Provide NamedScope<N>,
    @Provide Ambients
  ) -> Set<NamedProvidedValue<N, *>> = { _, _ -> emptySet() }
) {
  // todo move to constructor once fixed
  private val valueFactories = valueFactories

  fun createAmbients(@Inject ambients: Ambients): Ambients {
    val parent = AmbientScope.current()
    @Provide val scope = DisposableScope()
    val parentDisposable = scope.disposeWith(parent)
    parentDisposable.disposeWith(scope)
    val values = valueFactories(scope)
    return ambients + (AmbientScope provides scope) + values
  }
}

interface Ambient<T> {
  fun default(): T

  fun merge(oldValue: T?, newValue: T): T
}

interface ProvidableAmbient<T> : Ambient<T> {
  infix fun provides(value: T) = ProvidedValue(this, value, true)

  infix fun providesDefault(value: T) = ProvidedValue(this, value, false)
}

private class ProvidableAmbientImpl<T>(
  private val merge: (T?, T) -> T,
  private val defaultFactory: () -> T
) : ProvidableAmbient<T> {
  override fun default(): T = defaultFactory.invoke()

  override fun merge(oldValue: T?, newValue: T): T = merge.invoke(oldValue, newValue)
}

fun <T> ambientOf(
  merge: (T?, T) -> T = { _, newValue -> newValue },
  defaultFactory: () -> T
): ProvidableAmbient<T> = ProvidableAmbientImpl(merge, defaultFactory)

inline fun <T> Ambient<T>.current(@Inject ambients: Ambients): T = ambients[this]
