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
import com.ivianuu.injekt.ApplicationComponent
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.DistinctType
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.Unscoped
import com.ivianuu.injekt.get
import com.ivianuu.injekt.runReader

@Component(parent = RetainedActivityComponent::class)
interface ActivityComponent {
    @Component.Factory
    interface Factory {
        fun create(instance: ComponentActivity): ActivityComponent
    }
}

val ComponentActivity.activityComponent: ActivityComponent
    get() = lifecycle.singleton {
        retainedActivityComponent.runReader {
            get<ActivityComponent.Factory>().create(this)
        }
    }

@DistinctType
typealias ActivityContext = Context
@DistinctType
typealias ActivityResources = Resources
@DistinctType
typealias ActivityLifecycleOwner = LifecycleOwner
@DistinctType
typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner
@DistinctType
typealias ActivityViewModelStoreOwner = ViewModelStoreOwner

object ActivityModule {
    @Unscoped
    @Reader
    fun context(): ActivityContext = get<ComponentActivity>()

    @Unscoped
    @Reader
    fun resources(): ActivityResources = get<ActivityContext>().resources

    @Unscoped
    @Reader
    fun lifecycleOwner(): ActivityLifecycleOwner = get<ComponentActivity>()

    @Unscoped
    @Reader
    fun savedStateRegistryOwner(): ActivitySavedStateRegistryOwner = get<ComponentActivity>()

    @Unscoped
    @Reader
    fun viewModelStoreOwner(): ActivityViewModelStoreOwner = get<ComponentActivity>()
}
