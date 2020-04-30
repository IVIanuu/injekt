package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

@InjektDslMarker
interface SetDsl<E> {
    fun <T : E> add(): Unit = injektIntrinsic()
}

@Declaration
fun <E> set(block: SetDsl<E>.() -> Unit = {}): Unit = injektIntrinsic()
