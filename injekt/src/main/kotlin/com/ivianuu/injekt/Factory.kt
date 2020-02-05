/*
 * Copyright 2020 Manuel Wrage
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
 * Makes the annotated class injectable and generates a factory binding for it
 * The class will be created on each request
 *
 * @see Single
 * @see Name
 * @see Scope
 * @see InjektConstructor
 * @see ModuleBuilder.factory
 */
@KindMarker(FactoryKind::class)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class Factory

object FactoryKind : Kind {
    override fun <T> wrap(
        binding: Binding<T>,
        instance: Instance<T>,
        component: Component
    ): Instance<T> = instance

    override fun toString(): String = "Factory"
}

inline fun <reified T> ModuleBuilder.factory(
    name: Any? = null,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    scoping: Scoping = Scoping.Unscoped,
    noinline definition: Definition<T>
): BindingContext<T> = factory(
    type = typeOf(),
    name = name,
    overrideStrategy = overrideStrategy,
    scoping = scoping,
    definition = definition
)

/**
 * Contributes a binding which will be instantiated on each request
 *
 * @param type the of the instance
 * @param name the name of the instance
 * @param overrideStrategy the strategy for handling overrides
 * @param scoping whether or not to create instances in the added scope
 * @param definition the definitions which creates instances
 *
 * @see ModuleBuilder.bind
 */
fun <T> ModuleBuilder.factory(
    type: Type<T>,
    name: Any? = null,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    scoping: Scoping = Scoping.Unscoped,
    definition: Definition<T>
): BindingContext<T> = bind(
    binding = DefinitionBinding(
        key = keyOf(type, name),
        kind = FactoryKind,
        overrideStrategy = overrideStrategy,
        scoping = scoping,
        definition = definition
    )
)