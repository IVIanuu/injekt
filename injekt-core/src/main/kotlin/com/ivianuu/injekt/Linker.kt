package com.ivianuu.injekt

class Linker(val component: Component) {

    private val requestChain = mutableSetOf<Key<*>>()

    @KeyOverload
    fun <T> get(key: Key<T>): BindingProvider<T> {
        check(key !in requestChain) {
            "Circular dependency detected $key"
        }
        requestChain += key
        val provider = component.getBindingProvider(key)
        requestChain -= key
        return provider
    }

    fun getLinker(scope: Scope): Linker = component.getComponent(scope).linker

}
