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

import kotlin.reflect.KClass

/**
 * The context for defining [BeanDefinition]s
 */
data class ModuleContext(val module: Module) {

    internal val definitions = mutableListOf<BeanDefinition<*>>()

    /**
     * Adds the [definition]
     */
    fun <T : Any> declare(
        definition: BeanDefinition<T>
    ): BindingContext<T> {
        val scopeId = module.scopeId ?: definition.scopeId
        val override = if (module.override) module.override else definition.override
        val createOnStart =
            if (module.createOnStart) module.createOnStart else definition.createOnStart

        definition.scopeId = scopeId
        definition.createOnStart = createOnStart
        definition.override = override

        definitions.add(definition)

        return BindingContext(definition, this)
    }

}

/**
 * Defines a [Module]
 */
fun module(
    name: String? = null,
    scopeId: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: ModuleDefinition
): Module = Module(name, scopeId, createOnStart, override, definition)

/**
 * Provides a unscoped dependency which will be recreated on each request
 */
inline fun <reified T : Any> ModuleContext.factory(
    name: String? = null,
    scopeId: String? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = factory(T::class, name, scopeId, override, definition)

/**
 * Provides a unscoped dependency which will be recreated on each request
 */
fun <T : Any> ModuleContext.factory(
    type: KClass<T>,
    name: String? = null,
    scopeId: String? = null,
    override: Boolean = false,
    definition: Definition<T>
): BindingContext<T> = declare(
    type = type,
    name = name,
    kind = BeanDefinition.Kind.FACTORY,
    scopeId = scopeId,
    createOnStart = false,
    override = override,
    definition = definition
)

/**
 * Provides scoped dependency which will be created once for each component
 */
inline fun <reified T : Any> ModuleContext.single(
    name: String? = null,
    scopeId: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = single(T::class, name, scopeId, override, createOnStart, definition)

/**
 * Provides scoped dependency which will be created once for each component
 */
fun <T : Any> ModuleContext.single(
    type: KClass<T>,
    name: String? = null,
    scopeId: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: Definition<T>
): BindingContext<T> = declare(
    type = type,
    name = name,
    kind = BeanDefinition.Kind.SINGLE,
    scopeId = scopeId,
    override = override,
    createOnStart = createOnStart,
    definition = definition
)

/**
 * Adds a [BeanDefinition] for the provided parameters
 */
inline fun <reified T : Any> ModuleContext.declare(
    name: String? = null,
    kind: BeanDefinition.Kind,
    scopeId: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = declare(T::class, name, kind, scopeId, override, createOnStart, definition)

/**
 * Adds a [BeanDefinition] for the provided parameters
 */
fun <T : Any> ModuleContext.declare(
    type: KClass<T>,
    name: String? = null,
    kind: BeanDefinition.Kind,
    scopeId: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: Definition<T>
): BindingContext<T> = declare(
    BeanDefinition.create(type, name, kind, definition).also {
        it.scopeId = scopeId
        it.createOnStart = createOnStart
        it.override = override
    }
)

/**
 * Adds all definitions of [module]
 */
fun ModuleContext.module(module: Module) {
    module.getDefinitions().forEach { declare(it) }
}

/**
 * Adds all definitions of module
 */
fun ModuleContext.module(
    name: String? = null,
    scopeId: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: ModuleDefinition
) {
    module(com.ivianuu.injekt.module(name, scopeId, override, createOnStart, definition))
}

/**
 * Adds a binding for [T] for a existing definition of [S]
 */
inline fun <reified T : Any, reified S : T> ModuleContext.bind(
    bindingName: String? = null,
    existingName: String? = null
): BindingContext<T> = factory(bindingName) { get<S>(existingName) }