package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

interface Component {
    fun <T> get(key: String): T = stub()
}

inline fun <reified T> Component.get(qualifier: Qualifier? = null): T = stub()

fun Component(key: String, block: @Module () -> Unit = {}): Component = stub()

@Module
inline fun <reified T> scope(scope: T): Unit = stub()

@Module
inline fun parent(key: String, component: Component): Unit = stub()

@Module
inline fun <reified T> factory(
    qualifier: Qualifier? = null,
    noinline definition: ProviderDsl.() -> T
): Unit = stub()

@Module
inline fun <reified T> instance(instance: T, qualifier: Qualifier? = null): Unit = stub()

@Module
inline fun <reified T : S, reified S> alias(
    originalQualifier: Qualifier? = null,
    aliasQualifier: Qualifier? = null
): Unit = stub()
