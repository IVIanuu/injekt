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

@file:Suppress("UNCHECKED_CAST")

package com.ivianuu.injekt.android

import androidx.lifecycle.*
import com.ivianuu.injekt.scope.*

internal val scopesByLifecycle = mutableMapOf<Lifecycle, Scope>()

internal inline fun <T : Scope> Lifecycle.scope(init: () -> T): T {
  scopesByLifecycle[this]?.let { return it as T }
  return synchronized(scopesByLifecycle) {
    scopesByLifecycle[this]?.let { return it as T }
    val value = init()
    scopesByLifecycle[this] = value
    value
  }.also {
    addObserver(object : LifecycleEventObserver {
      override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
          synchronized(scopesByLifecycle) {
            scopesByLifecycle
              .remove(this@scope)
          }!!.dispose()
        }
      }
    })
  }
}

internal inline fun <T : Scope> ViewModelStore.scope(crossinline init: () -> T): T {
  return ViewModelProvider(
    this,
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ScopeHolder(init()) as T
    }
  )[ScopeHolder::class.java].scope as T
}

internal class ScopeHolder<T : Scope>(val scope: T) : ViewModel() {
  override fun onCleared() {
    super.onCleared()
    scope.dispose()
  }
}
