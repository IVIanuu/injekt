package com.ivianuu.injekt.common

import com.ivianuu.injekt.*

class NotGiven<out T> private constructor() {
    abstract class LowPriorityNotGiven internal constructor() {
        @Given
        fun <T> default(): NotGiven<T> = value
    }
    companion object : LowPriorityNotGiven() {
        val value: NotGiven<Nothing> = NotGiven()

        @Given
        fun <T> amb1(@Given value: T): NotGiven<T> = throw AssertionError()

        @Given
        fun <T> amb2(@Given value: T): NotGiven<T> = throw AssertionError()
    }
}
