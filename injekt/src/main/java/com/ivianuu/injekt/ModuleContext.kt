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
 * The context while defining declarations
 */
data class ModuleContext(
    val module: Module,
    val componentContext: ComponentContext
) {

    internal val declarations = hashMapOf<Key, Declaration<*>>()

    /**
     * Adds the [declaration]
     */
    fun <T : Any> declare(
        declaration: Declaration<T>
    ): Declaration<T> {
        declaration.moduleContext = this

        val override = if (module.override) module.override else declaration.override
        val createOnStart =
            if (module.createOnStart) module.createOnStart else declaration.createOnStart

        declaration.createOnStart = createOnStart
        declaration.override = override

        val oldDeclaration = declarations[declaration.key]
        val isOverride = oldDeclaration != null
        if (isOverride && !declaration.override) {
            throw OverrideException("${module.name} Try to override declaration $declaration but was already saved $oldDeclaration")
        }

        declarations[declaration.key] = declaration

        return declaration
    }

}

/**
 * Defines a [Module]
 */
fun module(
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: ModuleDefinition
) = Module(name, createOnStart, override, definition)

/**
 * Provides a dependency
 */
inline fun <reified T : Any> ModuleContext.factory(
    name: String? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
) = factory(T::class, name, override, definition)

/**
 * Provides a dependency
 */
fun <T : Any> ModuleContext.factory(
    type: KClass<T>,
    name: String? = null,
    override: Boolean = false,
    definition: Definition<T>
) = declare(
    type = type,
    kind = Declaration.Kind.FACTORY,
    name = name,
    createOnStart = false,
    override = override,
    definition = definition
)

/**
 * Provides a singleton dependency
 */
inline fun <reified T : Any> ModuleContext.single(
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    noinline definition: Definition<T>
) = single(T::class, name, override, createOnStart, definition)

/**
 * Provides a singleton dependency
 */
fun <T : Any> ModuleContext.single(
    type: KClass<T>,
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: Definition<T>
) = declare(
    type = type,
    kind = Declaration.Kind.SINGLE,
    name = name,
    override = override,
    createOnStart = createOnStart,
    definition = definition
)

/**
 * Adds a [Declaration] for the provided params
 */
inline fun <reified T : Any> ModuleContext.declare(
    name: String? = null,
    kind: Declaration.Kind,
    override: Boolean = false,
    createOnStart: Boolean = false,
    noinline definition: Definition<T>
) = declare(T::class, name, kind, override, createOnStart, definition)

/**
 * Adds a [Declaration] for the provided params
 */
fun <T : Any> ModuleContext.declare(
    type: KClass<T>,
    name: String? = null,
    kind: Declaration.Kind,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: Definition<T>
) = declare(
    Declaration.create(type, name, kind, definition).also {
        it.createOnStart = createOnStart
        it.override = override
    }
)

/**
 * Adds all declarations of [module]
 */
fun ModuleContext.module(module: Module) {
    module.getDeclarations(componentContext).forEach { declare(it.value) }
}

/**
 * Adds all declarations of module
 */
fun ModuleContext.module(
    name: String? = null,
    override: Boolean = false,
    createOnStart: Boolean = false,
    definition: ModuleDefinition
) {
    module(com.ivianuu.injekt.module(name, override, createOnStart, definition))
}

/**
 * Adds a binding for [T] for a existing declaration of [S]
 */
inline fun <reified T : Any, reified S : T> ModuleContext.bind(name: String? = null) =
    factory<T>(name) { get<S>() }