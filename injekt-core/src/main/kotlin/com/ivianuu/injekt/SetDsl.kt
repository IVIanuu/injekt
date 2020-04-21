package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

class SetDsl<E> {
    inline fun <reified T : E> add(): Unit = stub()
}

@Module
inline fun <reified E> set(
    vararg qualifiers: Qualifier,
    block: SetDsl<E>.() -> Unit = {}
): Unit = stub()
