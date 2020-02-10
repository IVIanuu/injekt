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

object EagerSingleKind : Kind {
    override fun <T> wrap(
        binding: Binding<T>,
        provider: BindingProvider<T>
    ): BindingProvider<T> = EagerSingleBindingProvider(SingleKind.wrap(binding, provider))

    override fun toString(): String = "EagerSingle"
}

inline fun <reified T> ModuleBuilder.eagerSingle(
    name: Any? = null,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    scoping: Scoping = Scoping.Scoped(),
    noinline definition: Definition<T>
): BindingContext<T> = eagerSingle(
    type = typeOf(),
    name = name,
    overrideStrategy = overrideStrategy,
    scoping = scoping,
    definition = definition
)

/**
 * Contributes a binding which will be reused throughout the lifetime of the [Component] it life's in
 *
 * @param type the of the instance
 * @param name the name of the instance
 * @param overrideStrategy the strategy for handling overrides
 * @param scoping the scoping definition for this binding
 * @param definition the definitions which creates instances
 *
 * @see ModuleBuilder.bind
 */
fun <T> ModuleBuilder.eagerSingle(
    type: Type<T>,
    name: Any? = null,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    scoping: Scoping = Scoping.Scoped(),
    definition: Definition<T>
): BindingContext<T> =
    bind(
        binding = DefinitionBinding(
            key = keyOf(type, name),
            kind = EagerSingleKind,
            scoping = scoping,
            overrideStrategy = overrideStrategy,
            definition = definition
        )
    )

private class EagerSingleBindingProvider<T>(
    private val provider: BindingProvider<T>
) : BindingProvider<T> {
    override fun resolve(component: Component, parameters: Parameters): T =
        provider.resolve(component, parameters)

    override fun onAttach(component: Component) {
        super.onAttach(component)
        resolve(component)
    }
}
