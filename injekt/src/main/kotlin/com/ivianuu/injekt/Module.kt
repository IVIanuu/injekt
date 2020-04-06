package com.ivianuu.injekt

/**
 * Runs this function on each [ComponentBuilder]
 *
 * Optionally annotate this function with a [Scope] annotation to ensure that this
 * function gets only invoked for [ComponentBuilder]s with a matching scope
 *
 * ```
 * @ActivityScope
 * @Module
 * private fun ComponentBuilder.myActivityBindings() {
 *     factory { get<MyActivity>().resources }
 * }
 * ```
 *
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Module(
    /**
     * Setting this flag to true will invoke this function before modules without this flag
     */
    val invokeOnInit: Boolean = false
) {
    // todo better name?

    interface Impl {
        val scope: Scope? get() = null
        val invokeOnInit: Boolean get() = false
        fun apply(builder: ComponentBuilder)
    }
}

internal object Modules {

    internal val modulesByScope =
        mutableMapOf<Scope?, MutableList<Module.Impl>>()

    private val listeners = mutableListOf<(Module.Impl) -> Unit>()

    fun get(scope: Scope? = null): List<Module.Impl> =
        synchronized(modulesByScope) { modulesByScope.getOrElse(scope) { emptyList() } }

    fun register(module: Module.Impl) {
        synchronized(modulesByScope) {
            modulesByScope.getOrPut(module.scope) { mutableListOf() }.run {
                this += module
                sortByDescending { it.invokeOnInit }
            }
        }
        synchronized(listeners) { listeners.toList() }
            .forEach { it(module) }
    }

    fun addRegisterListener(listener: (Module.Impl) -> Unit) {
        synchronized(listeners) { listeners += listener }
    }

    fun removeRegisterListener(listener: (Module.Impl) -> Unit) {
        synchronized(listeners) { listeners -= listener }
    }

}
