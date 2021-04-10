package com.ivianuu.injekt.common

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier

@Qualifier
annotation class LowPriority

@Given
class LowPriorityModule<@Given T : @LowPriority S, S> {
    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    @Given
    inline fun <U : S> impl(@Given instance: T): U = instance as U
}
