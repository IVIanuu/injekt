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
 * Kind for factory instances
 */
object FactoryKind : Kind() {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> = FactoryInstance(binding)
    override fun toString(): String = "Factory"
}

/**
 * Adds a [Binding] which will be created on each request
 */
inline fun <reified T> Module.factory(
    name: Qualifier? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> = factory(typeOf<T>(), name, override, definition)

/**
 * Adds a [Binding] which will be created on each request
 */
fun <T> Module.factory(
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false,
    definition: Definition<T>
): Binding<T> = bind(FactoryKind, type, name, override, definition)

@Target(AnnotationTarget.CLASS)
@KindAnnotation(FactoryKind::class)
annotation class Factory

private class FactoryInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    override fun get(parameters: ParametersDefinition?): T {
        InjektPlugins.logger?.info("Create instance $binding")
        return create(parameters)
    }

}