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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.ivianuu.injekt.common.Component
import com.ivianuu.injekt.common.EntryPoint
import com.ivianuu.injekt.common.dispose
import com.ivianuu.injekt.common.entryPoint

/**
 * Returns the [ActivityComponent] of this [ComponentActivity]
 * whose lifecycle is bound to the activity
 */
val ComponentActivity.activityComponent: ActivityComponent
  get() = synchronized(activityComponents) {
    activityComponents[this]?.let { return it }
    val component = entryPoint<ActivityComponentFactory>(activityRetainedComponent)
      .activityComponent(this)
    activityComponents[this] = component

    lifecycle.addObserver(object : LifecycleEventObserver {
      override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
          synchronized(activityComponents) { activityComponents.remove(this@activityComponent) }
          component.dispose()
        }
      }
    })

    component
  }

@Component interface ActivityComponent

@EntryPoint<ActivityRetainedComponent> interface ActivityComponentFactory {
  fun activityComponent(activity: ComponentActivity): ActivityComponent
}

private val activityComponents = mutableMapOf<ComponentActivity, ActivityComponent>()
