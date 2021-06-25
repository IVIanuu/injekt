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

class Ambients(val map: Map<Ambient<*>, () -> Any?>)

operator fun Ambients.plus(vararg values: ProvidedValue<*>): Ambients {
  val newMap = map.toMutableMap()

  for (providedValue in values) {
    val oldFactory = newMap[providedValue.ambient]
    newMap[providedValue.ambient] = when {
      oldFactory != null && !providedValue.canOverride -> continue
      oldFactory == null -> providedValue.factory
      providedValue.canOverride -> {
        {
          (providedValue.ambient as Ambient<Any?>)
            .merge(oldFactory.invoke(), providedValue.factory())
        }
      }
      else -> continue
    }
  }

  return Ambients(newMap)
}

operator fun Ambients.plus(values: Iterable<ProvidedValue<*>>): Ambients =
  plus(*values.toList().toTypedArray())

operator fun <T> Ambients.minus(@Inject ambient: Ambient<T>): Ambients =
  if (ambient !in map) this
  else Ambients(map.toMutableMap().also { it.remove(ambient) })

fun ambientsOf(): Ambients = Ambients(emptyMap())

@OptIn(ExperimentalStdlibApi::class)
fun ambientsOf(vararg values: ProvidedValue<*>): Ambients {
  val map: MutableMap<Ambient<*>, () -> Any?> = HashMap(values.size)

  for (providedValue in values)
    map[providedValue.ambient] = providedValue.factory

  return Ambients(map)
}

inline fun <R> withAmbients(
  vararg values: ProvidedValue<*>,
  @Inject ambients: Ambients,
  block: (@Provide Ambients) -> R
): R = block(ambients.plus(*values))

interface Ambient<T> {
  fun default(): T

  fun merge(oldValue: T?, newValue: T): T = newValue
}

fun <T> current(@Inject ambients: Ambients, @Inject ambient: Ambient<T>): T =
  ambients.map[ambient]?.invoke() as? T ?: ambient.default()

interface ProvidableAmbient<T> : Ambient<T>

class ProvidedValue<T> internal constructor(
  val ambient: Ambient<T>,
  val factory: () -> T,
  val canOverride: Boolean
)

infix fun <T> provide(
  @Inject ambient: ProvidableAmbient<T>,
  factory: () -> T
) = ProvidedValue(ambient, factory, true)

infix fun <T> provide(
  value: T,
  @Inject ambient: ProvidableAmbient<T>
) = provide { value }

infix fun <T> provideDefault(
  @Inject ambient: ProvidableAmbient<T>,
  factory: () -> T
) = ProvidedValue(ambient, factory, false)

infix fun <T> provideDefault(
  value: T,
  @Inject ambient: ProvidableAmbient<T>
) = provideDefault { value }

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
