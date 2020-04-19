package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

@Module
inline fun <reified T> single(noinline definition: ProviderDsl.() -> T): Unit = stub()
