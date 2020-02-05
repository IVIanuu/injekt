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

interface Instance<T> {
    fun resolve(
        component: Component,
        parameters: Parameters = emptyParameters()
    ): T

    fun onAttach(component: Component) {
    }
}

interface InstanceFactory<T> {
    fun create(): Instance<T>
}

fun <T> InstanceBinding(
    key: Key,
    instance: T,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail
) = Binding(
    key = key,
    kind = FactoryKind,
    scoping = Scoping.Unscoped,
    overrideStrategy = overrideStrategy,
    instanceFactory = InstanceInstanceFactory(instance)
)

private class InstanceInstanceFactory<T>(private val instance: T) : InstanceFactory<T> {
    override fun create(): Instance<T> = InstanceInstance(instance)
}

private class InstanceInstance<T>(val instance: T) : Instance<T> {
    override fun resolve(component: Component, parameters: Parameters): T = instance
}