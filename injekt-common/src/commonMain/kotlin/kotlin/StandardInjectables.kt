/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlin

import com.ivianuu.injekt.*

object StandardInjectables {
  /**
   * Provides a [Lazy] of [T]
   */
  @Provide inline fun <T> lazy(noinline init: () -> T): Lazy<T> = kotlin.lazy(init)
}
