@file:Suppress("UNCHECKED_CAST")

package com.ivianuu.injekt.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.lifecycleScope
import com.ivianuu.injekt.component.Component
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

private val componentsByLifecycle = mutableMapOf<Lifecycle, Component<*>>()

internal fun <T : Component<*>> Lifecycle.component(init: () -> T): T {
    componentsByLifecycle[this]?.let { return it as T }
    return synchronized(componentsByLifecycle) {
        componentsByLifecycle[this]?.let { return it as T }
        val value = init()
        componentsByLifecycle[this] = value
        value
    }.also {
        addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    // schedule clean up to the next frame
                    // to allow users to access bindings in their onDestroy()
                    source.lifecycleScope.launch(NonCancellable) {
                        synchronized(componentsByLifecycle) {
                            componentsByLifecycle
                                .remove(this@component)
                        }?.dispose()
                    }
                }
            }
        })
    }
}

internal fun <T : Component<*>> ViewModelStore.component(init: () -> T): T {
    return ViewModelProvider(
        this,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ViewModelComponentHolder(init()) as T
        }
    )[ViewModelComponentHolder::class.java].component as T
}

private class ViewModelComponentHolder<T : Component<*>>(val component: T) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        component.dispose()
    }
}
