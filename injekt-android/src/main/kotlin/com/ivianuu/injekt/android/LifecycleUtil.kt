package com.ivianuu.injekt.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ConcurrentHashMap

private val componentsByLifecycle = ConcurrentHashMap<Lifecycle, Any>()

internal fun <T> Lifecycle.singleton(initializer: () -> T): T {
    check(currentState != Lifecycle.State.DESTROYED) {
        "Cannot get component on destroyed lifecycles"
    }
    return componentsByLifecycle.getOrPut(this, initializer)
        .also {
            addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                        componentsByLifecycle -= this@singleton
                    }
                }
            })
        } as T
}
