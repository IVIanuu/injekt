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

abstract class Instance<T> {

    /**
     * The binding of this instance
     */
    abstract val binding: Binding<T>

    /**
     * The context of this instance
     */
    lateinit var context: DefinitionContext
        private set

    /**
     * Returns the value for this instance
     */
    abstract fun get(parameters: ParametersDefinition? = null): T

    /**
     * Creates the value for this instance
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

    open fun setDefinitionContext(context: DefinitionContext) {
        this.context = context
    }

}