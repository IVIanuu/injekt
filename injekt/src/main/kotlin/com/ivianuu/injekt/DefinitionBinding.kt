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

internal class DefinitionBinding<T>(
    overrideStrategy: OverrideStrategy = OverrideStrategy.Fail,
    eager: Boolean = false,
    scoped: Boolean = false,
    val single: Boolean = false,
    private val definition: Definition<T>
) : Binding<T>(overrideStrategy = overrideStrategy, eager = eager, scoped = scoped) {
    override fun link(component: Component): Provider<T> {
        val provider = DefinitionProvider(component, definition)
        return if (single) SingleProvider(provider) else provider
    }

    private class DefinitionProvider<T>(
        private val component: Component,
        private val definition: Definition<T>
    ) : Provider<T> {
        override fun invoke(parameters: Parameters): T =
            definition(component, parameters)
    }
}
