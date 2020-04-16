package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

interface Component {
    fun <T> get(key: String): T = stub()
}

fun Component(key: String, block: @Module ComponentDsl.() -> Unit = {}): Component = stub()

inline fun <reified T> Component.get(qualifier: Qualifier? = null): T =
    get(keyOf<T>(qualifier).toString())

@InjektDslMarker
class ComponentDsl {

    inline fun <reified T> scope(scope: T): Unit = stub()

    inline fun <reified T : Component> parent(key: String, component: T): Unit = stub()

    inline fun <reified T> factory(
        qualifier: Qualifier? = null,
        noinline definition: ProviderDsl.() -> T
    ): Unit = stub()

    inline fun <reified T> instance(instance: T, qualifier: Qualifier? = null): Unit = stub()

    inline fun <reified T : S, reified S> alias(
        originalQualifier: Qualifier? = null,
        aliasQualifier: Qualifier? = null
    ): Unit = stub()
}
