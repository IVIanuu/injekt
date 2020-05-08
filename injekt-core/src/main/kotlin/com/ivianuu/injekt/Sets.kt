package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

@InjektDslMarker
interface SetDsl<E> {
    fun <T : E> add()
    // todo fun <T : E> add(definition: ProviderDefinition<T>)
}

@Module
fun <E> set(block: SetDsl<E>.() -> Unit = {}): Unit = injektIntrinsic()
