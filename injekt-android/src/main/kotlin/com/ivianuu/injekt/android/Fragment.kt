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
import com.ivianuu.injekt.Context
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.childContext
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader

interface FragmentContext : Context

val Fragment.fragmentContext: FragmentContext
    get() = lifecycle.singleton {
        activity!!.activityContext.runReader {
            childContext(this)
        }
    }

typealias AndroidFragmentContext = android.content.Context

typealias FragmentResources = Resources

typealias FragmentLifecycleOwner = LifecycleOwner

typealias FragmentSavedStateRegistryOwner = SavedStateRegistryOwner

typealias FragmentViewModelStoreOwner = ViewModelStoreOwner

object FragmentGivens {

    @Given
    fun context(): AndroidFragmentContext = given<Fragment>().requireContext()

    @Given
    fun resources(): FragmentResources = given<AndroidFragmentContext>().resources

    @Given
    fun lifecycleOwner(): FragmentLifecycleOwner = given<Fragment>()

    @Given
    fun savedStateRegistryOwner(): FragmentSavedStateRegistryOwner = given<Fragment>()

    @Given
    fun viewModelStoreOwner(): FragmentViewModelStoreOwner = given<Fragment>()

}
