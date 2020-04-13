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
import com.ivianuu.injekt.AbstractBindingProvider
import com.ivianuu.injekt.BehaviorMarker
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.GenerateDsl
import com.ivianuu.injekt.InterceptingBehavior
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider
import androidx.lifecycle.ViewModelProvider as AndroidViewModelProvider

@GenerateDsl(builderName = "viewModel")
@BehaviorMarker
val BindViewModel = InterceptingBehavior {
    it.copy(provider = ViewModelProvider(it.provider, it.key))
}

private class ViewModelProvider<T>(
    private val wrapped: BindingProvider<T>,
    private val key: Key<*>
) : AbstractBindingProvider<T>() {

    private lateinit var viewModelStoreProvider: Provider<ViewModelStore>

    override fun doLink(linker: Linker) {
        wrapped.link(linker)
    }

    override fun invoke(parameters: Parameters): T {
        val viewModelProvider = AndroidViewModelProvider(
            viewModelStoreProvider(),
            object : AndroidViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                    wrapped(parameters) as T
            }
        )

        return viewModelProvider[key.hashCode().toString(), ViewModel::class.java] as T
    }
}
