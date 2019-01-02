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

import com.ivianuu.injekt.InjektPlugins.logger

/**
 * Manages all dependencies of a [Component]
 */
class ComponentRegistry internal constructor(val component: Component) {

    private val dependencies = hashSetOf<Component>()

    /**
     * Whether or not the [component] is a dependency
     */
    fun dependsOn(component: Component) = dependencies.contains(component)

    /**
     * All dependencies
     */
    fun getDependencies() = dependencies

    /**
     * Adds all of [components] as a dependency
     */
    fun addComponents(vararg components: Component) {
        val allComponents = dependencies.flatMap { listOf(it) + it.componentRegistry.dependencies }

        components.forEach {
            if (allComponents.contains(it)) {
                error("${component.name} component already added ${it.name}")
            }

            component.declarationRegistry.checkOverrides(it)

            logger?.info("${component.name} adding component ${it.name}")
            dependencies.add(it)
        }
    }

}