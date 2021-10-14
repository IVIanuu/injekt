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

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.AppComponent
import com.ivianuu.injekt.common.Component
import com.ivianuu.injekt.common.EntryPoint
import com.ivianuu.injekt.common.entryPoint

/**
 * Returns the [ActivityRetainedComponent] of this [ComponentActivity]
 * whose lifecycle is bound the retained lifecycle of the activity
 */
@Suppress("UNCHECKED_CAST")
@Provide
val ComponentActivity.activityRetainedComponent: ActivityRetainedComponent
  get() = ViewModelProvider(
    this,
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ActivityRetainedComponentHolder(
          entryPoint<ActivityRetainedComponentFactoryProvider>(appComponent)
            .activityRetainedComponent()
        ) as T
    }
  )[ActivityRetainedComponentHolder::class.java].component

@Component interface ActivityRetainedComponent

@EntryPoint<AppComponent> interface ActivityRetainedComponentFactoryProvider {
  fun activityRetainedComponent(): ActivityRetainedComponent
}

private class ActivityRetainedComponentHolder(val component: ActivityRetainedComponent) : ViewModel() {
  override fun onCleared() {
    super.onCleared()
    //component.dispose()
  }
}
