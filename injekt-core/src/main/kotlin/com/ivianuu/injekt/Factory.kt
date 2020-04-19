package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

annotation class Factory

@Module
inline fun <reified T> factory(noinline definition: ProviderDsl.() -> T): Unit = stub()