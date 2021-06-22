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
import com.ivianuu.injekt.*
import com.ivianuu.injekt.ambient.*
import com.ivianuu.injekt.scope.*
import kotlin.synchronized

internal val ambientsByLifecycle = mutableMapOf<Lifecycle, Ambients>()

internal inline fun Lifecycle.cachedAmbients(init: () -> Ambients): Ambients {
  ambientsByLifecycle[this]?.let { return it }
  return synchronized(ambientsByLifecycle) {
    ambientsByLifecycle[this]?.let { return it }
    val value = init()
    ambientsByLifecycle[this] = value
    value
  }.also { ambients: @Provide Ambients ->
    addObserver(object : LifecycleEventObserver {
      override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
          synchronized(ambientsByLifecycle) { ambientsByLifecycle.remove(this@cachedAmbients) }
          (AmbientScope.current() as DisposableScope).dispose()
        }
      }
    })
  }
}

internal inline fun ViewModelStore.cachedAmbients(crossinline init: () -> Ambients): Ambients =
  ViewModelProvider(
    this,
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ValueHolder(init()) as T
    }
  )[ValueHolder::class.java].ambients

internal class ValueHolder(@Provide val ambients: Ambients) : ViewModel() {
  override fun onCleared() {
    super.onCleared()
    (AmbientScope.current() as DisposableScope).dispose()
  }
}
