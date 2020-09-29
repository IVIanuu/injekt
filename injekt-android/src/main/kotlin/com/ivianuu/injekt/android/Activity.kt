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
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.merge.ApplicationComponent
import com.ivianuu.injekt.merge.EntryPoint
import com.ivianuu.injekt.merge.MergeComponent
import com.ivianuu.injekt.merge.MergeFactory
import com.ivianuu.injekt.merge.entryPoint

val ComponentActivity.activityComponent: ActivityComponent
    get() = lifecycle.singleton {
        retainedActivityComponent.entryPoint<ActivityComponentEntryPoint>()
            .activityComponentFactory(this)
    }

@MergeComponent
interface ActivityComponent

@MergeFactory(ApplicationComponent::class)
typealias ActivityComponentFactory = (ComponentActivity) -> ActivityComponent

@Module(ActivityComponent::class)
object ActivityModule {

    @Given
    val ComponentActivity.activityContext: ActivityContext
        get() = this

    @Given
    val ComponentActivity.activityResources: ActivityResources
        get() = resources

    @Given
    val ComponentActivity.activityLifecycleOwner: ActivityLifecycleOwner
        get() = this

    @Given
    val ComponentActivity.activitySavedStateRegistryOwner: ActivitySavedStateRegistryOwner
        get() = this

    @Given
    val ComponentActivity.activityViewModelStoreOwner: ActivityViewModelStoreOwner
        get() = this

}

typealias ActivityContext = Context

typealias ActivityResources = Resources

typealias ActivityLifecycleOwner = LifecycleOwner

typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner

typealias ActivityViewModelStoreOwner = ViewModelStoreOwner

@EntryPoint(RetainedActivityComponent::class)
interface ActivityComponentEntryPoint {
    val activityComponentFactory: ActivityComponentFactory
}
