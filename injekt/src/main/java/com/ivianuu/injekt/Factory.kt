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

// todo return types

inline fun <reified T> ModuleBuilder.factory(
    name: Qualifier? = null,
    override: Boolean = false,
    binding: Binding<T>
) = bind(keyOf(typeOf<T>(), name), binding, override)

inline fun <reified T> ModuleBuilder.factory(
    name: Qualifier? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
) = factory(typeOf(), name, override, definition)

fun <T> ModuleBuilder.factory(
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false,
    definition: Definition<T>
) = bind(keyOf(type, name), DefinitionBinding(definition), override)

inline fun <reified T> ModuleBuilder.factoryState(
    name: Qualifier? = null,
    override: Boolean = false,
    noinline block: StatefulDefinitionBuilder<T>.() -> Unit
) = factoryState(typeOf(), name, override, block)

fun <T> ModuleBuilder.factoryState(
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false,
    block: StatefulDefinitionBuilder<T>.() -> Unit
) = bind(
    keyOf(type, name),
    StatefulDefinitionBinding(StatefulDefinitionBuilder<T>().apply(block)),
    override
)

@Target(AnnotationTarget.CLASS)
annotation class Factory