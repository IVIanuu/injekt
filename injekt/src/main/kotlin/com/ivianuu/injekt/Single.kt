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
 * Makes the annotated class injectable and generates a single binding for it
 * The class will be created once per [Component]
 *
 * @see Factory
 * @see Name
 * @see Scope
 * @see InjektConstructor
 * @see ModuleBuilder.single
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
@KindMarker(SingleKind::class)
annotation class Single

object SingleKind : Kind {
    override fun <T> wrap(
        binding: Binding<T>,
        provider: BindingProvider<T>
    ): BindingProvider<T> = SingleBindingProvider(provider)

    override fun toString(): String = "Single"
}

private class SingleBindingProvider<T>(private val provider: BindingProvider<T>) :
    BindingProvider<T> {
    private var _value: Any? = this

    override fun resolve(component: Component, parameters: Parameters): T {
        var value = _value
        if (value === this) {
            synchronized(this) {
                value = _value
                if (value === this) {
                    _value = provider.resolve(component, parameters)
                    value = _value
                }
            }
        }

        return value as T
    }
}

inline fun <reified T> ModuleBuilder.single(
    name: Any? = null,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    scoping: Scoping = Scoping.Scoped(),
    noinline definition: Definition<T>
): BindingContext<T> = single(
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
fun <T> ModuleBuilder.single(
    type: Type<T>,
    name: Any? = null,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    scoping: Scoping = Scoping.Scoped(),
    definition: Definition<T>
): BindingContext<T> =
    bind(
        binding = DefinitionBinding(
            key = keyOf(type, name),
            kind = SingleKind,
            scoping = scoping,
            overrideStrategy = overrideStrategy,
            definition = definition
        )
    )
