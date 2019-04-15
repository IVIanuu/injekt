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
 * Factory kind
 */
object SingleKind : Kind {
    private const val SINGLE_KIND = "Single"

    override fun <T> createInstance(binding: Binding<T>, context: DefinitionContext?): Instance<T> =
        SingleInstance(binding, context)

    override fun asString(): String = SINGLE_KIND
}

private object UNINITIALIZED

/**
 * A [Instance] which creates the value 1 time per [Component] and caches the result
 */
class SingleInstance<T>(
    override val binding: Binding<T>,
    val context: DefinitionContext?
) : Instance<T>() {

    private var _value: Any? = UNINITIALIZED

    override fun get(
        context: DefinitionContext,
        parameters: ParametersDefinition?
    ): T {
        val context = this.context ?: context

        if (_value !== UNINITIALIZED) {
            InjektPlugins.logger?.info("Return existing instance $binding")
            return _value as T
        }

        synchronized(this) {
            if (_value !== UNINITIALIZED) {
                InjektPlugins.logger?.info("Return existing instance $binding")
                return@get _value as T
            }

            InjektPlugins.logger?.info("Create instance $binding")
            _value = create(context, parameters)
            return@get _value as T
        }
    }

}

/**
 * Provides scoped dependency which will be created once for each component
 */
inline fun <reified T> Module.single(
    qualifier: Qualifier? = null,
    override: Boolean = false,
    eager: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = add(
    Binding(
        type = T::class,
        qualifier = qualifier,
        kind = SingleKind,
        override = override,
        eager = eager,
        definition = definition
    )
)