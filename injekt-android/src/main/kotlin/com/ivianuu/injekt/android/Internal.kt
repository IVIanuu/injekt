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

@file:Suppress("UNCHECKED_CAST")

package com.ivianuu.injekt.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.lifecycleScope
import com.ivianuu.injekt.scope.GivenScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

private val givenScopesByLifecycle = mutableMapOf<Lifecycle, GivenScope>()

internal fun <T : GivenScope> Lifecycle.givenScope(init: () -> T): T {
    givenScopesByLifecycle[this]?.let { return it as T }
    return synchronized(givenScopesByLifecycle) {
        givenScopesByLifecycle[this]?.let { return it as T }
        val value = init()
        givenScopesByLifecycle[this] = value
        value
    }.also {
        addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    // schedule clean up to the next frame
                    // to allow users to access bindings in their onDestroy()
                    source.lifecycleScope.launch(NonCancellable) {
                        synchronized(givenScopesByLifecycle) {
                            givenScopesByLifecycle
                                .remove(this@givenScope)
                        }!!.dispose()
                    }
                }
            }
        })
    }
}

internal fun <T : GivenScope> ViewModelStore.givenScope(init: () -> T): T {
    return ViewModelProvider(
        this,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ViewModelGivenScopeHolder(init()) as T
        }
    )[ViewModelGivenScopeHolder::class.java].givenScope as T
}

private class ViewModelGivenScopeHolder<T : GivenScope>(val givenScope: T) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        givenScope.dispose()
    }
}
