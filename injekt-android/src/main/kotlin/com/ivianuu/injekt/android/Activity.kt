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
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Distinct
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader
import com.ivianuu.injekt.subcomponent

@Component
interface ActivityComponent

val ComponentActivity.activityComponent: ActivityComponent
    get() = lifecycle.singleton {
        retainedActivityComponent.runReader {
            subcomponent(this)
        }
    }

@Distinct
typealias ActivityContext = Context

@Distinct
typealias ActivityResources = Resources

@Distinct
typealias ActivityLifecycleOwner = LifecycleOwner

@Distinct
typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner

@Distinct
typealias ActivityViewModelStoreOwner = ViewModelStoreOwner

object ActivityModule {

    @Given
    @Reader
    fun context(): ActivityContext = given<ComponentActivity>()

    @Given
    @Reader
    fun resources(): ActivityResources = given<ActivityContext>().resources

    @Given
    @Reader
    fun lifecycleOwner(): ActivityLifecycleOwner = given<ComponentActivity>()

    @Given
    @Reader
    fun savedStateRegistryOwner(): ActivitySavedStateRegistryOwner = given<ComponentActivity>()

    @Given
    @Reader
    fun viewModelStoreOwner(): ActivityViewModelStoreOwner = given<ComponentActivity>()

}
