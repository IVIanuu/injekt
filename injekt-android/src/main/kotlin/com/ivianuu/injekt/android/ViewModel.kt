// injekt-incremental-fix 1614767868610 injekt-end
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
import com.ivianuu.injekt.Given
import kotlin.reflect.KClass

inline fun <reified VM : ViewModel> activityViewModel(
    @Given owner: ActivityViewModelStoreOwner,
    @Given noinline factory: () -> VM,
): VM = viewModel<ActivityViewModelStoreOwner, VM>()

inline fun <O : ViewModelStoreOwner, reified VM : ViewModel> viewModel(
    @Given owner: O,
    @Given noinline factory: () -> VM,
): VM = viewModelImpl<O, VM>(vmClass = VM::class)

@PublishedApi
internal fun <O : ViewModelStoreOwner, VM : ViewModel> viewModelImpl(
    vmClass: KClass<VM>,
    @Given owner: O,
    @Given factory: () -> VM,
): VM {
    return ViewModelProvider(
        owner,
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                factory() as T
        }
    )[vmClass.java]
}
