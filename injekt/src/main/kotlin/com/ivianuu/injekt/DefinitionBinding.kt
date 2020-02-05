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
 * Creates instances of type [T]
 */
typealias Definition<T> = Component.(Parameters) -> T

fun <T> DefinitionBinding(
    key: Key,
    kind: Kind = FactoryKind,
    scoping: Scoping = Scoping.Unscoped,
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    definition: Definition<T>
) = Binding(
    key = key,
    kind = kind,
    scoping = scoping,
    overrideStrategy = overrideStrategy,
    instanceFactory = DefinitionInstanceFactory(definition)
)

private class DefinitionInstanceFactory<T>(
    private val definition: Definition<T>
) : InstanceFactory<T> {
    override fun create(): Instance<T> = DefinitionInstance(definition)
}

private class DefinitionInstance<T>(private val definition: Component.(Parameters) -> T) :
    Instance<T> {
    override fun resolve(component: Component, parameters: Parameters): T =
        definition.invoke(component, parameters)
}
