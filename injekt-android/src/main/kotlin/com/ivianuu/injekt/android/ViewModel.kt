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
import com.ivianuu.injekt.BehaviorMarker
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.ComponentInitObserver
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Qualifier
import androidx.lifecycle.ViewModelProvider as AndroidViewModelProvider

object ViewModelBehavior : Behavior.Element {
    override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> =
        ViewModelProvider(provider)
}

inline fun <reified T : ViewModel> ComponentBuilder.viewModel(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    noinline provider: BindingProvider<T>
) {
    bind(
        qualifier = qualifier,
        behavior = ViewModelBehavior + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

@BehaviorMarker(ViewModelBehavior::class)
annotation class ViewModel

private class ViewModelProvider<T>(
    private val provider: BindingProvider<T>
) : (Component, Parameters) -> T, ComponentInitObserver {
    override fun onInit(component: Component) {
        (provider as? ComponentInitObserver)?.onInit(component)
    }

    override fun invoke(p1: Component, p2: Parameters): T {
        val viewModelStore = p1.get<ViewModelStore>()
        val viewModelProvider = AndroidViewModelProvider(
            viewModelStore,
            object : AndroidViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                    provider(p1, p2) as T
            }
        )

        return viewModelProvider[ViewModel::class.java] as T
    }
}
