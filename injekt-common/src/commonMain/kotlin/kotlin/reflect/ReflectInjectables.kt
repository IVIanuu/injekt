/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlin.reflect

import com.ivianuu.injekt.*

object ReflectInjectables {
  /**
   * Provides a [KClass] of [T]
   */
  @Provide inline fun <reified T : Any> kClass(): KClass<T> = T::class

  /**
   * Provides a [KTypeT] of [T]
   */
  @OptIn(ExperimentalStdlibApi::class)
  @Provide inline fun <reified T : Any> kTypeT(): KTypeT<T> = typeOf<T>()
}

@Tag annotation class KTypeTTag<T>

typealias KTypeT<T> = @KTypeTTag<T> KType
