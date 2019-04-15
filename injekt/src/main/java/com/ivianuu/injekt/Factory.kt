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
object FactoryKind : Kind {
    private const val FACTORY_KIND = "Factory"

    override fun <T> createInstance(binding: Binding<T>, component: Component?): Instance<T> =
        FactoryInstance(binding, component)

    override fun asString(): String = FACTORY_KIND

}

/**
 * A [Instance] which creates a new value on every [get] call
 */
class FactoryInstance<T>(
    override val binding: Binding<T>,
    val component: Component?
) : Instance<T>() {

    override fun get(
        component: Component,
        parameters: ParametersDefinition?
    ): T {
        val component = this.component ?: component
        InjektPlugins.logger?.info("${component.componentName()} Create instance $binding")
        return create(component, parameters)
    }

}

/**
 * Provides a unscoped dependency which will be recreated on each request
 */
inline fun <reified T> Module.factory(
    qualifier: Qualifier? = null,
    scope: Scope? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = add(
    Binding(
        type = T::class,
        qualifier = qualifier,
        kind = FactoryKind,
        scope = scope,
        override = override,
        definition = definition
    )
)