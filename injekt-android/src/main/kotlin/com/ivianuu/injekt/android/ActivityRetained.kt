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
import com.ivianuu.injekt.container.*

/**
 * Returns the [Container] for [ActivityRetainedScope] of this [ComponentActivity]
 * whose lifecycle is bound the retained lifecycle of the activity
 */
@Suppress("UNCHECKED_CAST")
val ComponentActivity.activityRetainedContainer: Container<ActivityRetainedScope>
  get() = ViewModelProvider(
    this,
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ActivityRetainedContainerHolder(
          application.appContainer
            .element<@ChildContainerFactory () -> Container<ActivityRetainedScope>>()
            .invoke()
        ) as T
    }
  )[ActivityRetainedContainerHolder::class.java].container

abstract class ActivityRetainedScope private constructor()

@Provide val activityRetainedContainerModule =
  ChildContainerModule0<AppScope, ActivityRetainedScope>()

private class ActivityRetainedContainerHolder(
  val container: Container<ActivityRetainedScope>
) : ViewModel() {
  override fun onCleared() {
    super.onCleared()
    container.dispose()
  }
}
