package com.ivianuu.injekt

/**
 * Runs the [invoke] function on each [ComponentBuilder]
 *
 * Optionally annotate this function with a [Scope] annotation to ensure that this
 * function gets only invoked for [ComponentBuilder]s with a matching scope
 *
 * ```
 * @ModuleMarker
 * private val MyActivityModule = Module(ActivityScope) {
 *     factory { get<MyActivity>().resources }
 * }
 * ```
 *
 */
interface Module {
    val scopes: List<Scope> get() = emptyList()
    val invokeOnInit: Boolean get() = false
    operator fun invoke(builder: ComponentBuilder)
}

/**
 * Invokes the [apply] function on any matching [Component]
 */
@Target(AnnotationTarget.PROPERTY)
annotation class ModuleMarker

inline fun Module(
    scope: Scope,
    invokeOnInit: Boolean = false,
    crossinline block: ComponentBuilder.() -> Unit
): Module = Module(listOf(scope), invokeOnInit, block)

inline fun Module(
    scopes: List<Scope>,
    invokeOnInit: Boolean = false,
    crossinline block: ComponentBuilder.() -> Unit
): Module = object : Module {
    override val scopes: List<Scope> = scopes
    override val invokeOnInit: Boolean = invokeOnInit
    override fun invoke(builder: ComponentBuilder) {
        block(builder)
    }
}

internal object Modules {

    internal val modulesByScope =
        mutableMapOf<Scope, MutableList<Module>>()

    fun get(scope: Scope): List<Module> =
        synchronized(modulesByScope) { modulesByScope.getOrElse(scope) { emptyList() } }

    fun register(module: Module) {
        synchronized(modulesByScope) {
            module.scopes.forEach { scope ->
                modulesByScope.getOrPut(scope) { mutableListOf() }.run {
                    this += module
                    sortByDescending { it.invokeOnInit }
                }
            }
        }
    }

}
