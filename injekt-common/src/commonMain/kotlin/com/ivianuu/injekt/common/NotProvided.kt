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

import com.ivianuu.injekt.*

class NotProvided<out T> private constructor() {
  abstract class LowPriorityModule internal constructor() {
    private val defaultInstance: NotProvided<Nothing> = NotProvided()

    @Provide fun <T> default(): NotProvided<T> = defaultInstance
  }

  @Suppress("UNUSED_PARAMETER")
  companion object : LowPriorityModule() {
    @Provide fun <T> amb1(value: T): NotProvided<T> = throw AssertionError()

    @Provide fun <T> amb2(value: T): NotProvided<T> = throw AssertionError()
  }
}
