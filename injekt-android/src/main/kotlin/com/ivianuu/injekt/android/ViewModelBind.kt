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

package com.ivianuu.injekt.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.ivianuu.injekt.Behavior
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.InterceptingBehavior
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.bind
import com.ivianuu.injekt.get
import com.ivianuu.injekt.keyOf
import androidx.lifecycle.ViewModelProvider as AndroidViewModelProvider

annotation class BindViewModel {
    companion object : Behavior by (InterceptingBehavior {
        it.copy(provider = ViewModelProvider(it.provider, it.key))
    })
}

inline fun <reified T> ComponentBuilder.viewModel(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    noinline provider: BindingProvider<T>
) {
    viewModel(
        key = keyOf(qualifier),
        behavior = behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

fun <T> ComponentBuilder.viewModel(
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    provider: BindingProvider<T>
) {
    bind(
        key = key,
        behavior = BindViewModel + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

private class ViewModelProvider<T>(
    private val wrapped: BindingProvider<T>,
    private val key: Key<*>
) : (Component, Parameters) -> T {
    override fun invoke(component: Component, parameters: Parameters): T {
        val viewModelStore = component.get<ViewModelStore>()
        val viewModelProvider = AndroidViewModelProvider(
            viewModelStore,
            object : AndroidViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                    wrapped(component, parameters) as T
            }
        )

        return viewModelProvider[key.hashCode().toString(), ViewModel::class.java] as T
    }
}
