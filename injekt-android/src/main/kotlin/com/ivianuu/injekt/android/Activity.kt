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
import com.ivianuu.injekt.Module

@Module
class ActivityModule<T : ComponentActivity>(@Binding val activity: T) {

    @Binding
    val T.componentActivity: ComponentActivity
        get() = this

    @Binding
    val ComponentActivity.activityContext: ActivityContext
        get() = this

    @Binding
    val ComponentActivity.activityResources: ActivityResources
        get() = resources

    @Binding
    val ComponentActivity.activityLifecycleOwner: ActivityLifecycleOwner
        get() = this

    @Binding
    val ComponentActivity.activitySavedStateRegistryOwner: ActivitySavedStateRegistryOwner
        get() = this

    @Binding
    val ComponentActivity.activityViewModelStoreOwner: ActivityViewModelStoreOwner
        get() = this
}

typealias ActivityContext = Context

typealias ActivityResources = Resources

typealias ActivityLifecycleOwner = LifecycleOwner

typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner

typealias ActivityViewModelStoreOwner = ViewModelStoreOwner
