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
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSet
import com.ivianuu.injekt.given

typealias ActivityComponent = Component<ActivityComponentKey<*>>

interface ActivityComponentKey<T> : Component.Key<T>

@GivenSet fun defaultActivityComponentElements(): ComponentElements<ActivityComponentKey<*>> =
    emptyMap()

@Given fun activityComponent(
    activity: ComponentActivity = given,
    activityRetainedComponent: ActivityRetainedComponent = given,
) = activity.lifecycle.component {
    activityRetainedComponent[ActivityComponentFactoryKey](activity)
}

object ActivityKey : ActivityComponentKey<ComponentActivity>

@Given fun activity(component: ActivityComponent = given): ComponentActivity =
    component[ActivityKey]

object ActivityComponentFactoryKey :
    ActivityRetainedComponentKey<(@Given ComponentActivity) -> ActivityComponent>

@GivenSet fun activityComponentFactoryKey(
    builderFactory: () -> Component.Builder<ActivityComponentKey<*>> = given,
): ComponentElements<ActivityRetainedComponentKey<*>> =
    componentElementsOf(ActivityComponentFactoryKey) {
        builderFactory()
            .set(ActivityKey, it)
            .build()
    }

typealias ActivityStorage = Storage

@Given fun activityStorage(component: ActivityComponent = given): ActivityStorage =
    component.storage

typealias ActivityContext = Context

@Given
inline fun activityContext(activity: ComponentActivity = given): ActivityContext = activity

typealias ActivityResources = Resources

@Given
inline fun activityResources(activity: ComponentActivity = given): ActivityResources =
    activity.resources

typealias ActivityLifecycleOwner = LifecycleOwner

@Given
inline fun activityLifecycleOwner(activity: ComponentActivity = given): ActivityLifecycleOwner =
    activity

typealias ActivityOnBackPressedDispatcherOwner = OnBackPressedDispatcherOwner

@Given
inline fun activityOnBackPressedDispatcherOwner(activity: ComponentActivity = given): ActivityOnBackPressedDispatcherOwner =
    activity

typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner

@Given
inline fun activitySavedStateRegistryOwner(activity: ComponentActivity = given): ActivitySavedStateRegistryOwner =
    activity

typealias ActivityViewModelStoreOwner = ViewModelStoreOwner

@Given
fun activityViewModelStoreOwner(activity: ComponentActivity = given): ActivityViewModelStoreOwner =
    activity
