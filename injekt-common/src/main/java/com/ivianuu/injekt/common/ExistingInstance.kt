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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Attributes
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.InjektPlugins
import com.ivianuu.injekt.Instance
import com.ivianuu.injekt.InstanceFactory
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.attributesOf
import com.ivianuu.injekt.create
import com.ivianuu.injekt.logger
import kotlin.reflect.KClass

const val INSTANCE_KIND = "INSTANCE"

private val noopDefinition: Definition<Any> = {}

/**
 * Creates a [Binding] for an existing instance
 */
fun <T> Binding.Companion.createInstance(
    type: KClass<*>,
    instance: T,
    name: String? = null,
    scopeName: String? = null,
    attributes: Attributes = attributesOf(),
    override: Boolean = false
): Binding<T> =
    Binding.create(
        type, name, INSTANCE_KIND, ExistingInstanceFactory(instance),
        scopeName, attributes, override, false, noopDefinition as Definition<T>
    )

/**
 * Holds a already existing instance
 */
class ExistingInstance<T>(
    override val binding: Binding<T>,
    private val instance: T
) : Instance<T> {

    override val isCreated: Boolean
        get() = true

    override fun get(component: Component, parameters: ParametersDefinition?): T {
        InjektPlugins.logger?.info("${component.name} Return instance $binding")
        return instance
    }

}

/**
 * Instance factory for [ExistingInstance]s
 */
class ExistingInstanceFactory<S>(
    private val existingInstance: S
) : InstanceFactory {

    private var instance: Instance<S>? = null

    override fun <T> create(binding: Binding<T>, component: Component?): Instance<T> {
        if (instance == null) {
            instance = ExistingInstance(binding as Binding<S>, existingInstance)
        }

        return instance as Instance<T>
    }

}

/**
 * Provides a existing instance
 */
inline fun <reified T> Module.instance(
    name: String? = null,
    scopeName: String? = null,
    override: Boolean = false,
    instance: () -> T
): BindingContext<T> = instance(T::class, name, scopeName, override, instance)

/**
 * Provides a existing instance
 */
inline fun <T> Module.instance(
    type: KClass<*>,
    name: String? = null,
    scopeName: String? = null,
    override: Boolean = false,
    instance: () -> T
): BindingContext<T> = instance(type, instance(), name, scopeName, override)

@PublishedApi
internal fun <T> Module.instance(
    type: KClass<*>,
    instance: T,
    name: String? = null,
    scopeName: String? = null,
    override: Boolean = false
): BindingContext<T> = declare(
    Binding.createInstance(
        type = type,
        instance = instance,
        name = name,
        scopeName = scopeName,
        override = override
    )
)

/**
 * Adds a [Binding] for the [instance]
 */
fun <T : Any> Component.addInstance(instance: T) {
    addBinding(Binding.createInstance(instance::class, instance))
}