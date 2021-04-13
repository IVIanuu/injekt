package com.ivianuu.injekt.common

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier

@Qualifier
annotation class NotGiven<T : Any> {
    companion object {
        @Given
        class NotGivenModule<@Given T : @NotGiven<N> S, N : Any, S> {
            @Qualifier
            private annotation class Amb

            @Given
            fun value(
                @Suppress("UNUSED_PARAMETER") @Given amb: @Amb Unit,
                @Given value: T
            ): S = value

            @Given
            fun amb1(): @Amb Unit = Unit

            @Given
            fun <@Given K : N> amb2(): @Amb Unit = Unit
        }
    }
}