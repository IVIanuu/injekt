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
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.scope.ChildScopeFactory
import com.ivianuu.injekt.scope.ChildGivenScopeModule1
import com.ivianuu.injekt.scope.DefaultGivenScope
import com.ivianuu.injekt.scope.element

/**
 * Returns the [ActivityGivenScope] of this [ComponentActivity]
 * whose lifecycle is bound to the activity
 */
val ComponentActivity.activityGivenScope: ActivityGivenScope
    get() = lifecycle.givenScope {
        activityRetainedGivenScope
            .element<@ChildScopeFactory (ComponentActivity) -> ActivityGivenScope>()
            .invoke(this)
    }

typealias ActivityGivenScope = DefaultGivenScope

@Given
val activityGivenScopeModule =
    ChildGivenScopeModule1<ActivityRetainedGivenScope, ComponentActivity, ActivityGivenScope>()

typealias ActivityContext = Context

@Given
inline val ComponentActivity.activityContext: ActivityContext
    get() = this

typealias ActivityResources = Resources

@Given
inline val ComponentActivity.activityResources: ActivityResources
    get() = resources

typealias ActivityLifecycleOwner = LifecycleOwner

@Given
inline val ComponentActivity.activityLifecycleOwner: ActivityLifecycleOwner
    get() = this

typealias ActivityOnBackPressedDispatcherOwner = OnBackPressedDispatcherOwner

@Given
inline val ComponentActivity.activityOnBackPressedDispatcherOwner: ActivityOnBackPressedDispatcherOwner
    get() = this

typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner

@Given
inline val ComponentActivity.activitySavedStateRegistryOwner: ActivitySavedStateRegistryOwner
    get() = this

typealias ActivityViewModelStoreOwner = ViewModelStoreOwner

@Given
inline val ComponentActivity.activityViewModelStoreOwner: ActivityViewModelStoreOwner
    get() = this

typealias ActivityCoroutineScope = LifecycleCoroutineScope

@Given
inline val ComponentActivity.activityCoroutineScope: ActivityCoroutineScope
    get() = lifecycleScope
