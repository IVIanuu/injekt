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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.ivianuu.injekt.Decorator

@Decorator
annotation class ActivityViewModel {
    companion object {
        inline fun <reified VM : ViewModel> decorate(
            storeOwner: ActivityViewModelStoreOwner,
            crossinline factory: () -> VM
        ): () -> VM = { storeOwner.get(factory) }
    }
}

@Decorator
annotation class FragmentViewModel {
    companion object {
        inline fun <reified VM : ViewModel> decorate(
            storeOwner: FragmentViewModelStoreOwner,
            crossinline factory: () -> VM
        ): () -> VM = { storeOwner.get(factory) }
    }
}

@PublishedApi
internal inline fun <reified VM : ViewModel> ViewModelStoreOwner.get(
    crossinline viewModelFactory: () -> VM
): VM {
    return ViewModelProvider(
        this,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                viewModelFactory() as T
        }
    )[VM::class.java]
}
