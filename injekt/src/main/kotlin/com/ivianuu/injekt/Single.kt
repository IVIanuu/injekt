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
        key: Key,
        binding: Binding<T>,
        provider: Provider<T>,
        component: Component
    ): Provider<T> = SingleProvider(provider)

    override fun toString(): String = "Single"
}

private class SingleProvider<T>(private val provider: Provider<T>) : Provider<T> {
    private var _value: Any? = this

    override fun invoke(parameters: Parameters): T {
        var value = _value
        if (value === this) {
            synchronized(this) {
                value = _value
                if (value === this) {
                    _value = provider(parameters)
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
    scoped: Boolean = true,
    eager: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = single(
    type = typeOf(),
    name = name,
    overrideStrategy = overrideStrategy,
    scoped = scoped,
    eager = eager,
    definition = definition
)

/**
 * Contributes a binding which will be reused throughout the lifetime of the [Component] it life's in
 *
 * @param type the of the instance
 * @param name the name of the instance
 * @param overrideStrategy the strategy for handling overrides
 * @param scoped whether or not to create instances in the added scope
 * @param eager whether the instance should be created when the [Component] get's created
 * @param definition the definitions which creates instances
 *
 * @see ModuleBuilder.bind
 */
fun <T> ModuleBuilder.single(
    type: Type<T>,
    name: Any? = null,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    scoped: Boolean = true,
    eager: Boolean = false,
    definition: Definition<T>
): BindingContext<T> =
    bind(
        key = keyOf(type, name),
        binding = DefinitionBinding(
            kind = SingleKind,
            overrideStrategy = overrideStrategy,
            eager = eager,
            scoped = scoped,
            definition = definition
        )
    )
