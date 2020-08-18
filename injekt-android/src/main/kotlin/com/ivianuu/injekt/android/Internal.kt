package com.ivianuu.injekt.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.lifecycleScope
import com.ivianuu.injekt.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

private val lifecycleContexts = mutableMapOf<Lifecycle, Context>()

internal fun Lifecycle.scopedContext(init: () -> Context): Context {
    lifecycleContexts[this]?.let { return it }
    return synchronized(lifecycleContexts) {
        lifecycleContexts[this]?.let { return it }
        val context = init()
        lifecycleContexts[this] = context
        context
    }.also {
        addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    // schedule clean up to the next frame
                    // to allow users to access bindings in their onDestroy()
                    source.lifecycleScope.launch(Dispatchers.Main + NonCancellable) {
                        synchronized(lifecycleContexts) {
                            lifecycleContexts -= this@scopedContext
                        }
                    }
                }
            }
        })
    }
}

internal fun ViewModelStore.scopedContext(init: () -> Context): Context {
    return ViewModelProvider(
        this,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ViewModelContextHolder(init()) as T
        }
    )[ViewModelContextHolder::class.java].context
}

private class ViewModelContextHolder(
    val context: Context
) : ViewModel()
