/*
 * Copyright 2021 Manuel Wrage
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

import android.content.*
import android.content.res.*
import androidx.activity.*
import androidx.lifecycle.*
import androidx.savedstate.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*

/**
 * Returns the [ActivityScope] of this [ComponentActivity]
 * whose lifecycle is bound to the activity
 */
val ComponentActivity.activityScope: ActivityScope
  get() = lifecycle.scope {
    activityRetainedScope
      .element<@ChildScopeFactory (ComponentActivity) -> ActivityScope>()
      .invoke(this)
  }

typealias ActivityScope = Scope

@Provide val activityScopeModule =
  ChildScopeModule1<ActivityRetainedScope, ComponentActivity, ActivityScope>()

typealias ActivityContext = Context

@Provide inline val ComponentActivity.activityContext: ActivityContext
  get() = this

typealias ActivityResources = Resources

@Provide inline val ComponentActivity.activityResources: ActivityResources
  get() = resources

typealias ActivityLifecycleOwner = LifecycleOwner

@Provide inline val ComponentActivity.activityLifecycleOwner: ActivityLifecycleOwner
  get() = this

typealias ActivityOnBackPressedDispatcherOwner = OnBackPressedDispatcherOwner

@Provide
inline val ComponentActivity.activityOnBackPressedDispatcherOwner: ActivityOnBackPressedDispatcherOwner
  get() = this

typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner

@Provide inline val ComponentActivity.activitySavedStateRegistryOwner: ActivitySavedStateRegistryOwner
  get() = this

typealias ActivityViewModelStoreOwner = ViewModelStoreOwner

@Provide inline val ComponentActivity.activityViewModelStoreOwner: ActivityViewModelStoreOwner
  get() = this

typealias ActivityCoroutineScope = LifecycleCoroutineScope

@Provide inline val ComponentActivity.activityCoroutineScope: ActivityCoroutineScope
  get() = lifecycleScope
