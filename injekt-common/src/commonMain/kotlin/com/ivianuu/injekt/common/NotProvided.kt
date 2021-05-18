package com.ivianuu.injekt.common

import com.ivianuu.injekt.*

class NotProvided<out T> private constructor() {
  abstract class LowPriorityProviders internal constructor() {
    private val defaultInstance: NotProvided<Nothing> = NotProvided()

    @Provide fun <T> default(): NotProvided<T> = defaultInstance
  }

  companion object : LowPriorityProviders() {
    @Provide fun <T> amb1(value: T): NotProvided<T> = throw AssertionError()

    @Provide fun <T> amb2(value: T): NotProvided<T> = throw AssertionError()
  }
}
