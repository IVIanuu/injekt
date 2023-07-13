/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Provide
import kotlin.reflect.KClass

@Provide object ReflectModule {
  /**
   * Provides a [KClass] of [T]
   */
  @Provide inline fun <reified T : Any> kClass(): KClass<T> = T::class
}
