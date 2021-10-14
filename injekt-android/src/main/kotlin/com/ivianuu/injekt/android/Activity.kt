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

/**
/**
 * Returns the [ActivityScope] of this [ComponentActivity]
 * whose lifecycle is bound to the activity
 */
@Provide val ComponentActivity.activityScope: ActivityScope
  get() = synchronized(activityScopes) {
    activityScopes[this]?.let { return it }
    val scope = requireElement<@ChildScopeFactory (ComponentActivity) -> ActivityScope>(
      activityRetainedScope
    ).invoke(this)
    activityScopes[this] = scope

    lifecycle.addObserver(object : LifecycleEventObserver {
      override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
          synchronized(activityScopes) { activityScopes.remove(this@activityScope) }
          scope.dispose()
        }
      }
    })

    scope
  }

typealias ActivityScope = Scope

@Provide val activityScopeModule =
  ChildScopeModule1<ActivityRetainedScope, ComponentActivity, ActivityScope>()

private val activityScopes = mutableMapOf<ComponentActivity, ActivityScope>()
*/