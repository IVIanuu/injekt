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

class DefinitionBinding<T>(private val definition: Definition<T>) : Binding<T> {
    private lateinit var component: Component
    override fun attach(component: Component) {
        this.component = component
    }

    override fun get(parameters: ParametersDefinition?): T {
        return try {
            definition.invoke(component, parameters?.invoke() ?: emptyParameters())
        } catch (e: Exception) {
            throw IllegalStateException("Couldn't instantiate", e) // todo
        }
    }
}

/**
 * Will called when ever a new instance is needed
 */
typealias Definition<T> = Component.(parameters: Parameters) -> T