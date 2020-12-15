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
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.component.ApplicationScoped
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.ComponentKey
import com.ivianuu.injekt.component.componentElement
import com.ivianuu.injekt.component.get
import com.ivianuu.injekt.component.getDependency
import com.ivianuu.injekt.given
import kotlinx.coroutines.CoroutineScope

@Given object ActivityScoped : Component.Name

@Given val @Given ComponentActivity.activityComponent: Component<ActivityScoped>
    get() = lifecycle.component {
        activityRetainedComponent[ActivityComponentFactoryKey](this)
    }

private val ActivityKey = ComponentKey<ComponentActivity>()
private val ActivityComponentFactoryKey =
    ComponentKey<(ComponentActivity) -> Component<ActivityScoped>>()

@GivenSetElement fun activityComponentFactory(
    parent: Component<ApplicationScoped> = given,
    builderFactory: () -> Component.Builder<ActivityScoped> = given,
) = componentElement(ActivityRetainedScoped, ActivityComponentFactoryKey) {
    builderFactory()
        .dependency(parent)
        .element(ActivityKey, it)
        .build()
}

@Given val @Given Component<ActivityScoped>.retainedComponentFromActivity: Component<ActivityRetainedScoped>
    get() = getDependency(ActivityRetainedScoped)

@Given val @Given Component<ActivityScoped>.activity: ComponentActivity
    get() = this[ActivityKey]

typealias ActivityContext = Context

@Given inline val @Given ComponentActivity.activityContext: ActivityContext
    get() = this

typealias ActivityResources = Resources

@Given inline val @Given ComponentActivity.activityResources: ActivityResources
    get() = resources

typealias ActivityLifecycleOwner = LifecycleOwner

@Given inline val @Given ComponentActivity.activityLifecycleOwner: ActivityLifecycleOwner
    get() = this

typealias ActivityOnBackPressedDispatcherOwner = OnBackPressedDispatcherOwner

@Given inline val @Given ComponentActivity.activityOnBackPressedDispatcherOwner: ActivityOnBackPressedDispatcherOwner
    get() = this

typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner

@Given inline val @Given ComponentActivity.activitySavedStateRegistryOwner: ActivitySavedStateRegistryOwner
    get() = this

typealias ActivityViewModelStoreOwner = ViewModelStoreOwner

@Given inline val @Given ComponentActivity.activityViewModelStoreOwner: ActivityViewModelStoreOwner
    get() = this

typealias ActivityCoroutineScope = LifecycleCoroutineScope

@Given inline val @Given ComponentActivity.activityCoroutineScope: ActivityCoroutineScope
    get() = lifecycleScope

@Given inline val @Given ActivityCoroutineScope.activityCoroutineScope: CoroutineScope
    get() = this
