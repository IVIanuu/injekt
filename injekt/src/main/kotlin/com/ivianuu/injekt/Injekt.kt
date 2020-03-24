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
 * Global configurations
 */
object Injekt {

    /**
     * The logger to use
     */
    var logger: Logger? = null

    private val _componentBuilderContributors = mutableListOf<ComponentBuilderContributor>()
    val componentBuilderContributors: List<ComponentBuilderContributor> get() = _componentBuilderContributors

    /**
     * Invokes any of [contributors] on each [ComponentBuilder]
     */
    fun componentBuilderContributors(vararg contributors: ComponentBuilderContributor) {
        _componentBuilderContributors += contributors
    }

    /**
     * Replaces all existing contributors with [contributors]
     */
    fun setComponentBuilderContributors(contributors: List<ComponentBuilderContributor>) {
        _componentBuilderContributors.clear()
        _componentBuilderContributors += contributors
    }

}

inline fun Injekt.componentBuilderContributor(crossinline block: ComponentBuilder.() -> Unit) {
    componentBuilderContributors(
        object : ComponentBuilderContributor {
            override fun apply(builder: ComponentBuilder) {
                builder.block()
            }
        }
    )
}
