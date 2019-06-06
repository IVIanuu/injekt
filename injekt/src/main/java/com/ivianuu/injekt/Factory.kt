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

inline fun <reified T> ModuleBuilder.factory(
    name: Any? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = factory(typeOf(), name, override, definition)

fun <T> ModuleBuilder.factory(
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false,
    definition: Definition<T>
): BindingContext<T> = bind(
    DefinitionBinding(definition), type, name, override
)

inline fun <reified T> ModuleBuilder.stateFactory(
    name: Any? = null,
    override: Boolean = false,
    noinline block: StatefulDefinitionBuilder<T>.() -> Unit
): BindingContext<T> = stateFactory(typeOf(), name, override, block)

fun <T> ModuleBuilder.stateFactory(
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false,
    block: StatefulDefinitionBuilder<T>.() -> Unit
): BindingContext<T> = bind(StatefulDefinitionBinding(block), type, name, override)

@Target(AnnotationTarget.CLASS)
annotation class Factory