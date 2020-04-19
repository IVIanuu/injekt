package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

@Module
inline fun <reified T> factory(
    vararg qualifiers: Qualifier,
    noinline definition: ProviderDsl.() -> T
): Unit = stub()
