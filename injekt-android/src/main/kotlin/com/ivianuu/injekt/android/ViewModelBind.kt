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
import com.ivianuu.injekt.DelegatingBindingProvider
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.IntoComponent
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.KeyOverload
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.TagMarker
import com.ivianuu.injekt.keyOverloadStub
import androidx.lifecycle.ViewModelProvider as AndroidViewModelProvider

class ViewModelBehavior(private val key: Key<*>) : Behavior.Element {
    override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> =
        ViewModelProvider(provider, key)
}

@KeyOverload
inline fun <reified T : ViewModel> ComponentBuilder.viewModel(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    crossinline provider: Component.(Parameters) -> T
) {
    keyOverloadStub<Unit>()
}

inline fun <T : ViewModel> ComponentBuilder.viewModel(
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    crossinline provider: Component.(Parameters) -> T
) {
    bind(
        key = key,
        behavior = ViewModelBehavior(key) + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

@TagMarker
annotation class ViewModelBind {
    companion object : Tag
}

@IntoComponent(invokeOnInit = true)
private fun ComponentBuilder.viewModelBindingInterceptor() {
    bindingInterceptor { binding ->
        if (ViewModelBind in binding.tags) {
            binding.copy(behavior = ViewModelBehavior(binding.key) + binding.behavior)
        } else {
            binding
        }
    }
}

private class ViewModelProvider<T>(
    delegate: BindingProvider<T>,
    private val key: Key<*>
) : DelegatingBindingProvider<T>(delegate) {
    override fun invoke(component: Component, parameters: Parameters): T {
        val viewModelStore = component.get<ViewModelStore>()
        val viewModelProvider = AndroidViewModelProvider(
            viewModelStore,
            object : AndroidViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                    this@ViewModelProvider(component, parameters) as T
            }
        )

        return viewModelProvider[key.hashCode().toString(), ViewModel::class.java] as T
    }
}
