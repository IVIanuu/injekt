package com.ivianuu.injekt

/**
 * @author Manuel Wrage (IVIanuu)
 */
interface ComponentExtension {

    fun onComponentInitialized(component: Component) {
    }

    fun onDependencyAdded(component: Component, dependency: Component) {
    }

    fun onScopeAdded(component: Component, scope: Scope) {
    }

    fun onModuleAdded(component: Component, module: Module) {
    }

    fun onBindingAdded(component: Component, binding: Binding<*>) {
    }

    fun onInstanceAdded(component: Component, instance: Instance<*>) {
    }

    fun onResolveBinding(component: Component, key: Key): Binding<*>? = null

}