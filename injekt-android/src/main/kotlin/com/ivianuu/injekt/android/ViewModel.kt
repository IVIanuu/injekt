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
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.get
import kotlin.reflect.KClass

@Reader
inline fun <reified T : ViewModel> viewModel(
    owner: ViewModelStoreOwner = get<ActivityViewModelStoreOwner>(),
    noinline init: () -> T = { get() }
) = _viewModel(T::class, owner, init)

@PublishedApi
internal fun <T : ViewModel> _viewModel(
    clazz: KClass<T>,
    owner: ViewModelStoreOwner,
    init: () -> T
): T {
    return ViewModelProvider(
        owner,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T = init() as T
        }
    )[clazz.java]
}
