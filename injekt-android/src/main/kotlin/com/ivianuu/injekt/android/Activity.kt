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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.merge.MergeChildComponent
import com.ivianuu.injekt.merge.MergeInto
import com.ivianuu.injekt.merge.mergeComponent

val ComponentActivity.activityComponent: ActivityComponent
    get() = lifecycle.singleton {
        retainedActivityComponent
            .mergeComponent<ActivityComponentFactoryOwner>()
            .activityComponentFactoryOwner(this)
    }

@MergeChildComponent
abstract class ActivityComponent(@Binding protected val activity: ComponentActivity)

typealias ActivityContext = Context
@Binding
val ComponentActivity.activityContext: ActivityContext
    get() = this

typealias ActivityResources = Resources
@Binding
val ComponentActivity.activityResources: ActivityResources
    get() = resources

typealias ActivityLifecycleOwner = LifecycleOwner
@Binding
val ComponentActivity.activityLifecycleOwner: ActivityLifecycleOwner
    get() = this

typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner
@Binding
val ComponentActivity.activitySavedStateRegistryOwner: ActivitySavedStateRegistryOwner
    get() = this

typealias ActivityViewModelStoreOwner = ViewModelStoreOwner
@Binding
val ComponentActivity.activityViewModelStoreOwner: ActivityViewModelStoreOwner
    get() = this

@MergeInto(RetainedActivityComponent::class)
interface ActivityComponentFactoryOwner {
    val activityComponentFactoryOwner: (ComponentActivity) -> ActivityComponent
}
