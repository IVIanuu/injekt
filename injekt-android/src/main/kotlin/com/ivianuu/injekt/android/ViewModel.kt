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
import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.FunBinding
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.merge.BindingModule
import kotlin.reflect.KClass

@BindingModule(ActivityComponent::class)
annotation class ActivityViewModelBinding {
    companion object {
        @Module
        inline fun <VM : S, reified S : ViewModel> viewModel(getViewModel: getViewModel<VM, S, ActivityViewModelStoreOwner>): S =
            getViewModel(S::class)
    }
}

@BindingModule(FragmentComponent::class)
annotation class FragmentViewModelBinding {
    companion object {
        @Module
        inline fun <VM : S, reified S : ViewModel> viewModel(getViewModel: getViewModel<VM, S, FragmentViewModelStoreOwner>): S =
            getViewModel(S::class)
    }
}

@FunBinding
fun <VM : S, S : ViewModel, VMSO : ViewModelStoreOwner> getViewModel(
    viewModelStoreOwner: VMSO,
    viewModelFactory: () -> VM,
    viewModelClass: @Assisted KClass<S>
): S {
    return ViewModelProvider(
        viewModelStoreOwner,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                viewModelFactory() as T
        }
    )[viewModelClass.java]
}
