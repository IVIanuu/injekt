package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

@Module
inline fun <reified T> scope(): Unit = stub()

@Module
inline fun parent(key: String, component: Component): Unit = stub()

@Module
inline fun <reified T> factory(noinline definition: ProviderDsl.() -> T): Unit = stub()

@Module
inline fun <reified T> single(noinline definition: ProviderDsl.() -> T): Unit = stub()

// todo
@Module
inline fun <reified T> instance(instance: T): Unit = stub()

// todo
@Module
inline fun <reified T : S, reified S> alias(): Unit = stub()
