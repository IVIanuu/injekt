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

internal abstract class Instance<T> {

    abstract val binding: Binding<T>

    abstract fun get(
        context: DefinitionContext,
        parameters: ParametersDefinition?
    ): T

    protected fun create(
        context: DefinitionContext,
        parameters: ParametersDefinition?
    ): T {
        return try {
            binding.definition.invoke(
                context,
                parameters?.invoke() ?: emptyParameters()
            )
        } catch (e: Exception) {
            throw IllegalStateException("Couldn't instantiate $binding", e)
        }
    }

}

internal class FactoryInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    override fun get(
        context: DefinitionContext,
        parameters: ParametersDefinition?
    ): T {
        InjektPlugins.logger?.info("Create instance $binding")
        return create(context, parameters)
    }

}

internal class SingleInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    private var _value: Any? = UNINITIALIZED

    override fun get(
        context: DefinitionContext,
        parameters: ParametersDefinition?
    ): T {
        if (_value !== UNINITIALIZED) {
            InjektPlugins.logger?.info("Return existing instance $binding")
            return _value as T
        }

        synchronized(this) {
            if (_value !== UNINITIALIZED) {
                InjektPlugins.logger?.info("Return existing instance $binding")
                return@get _value as T
            }

            InjektPlugins.logger?.info("Create instance $binding")
            _value = create(context, parameters)
            return@get _value as T
        }
    }

    private companion object {
        private object UNINITIALIZED
    }
}