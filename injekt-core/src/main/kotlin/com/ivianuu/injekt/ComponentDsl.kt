package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

@Module
inline fun <reified T> scope(): Unit = stub()

@Module
inline fun parent(key: String, component: Component): Unit = stub()
