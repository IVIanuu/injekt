/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.common

import com.ivianuu.injekt.*

sealed interface WithFallback<out A, out B>  {
  data class Preferred<out A>(val value: A) : WithFallback<A, Nothing>
  data class Fallback<out B>(val value: B) : WithFallback<Nothing, B>

  sealed interface LowPriorityModule {
    @Provide fun <B> fallback(value: B) = Fallback(value)
  }

  companion object : LowPriorityModule {
    @Provide fun <A> preferred(value: A) = Preferred(value)
  }
}
