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

import androidx.activity.*
import androidx.lifecycle.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*

/**
 * Returns the [ActivityRetainedScope] of this [ComponentActivity]
 * whose lifecycle is bound the retained lifecycle of the activity
 */
@Suppress("UNCHECKED_CAST")
val ComponentActivity.activityRetainedScope: ActivityRetainedScope
  get() = ViewModelProvider(
    this,
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ActivityRetainedScopeHolder(
          application.appScope
            .element<@ChildScopeFactory () -> ActivityRetainedScope>()
            .invoke()
        ) as T
    }
  )[ActivityRetainedScopeHolder::class.java].scope

typealias ActivityRetainedScope = Scope

@Provide val activityRetainedScopeModule =
  ChildScopeModule0<AppScope, ActivityRetainedScope>()

private class ActivityRetainedScopeHolder(
  val scope: ActivityRetainedScope
) : ViewModel() {
  override fun onCleared() {
    super.onCleared()
    (scope as DisposableScope).dispose()
  }
}
