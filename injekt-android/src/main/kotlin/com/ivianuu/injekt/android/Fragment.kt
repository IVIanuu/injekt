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
import com.ivianuu.injekt.Distinct
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.Scoping
import com.ivianuu.injekt.Storage
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader

inline fun <R> Fragment.runFragmentReader(block: @Reader () -> R): R =
    runReader(this, activity!! as ComponentActivity, this) { block() }

@Scoping
object FragmentScoped {
    @Reader
    inline operator fun <T> invoke(
        key: Any,
        init: () -> T
    ) = given<FragmentStorage>().scope(key, init)
}

@Distinct
typealias FragmentStorage = Storage

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
    fun context(): FragmentContext = given<Fragment>().requireContext()

    @Given
    fun resources(): FragmentResources = given<FragmentContext>().resources

    @Given
    fun lifecycleOwner(): FragmentLifecycleOwner = given<ComponentActivity>()

    @Given
    fun savedStateRegistryOwner(): FragmentSavedStateRegistryOwner = given<Fragment>()

    @Given
    fun viewModelStoreOwner(): FragmentViewModelStoreOwner = given<ViewModelStoreOwner>()

}
