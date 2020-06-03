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
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.composition.BindingAdapter
import com.ivianuu.injekt.composition.BindingAdapterFunction
import com.ivianuu.injekt.transient

@BindingAdapter(ActivityComponent::class)
annotation class ActivityViewModel

@BindingAdapterFunction(ActivityViewModel::class)
@Module
inline fun <reified T : ViewModel> activityViewModel() {
    baseViewModel<T, @ForActivity ViewModelStoreOwner>()
}

@BindingAdapter(FragmentComponent::class)
annotation class FragmentViewModel

@BindingAdapterFunction(FragmentViewModel::class)
@Module
inline fun <reified T : ViewModel> fragmentComponent() {
    baseViewModel<T, @ForFragment ViewModelStoreOwner>()
}

@Module
inline fun <reified T : ViewModel, S : ViewModelStoreOwner> baseViewModel() {
    transient<@UnscopedViewModel T>()
    transient { viewModelStoreOwner: S, viewModelProvider: @Provider () -> @UnscopedViewModel T ->
        ViewModelProvider(
            viewModelStoreOwner,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    viewModelProvider() as T
            }
        ).get(T::class.java)
    }
}

@Target(AnnotationTarget.TYPE)
@Qualifier
private annotation class UnscopedViewModel
