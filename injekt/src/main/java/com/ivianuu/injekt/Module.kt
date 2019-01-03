package com.ivianuu.injekt

/**
 * A module is the container for definitions
 */
class Module internal constructor(
    val name: String? = null,
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

typealias ModuleDefinition = ModuleContext.() -> Unit