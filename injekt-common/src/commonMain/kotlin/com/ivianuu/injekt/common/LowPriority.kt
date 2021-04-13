package com.ivianuu.injekt.common

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier

@Qualifier
annotation class LowPriority {
    companion object {
        @Suppress("UNCHECKED_CAST")
        @Given
        inline fun <@Given T : @LowPriority S, S, U : S> lowPriorityValue(
            @Given value: T
        ): U = value as U
    }
}
