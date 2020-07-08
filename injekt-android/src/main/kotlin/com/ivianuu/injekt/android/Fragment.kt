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

import android.content.Context
import android.content.res.Resources
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.DistinctType
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.Unscoped
import com.ivianuu.injekt.get
import com.ivianuu.injekt.runReader

@Component(parent = ActivityComponent::class)
interface FragmentComponent {
    @Component.Factory
    interface Factory {
        fun create(instance: Fragment): FragmentComponent
    }
}

val Fragment.fragmentComponent: FragmentComponent
    get() = lifecycle.singleton {
        activity!!.activityComponent.runReader {
            get<FragmentComponent.Factory>().create(this)
        }
    }

@DistinctType
typealias FragmentContext = Context
@DistinctType
typealias FragmentResources = Resources
@DistinctType
typealias FragmentLifecycleOwner = LifecycleOwner
@DistinctType
typealias FragmentSavedStateRegistryOwner = SavedStateRegistryOwner
@DistinctType
typealias FragmentViewModelStoreOwner = ViewModelStoreOwner

object FragmentModule {
    @Unscoped
    @Reader
    fun context(): FragmentContext = get<Fragment>().requireContext()

    @Unscoped
    @Reader
    fun resources(): FragmentResources = get<FragmentContext>().resources

    @Unscoped
    @Reader
    fun lifecycleOwner(): FragmentLifecycleOwner = get<ComponentActivity>()

    @Unscoped
    @Reader
    fun savedStateRegistryOwner(): FragmentSavedStateRegistryOwner = get<Fragment>()

    @Unscoped
    @Reader
    fun viewModelStoreOwner(): FragmentViewModelStoreOwner = get<ViewModelStoreOwner>()
}
