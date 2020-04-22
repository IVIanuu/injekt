package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

@Module
inline fun <reified T : S, reified S> alias(): Unit = stub()
