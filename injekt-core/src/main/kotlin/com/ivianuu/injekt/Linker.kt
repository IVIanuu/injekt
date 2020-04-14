package com.ivianuu.injekt

/**
 * Provides dependencies for [BindingProvider]s
 */
class Linker(
    val bindings: Map<Key<*>, Binding<*>>,
    val parents: List<Linker>,
    private val scopes: Set<Scope>,
    private val jitFactories: List<(Linker, Key<Any?>) -> BindingProvider<Any?>?>
) {

    /**
     * Returns the [BindingProvider] for [key]
     */
    @KeyOverload
    fun <T> get(key: Key<T>): BindingProvider<T> {
        findBindingProvider(key)?.let { return it }
        if (key.isNullable) return NullBindingProvider as BindingProvider<T>
        error("Couldn't get instance for $key")
    }

    fun getLinker(scope: Scope): Linker =
        findLinker(scope) ?: error("Couldn't find linker for scope $scope")

    private fun findLinker(scope: Scope): Linker? {
        if (scope in scopes) return this

        for (index in parents.indices) {
            parents[index].findLinker(scope)?.let { return it }
        }

        return null
    }

    private fun <T> findBindingProvider(key: Key<T>): BindingProvider<T>? {
        (bindings[key] as? Binding<T>)
            ?.takeUnless { !key.isNullable && it.key.isNullable }
            ?.provider
            ?.let { return it }

        for (index in parents.lastIndex downTo 0) {
            parents[index].findBindingProvider(key)?.let { return it }
        }

        for (index in jitFactories.lastIndex downTo 0) {
            (jitFactories[index](this, key as Key<Any?>) as? BindingProvider<T>)
                ?.let { return it }
        }

        return null
    }

    private object NullBindingProvider : BindingProvider<Any?> {
        override fun invoke(parameters: Parameters): Any? = null
    }

}
