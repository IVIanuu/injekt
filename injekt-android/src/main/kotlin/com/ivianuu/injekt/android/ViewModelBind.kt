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
import com.ivianuu.injekt.BehaviorMarker
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.DelegatingBindingProvider
import com.ivianuu.injekt.GenerateDslBuilder
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.get
import com.ivianuu.injekt.interceptingBehavior
import androidx.lifecycle.ViewModelProvider as AndroidViewModelProvider

@GenerateDslBuilder
@BehaviorMarker
val BindViewModel = interceptingBehavior {
    it.copy(provider = ViewModelProvider(it.provider, it.key))
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
