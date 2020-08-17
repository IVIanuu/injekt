package com.ivianuu.injekt.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

private val lifecycleSingletons = mutableMapOf<Lifecycle, Any>()

internal fun <T : Any> Lifecycle.singleton(init: () -> T): T {
    return synchronized(lifecycleSingletons) {
        lifecycleSingletons[this]?.let { return it as T }
        val value = init()
        lifecycleSingletons[this] = value
        value
    }.also {
        addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    // schedule clean up to the next frame
                    // to allow users to access bindings in their onDestroy()
                    source.lifecycleScope.launch(Dispatchers.Main + NonCancellable) {
                        synchronized(lifecycleSingletons) {
                            lifecycleSingletons -= this@singleton
                        }
                    }
                }
            }
        })
    }
}

internal fun <T> ViewModelStore.singleton(init: () -> T): T {
    val holder = ViewModelProvider(
        this,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ViewModelContextHolder() as T
        }
    )[ViewModelContextHolder::class.java]

    var value = holder.value
    if (value == null) {
        value = init()
        holder.value = value
    }

    return value as T
}

private class ViewModelContextHolder : ViewModel() {
    var value: Any? = null
}
