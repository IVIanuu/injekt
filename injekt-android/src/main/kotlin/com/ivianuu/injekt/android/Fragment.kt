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
import com.ivianuu.injekt.Distinct
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given
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
            given<FragmentComponent.Factory>().create(this)
        }
    }

@Distinct
typealias FragmentContext = Context
@Distinct
typealias FragmentResources = Resources
@Distinct
typealias FragmentLifecycleOwner = LifecycleOwner
@Distinct
typealias FragmentSavedStateRegistryOwner = SavedStateRegistryOwner
@Distinct
typealias FragmentViewModelStoreOwner = ViewModelStoreOwner

object FragmentModule {
    @Given
    @Reader
    fun context(): FragmentContext = given<Fragment>().requireContext()

    @Given
    @Reader
    fun resources(): FragmentResources = given<FragmentContext>().resources

    @Given
    @Reader
    fun lifecycleOwner(): FragmentLifecycleOwner = given<ComponentActivity>()

    @Given
    @Reader
    fun savedStateRegistryOwner(): FragmentSavedStateRegistryOwner = given<Fragment>()

    @Given
    @Reader
    fun viewModelStoreOwner(): FragmentViewModelStoreOwner = given<ViewModelStoreOwner>()
}
