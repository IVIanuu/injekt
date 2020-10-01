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

import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Module

@Module
class FragmentModule<T : Fragment>(@Binding val fragment: T) {
    @Binding
    val T.fragment: Fragment
        get() = this

    @Binding
    val Fragment.fragmentContext: FragmentContext
        get() = requireContext()

    @Binding
    val FragmentContext.fragmentResources: FragmentResources
        get() = resources

    @Binding
    val Fragment.fragmentLifecycleOwner: FragmentLifecycleOwner
        get() = this

    @Binding
    val Fragment.fragmentSavedStateRegistryOwner: FragmentSavedStateRegistryOwner
        get() = this

    @Binding
    val Fragment.fragmentViewModelStoreOwner: FragmentViewModelStoreOwner
        get() = this
}

typealias FragmentContext = android.content.Context

typealias FragmentResources = Resources

typealias FragmentLifecycleOwner = LifecycleOwner

typealias FragmentSavedStateRegistryOwner = SavedStateRegistryOwner

typealias FragmentViewModelStoreOwner = ViewModelStoreOwner
