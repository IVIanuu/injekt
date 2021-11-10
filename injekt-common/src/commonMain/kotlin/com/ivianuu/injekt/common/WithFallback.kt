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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Provide

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
