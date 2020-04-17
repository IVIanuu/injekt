package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

interface Component {
    fun <T> get(key: String): T = stub()
}

inline fun <reified T> Component.get(qualifier: Qualifier? = null): T = stub()

fun Component(key: String, block: @Module () -> Unit = {}): Component = stub()
