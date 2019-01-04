package com.ivianuu.injekt

/**
 * @author Manuel Wrage (IVIanuu)
 */
object GlobalModuleRegistry {

    private val modules = hashSetOf<Module>()

    fun addModules(vararg modules: Module) {
        this.modules.addAll(modules)
    }

    fun findAllMatching(component: Component): List<BeanDefinition<*>> {
        return modules
            .flatMap { it.getDefinitions() }
            .filter { it.scope == null || it.scope == component.scope }
    }

}