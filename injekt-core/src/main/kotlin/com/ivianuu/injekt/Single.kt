package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

@Module
inline fun <reified T> single(
    vararg qualifiers: Qualifier,
    noinline definition: ProviderDsl.() -> T
): Unit = stub()
