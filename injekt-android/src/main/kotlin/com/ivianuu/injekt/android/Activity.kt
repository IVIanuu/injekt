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

import android.content.res.Resources
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Context
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.childContext
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader

@Context
interface ActivityContext

val ComponentActivity.activityContext: ActivityContext
    get() = lifecycle.singleton {
        retainedActivityContext.runReader {
            childContext(this)
        }
    }

typealias ActivityAndroidContext = android.content.Context

typealias ActivityResources = Resources

typealias ActivityLifecycleOwner = LifecycleOwner

typealias ActivitySavedStateRegistryOwner = SavedStateRegistryOwner

typealias ActivityViewModelStoreOwner = ViewModelStoreOwner

object ActivityGivens {

    @Given
    fun context(): ActivityAndroidContext = given<ComponentActivity>()

    @Given
    fun resources(): ActivityResources = given<android.content.Context>().resources

    @Given
    fun lifecycleOwner(): ActivityLifecycleOwner = given<ComponentActivity>()

    @Given
    fun savedStateRegistryOwner(): ActivitySavedStateRegistryOwner = given<ComponentActivity>()

    @Given
    fun viewModelStoreOwner(): ActivityViewModelStoreOwner = given<ComponentActivity>()

}
