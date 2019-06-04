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
 * Holder for instances of [Binding]s
 * It can just create a new instance of the dependencies or perform caching in some form
 */
abstract class Instance<T> {

    lateinit var context: DefinitionContext
        internal set

    abstract val binding: Binding<T>

    /**
     * Returns the value for this instance
     */
    abstract fun get(parameters: ParametersDefinition? = null): T

    /**
     * Creates the value using [parameters]
     */
    protected fun create(parameters: ParametersDefinition?): T {
        return try {
            binding.definition.invoke(
                context,
                parameters?.invoke() ?: emptyParameters()
            )
        } catch (e: Exception) {
            throw IllegalStateException("Couldn't instantiate $binding", e)
        }
    }

    open fun attached() {
    }
}