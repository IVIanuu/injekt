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
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.merge.EntryPoint
import com.ivianuu.injekt.merge.MergeComponent
import com.ivianuu.injekt.merge.entryPoint

val Fragment.fragmentComponent: FragmentComponent
    get() = lifecycle.singleton {
        activity!!.activityComponent
            .entryPoint<FragmentComponentEntryPoint>()
            .fragmentComponentFactory
            .create(this)
    }

@MergeComponent
interface FragmentComponent {
    @MergeComponent.Factory
    interface Factory {
        fun create(fragment: Fragment): FragmentComponent
    }
}

typealias FragmentContext = android.content.Context

typealias FragmentResources = Resources

typealias FragmentLifecycleOwner = LifecycleOwner

typealias FragmentSavedStateRegistryOwner = SavedStateRegistryOwner

typealias FragmentViewModelStoreOwner = ViewModelStoreOwner

@Module(FragmentComponent::class)
object FragmentModule {

    @Given
    val Fragment.fragmentContext: FragmentContext
        get() = requireContext()

    @Given
    val FragmentContext.fragmentResources: FragmentResources
        get() = resources

    @Given
    val Fragment.fragmentLifecycleOwner: FragmentLifecycleOwner
        get() = this

    @Given
    val Fragment.fragmentSavedStateRegistryOwner: FragmentSavedStateRegistryOwner
        get() = this

    @Given
    val Fragment.fragmentViewModelStoreOwner: FragmentViewModelStoreOwner
        get() = this

}

@EntryPoint(FragmentComponent::class)
interface FragmentComponentEntryPoint {
    val fragmentComponentFactory: FragmentComponent.Factory
}
