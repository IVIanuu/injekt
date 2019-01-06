package com.ivianuu.injekt

/**
 * A module is the container for definitions
 */
class Module internal constructor(
    val name: String?,
    val scopeId: String?,
    val createOnStart: Boolean,
    val override: Boolean,
    val definition: ModuleDefinition
) {

    fun getDefinitions(): List<BeanDefinition<*>> {
        val moduleContext = ModuleContext(this)
        definition.invoke(moduleContext)
        return moduleContext.definitions
    }

}

operator fun Module.plus(module: Module) = listOf(this, module)
operator fun Module.plus(modules: Iterable<Module>) = listOf(this) + modules
operator fun Module.plus(modules: Array<Module>) = listOf(this) + modules

/**
 * Defines module entries
 */
typealias ModuleDefinition = ModuleContext.() -> Unit