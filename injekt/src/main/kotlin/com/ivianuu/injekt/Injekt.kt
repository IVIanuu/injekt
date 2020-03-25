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

    /**
     * Invokes any of [contributors] on each [ComponentBuilder]
     */
    fun componentBuilderContributors(vararg contributors: ComponentBuilderContributor) {
        contributors.forEach { ComponentBuilderContributors.register(it) }
    }

}

inline fun Injekt(block: Injekt.() -> Unit) {
    Injekt.block()
}

inline fun Injekt.componentBuilderContributor(
    scope: Scope? = null,
    invokeOnInit: Boolean = false,
    crossinline block: ComponentBuilder.() -> Unit
) {
    componentBuilderContributors(
        object : ComponentBuilderContributor {
            override val scope: Scope?
                get() = scope
            override val invokeOnInit: Boolean
                get() = invokeOnInit
            override fun apply(builder: ComponentBuilder) {
                builder.block()
            }
        }
    )
}
