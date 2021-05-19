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
