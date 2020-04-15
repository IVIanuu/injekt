package com.ivianuu.injekt

open class Module(block: ModuleDsl.() -> Unit)

class ModuleDsl {
    inline fun <reified T> factory(
        qualifier: Qualifier? = null,
        provider: ProviderDsl.() -> T
    ) {
        stub()
    }

    inline fun <reified K, reified V> map(
        mapQualifier: Qualifier? = null,
        block: MapDsl<K, V>.() -> Unit
    ) {
        stub()
    }

    inline fun <reified E> set(
        setQualifier: Qualifier? = null,
        block: SetDsl<E>.() -> Unit
    ) {
        stub()
    }
}
