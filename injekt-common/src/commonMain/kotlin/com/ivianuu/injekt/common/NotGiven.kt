package com.ivianuu.injekt.common

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Qualifier

@Qualifier
annotation class NotGiven<T : Any>

@Given
class NotGivenModule<@Given T : @NotGiven<N> S, N : Any, S> {
    @Qualifier
    private annotation class Amb

    @Suppress("NOTHING_TO_INLINE")
    @Given
    inline fun instance(
        @Suppress("UNUSED_PARAMETER") @Given amb: @Amb Unit,
        @Given instance: T
    ): S = instance

    @Given
    fun amb1(): @Amb Unit = Unit

    @Given
    fun <@Given K : N> amb2(): @Amb Unit = Unit
}
