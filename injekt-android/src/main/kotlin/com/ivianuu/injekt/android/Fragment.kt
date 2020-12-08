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

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt.android

import android.content.Context
import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.Scoped
import com.ivianuu.injekt.merge.MergeComponent
import com.ivianuu.injekt.merge.get

val Fragment.fragmentComponent: FragmentComponent
    get() = lifecycle.singleton {
        requireActivity().activityComponent
            .get<(Fragment) -> FragmentComponent>()(this)
    }

@Scope interface FragmentScope

@Scoped(FragmentScope::class) @MergeComponent interface FragmentComponent

typealias FragmentContext = Context

@Binding inline fun Fragment.provideFragmentContext(): FragmentContext = requireContext()

typealias FragmentResources = Resources

@Binding inline fun FragmentContext.provideFragmentResources(): FragmentResources = resources

typealias FragmentLifecycleOwner = LifecycleOwner

@Binding inline fun Fragment.provideFragmentLifecycleOwner(): FragmentLifecycleOwner = this

typealias FragmentSavedStateRegistryOwner = SavedStateRegistryOwner

@Binding inline fun Fragment.provideFragmentSavedStateRegistryOwner(): FragmentSavedStateRegistryOwner =
    this

typealias FragmentViewModelStoreOwner = ViewModelStoreOwner

@Binding inline fun Fragment.provideFragmentViewModelStoreOwner(): FragmentViewModelStoreOwner =
    this
