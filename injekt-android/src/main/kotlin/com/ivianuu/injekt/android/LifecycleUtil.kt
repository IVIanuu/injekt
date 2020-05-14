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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ConcurrentHashMap

private val instancesByLifecycle = ConcurrentHashMap<Lifecycle, Any>()

internal fun <T> Lifecycle.singleton(initializer: () -> T): T {
    check(currentState != Lifecycle.State.DESTROYED) {
        "Cannot store instances on destroyed lifecycles"
    }
    return instancesByLifecycle.getOrPut(this, initializer)
        .also {
            addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                        instancesByLifecycle -= this@singleton
                    }
                }
            })
        } as T
}
