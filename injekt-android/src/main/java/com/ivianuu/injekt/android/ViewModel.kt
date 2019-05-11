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

package com.ivianuu.injekt.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.ivianuu.injekt.*
import kotlin.reflect.KClass

/**
 * Declares a new [ViewModel] binding which will be scoped by the [ViewModelStore]
 */
inline fun <reified T : ViewModel> Module.viewModel(
    name: Any? = null,
    viewModelStoreName: Any? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> =
    factory(
        name,
        override,
        ViewModelDefinition(T::class, name?.toString(), viewModelStoreName, definition)
    )

@PublishedApi
internal class ViewModelDefinition<T : ViewModel>(
    private val type: KClass<T>,
    private val key: String?,
    private val viewModelStoreName: Any?,
    private val definition: Definition<T>
) : (DefinitionContext, Parameters) -> T {
    override fun invoke(context: DefinitionContext, parameters: Parameters): T = with(context) {
        val store = get<ViewModelStore>(viewModelStoreName)

        val factory = Factory(context, parameters, definition)
        val provider = ViewModelProvider(store, factory)
        return@with if (key != null) {
            provider.get(key, type.java)
        } else {
            provider.get(type.java)
        }
    }

    private class Factory<T : ViewModel>(
        private val context: DefinitionContext,
        private val parameters: Parameters,
        private val definition: Definition<T>
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            definition(context, parameters) as T
    }
}