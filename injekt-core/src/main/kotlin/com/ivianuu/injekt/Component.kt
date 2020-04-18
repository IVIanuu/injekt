package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

interface Component {
    fun <T> get(key: Int): T = error("Couldn't find binding for $key")
}

inline fun <reified T> Component.get(qualifier: Qualifier? = null): T = stub()

fun Component(key: String, block: @Module () -> Unit = {}): Component = stub()
