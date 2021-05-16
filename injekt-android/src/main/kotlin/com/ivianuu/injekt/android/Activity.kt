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
 * Returns the [ActivityGivenScope] of this [ComponentActivity]
 * whose lifecycle is bound to the activity
 */
val ComponentActivity.activityGivenScope: ActivityGivenScope
  get() = lifecycle.givenScope {
    activityRetainedGivenScope
      .element<@ChildScopeFactory (ComponentActivity) -> ActivityGivenScope>()
      .invoke(this)
  }

typealias ActivityGivenScope = GivenScope

@Given val activityGivenScopeModule =
  ChildScopeModule1<ActivityRetainedGivenScope, ComponentActivity, ActivityGivenScope>()

typealias ActivityContext = Context

object ActivityContextGivens {
  @Given inline val ComponentActivity.activityContext: ActivityContext
    get() = this
}

typealias ActivityResources = Resources

object ActivityResourcesGivens {
  @Given inline val ComponentActivity.activityResources: ActivityResources
    get() = resources
}

typealias ActivityLifecycleOwner = LifecycleOwner

object ActivityLifecycleOwnerGivens {
  @Given inline val ComponentActivity.activityLifecycleOwner: ActivityLifecycleOwner
    get() = this
}

typealias ActivityOnBackPressedDispatcherOwner = OnBackPressedDispatcherOwner

object ActivityOnBackPressedDispatcherOwnerGivens {
  @Given
  inline val ComponentActivity.activityOnBackPressedDispatcherOwner: ActivityOnBackPressedDispatcherOwner
    get() = this
}

typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner

object ActivitySavedStateRegistryOwnerGivens {
  @Given inline val ComponentActivity.activitySavedStateRegistryOwner: ActivitySavedStateRegistryOwner
    get() = this
}

typealias ActivityViewModelStoreOwner = ViewModelStoreOwner

object ActivityViewModelStoreOwnerGivens {
  @Given inline val ComponentActivity.activityViewModelStoreOwner: ActivityViewModelStoreOwner
    get() = this
}

typealias ActivityCoroutineScope = LifecycleCoroutineScope

object ActivityCoroutineScopeGivens {
  @Given inline val ComponentActivity.activityCoroutineScope: ActivityCoroutineScope
    get() = lifecycleScope
}
