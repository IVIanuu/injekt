/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt

/**
 * Builder for [Component]s
 */
class ComponentBuilder {

    private val dependencies = linkedSetOf<Component>()
    private val bindings = linkedMapOf<Key, Binding<*>>()
    private val instances = hashMapOf<Key, Instance<*>>()

    /**
     * Adds all binding of the [module]
     */
    fun addModule(module: Module) {
        module.bindings.forEach { addBinding(it.value) }
    }

    /**
     * Adds the [dependency] as a dependency
     */
    fun addDependency(dependency: Component) {
        if (!dependencies.add(dependency)) {
            error("Already added $dependency")
        }
    }

    /**
     * Saves the [binding]
     */
    fun addBinding(binding: Binding<*>) {
        val isOverride = bindings.remove(binding.key) != null

        if (isOverride && !binding.override) {
            throw OverrideException("Try to override binding $binding but was already declared ${binding.key}")
        }

        bindings[binding.key] = binding

        instances[binding.key] = when (binding.kind) {
            Binding.Kind.FACTORY -> FactoryInstance(binding)
            Binding.Kind.SINGLE -> SingleInstance(binding)
        }
    }

    /**
     * Builds a [Component] for this builder
     */
    fun build(): Component {
        return Component(dependencies, bindings, instances)
    }

}

/**
 * Adds all [modules]
 */
fun ComponentBuilder.modules(modules: Iterable<Module>) {
    modules.forEach { addModule(it) }
}

/**
 * Adds all [modules]
 */
fun ComponentBuilder.modules(vararg modules: Module) {
    modules.forEach { addModule(it) }
}

/**
 * Adds the [module]
 */
fun ComponentBuilder.modules(module: Module) {
    addModule(module)
}

/**
 * Adds all [dependencies]
 */
fun ComponentBuilder.dependencies(dependencies: Iterable<Component>) {
    dependencies.forEach { addDependency(it) }
}

/**
 * Adds all [dependencies]
 */
fun ComponentBuilder.dependencies(vararg dependencies: Component) {
    dependencies.forEach { addDependency(it) }
}

/**
 * Adds the [dependency]
 */
fun ComponentBuilder.dependencies(dependency: Component) {
    addDependency(dependency)
}