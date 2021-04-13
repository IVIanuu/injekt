package com.ivianuu.injekt.common

import com.ivianuu.injekt.DefaultOnAllErrors
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier

@Qualifier
annotation class WithFallback<T> {
    companion object {
        @Given
        inline fun <T, F : T> withFallback(
            @Given actual: @DefaultOnAllErrors () -> T?,
            @Given fallback: () -> F
        ): @WithFallback<F> T = actual() ?: fallback() as T
    }
}
