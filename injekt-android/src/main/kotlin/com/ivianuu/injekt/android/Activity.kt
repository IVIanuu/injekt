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
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.merge.MergeChildComponent
import com.ivianuu.injekt.merge.get

val ComponentActivity.activityComponent: ActivityComponent
    get() = lifecycle.singleton {
        retainedActivityComponent
            .get<(ComponentActivity) -> ActivityComponent>()(this)
    }

@MergeChildComponent
abstract class ActivityComponent(@Binding protected val activity: ComponentActivity)

typealias ActivityContext = Context

@Binding inline fun ComponentActivity.provideActivityContext(): ActivityContext = this

typealias ActivityResources = Resources

@Binding inline fun ComponentActivity.provideActivityResources(): ActivityResources = resources

typealias ActivityLifecycleOwner = LifecycleOwner

@Binding inline fun ComponentActivity.provideActivityLifecycleOwner(): ActivityLifecycleOwner = this

typealias ActivityOnBackPressedDispatcherOwner = OnBackPressedDispatcherOwner

@Binding
inline fun ComponentActivity.provideActivityOnBackPressedDispatcherOwner(): ActivityOnBackPressedDispatcherOwner =
    this

typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner

@Binding
inline fun ComponentActivity.provideActivitySavedStateRegistryOwner(): ActivitySavedStateRegistryOwner =
    this

typealias ActivityViewModelStoreOwner = ViewModelStoreOwner

@Binding inline fun ComponentActivity.provideActivityViewModelStoreOwner(): ActivityViewModelStoreOwner =
    this
