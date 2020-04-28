package com.ivianuu.injekt.android

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.instance
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@PublishedApi
internal val componentsByLifecycle = ConcurrentHashMap<Lifecycle, Component>()

internal inline fun Lifecycle.component(initializer: () -> Component): Component {
    check(currentState != Lifecycle.State.DESTROYED) {
        "Cannot get component on destroyed lifecycles"
    }
    return componentsByLifecycle.getOrPut(this, initializer)
        .also {
            addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                        componentsByLifecycle -= this@component
                    }
                }
            })
        }
}

internal fun ComponentDsl.lifecycleOwner(
    instance: LifecycleOwner,
    qualifier: KClass<*>? = null
) {
    instance(qualifier = qualifier, instance = instance)
    factory(qualifier = qualifier) { instance.lifecycle }
    factory<CoroutineScope>(qualifier = qualifier) { instance.lifecycleScope }
}

internal fun ComponentDsl.viewModelStoreOwner(
    instance: ViewModelStoreOwner,
    qualifier: KClass<*>? = null
) {
    instance(qualifier = qualifier, instance = instance)
    factory(qualifier = qualifier) { instance.viewModelStore }
}

internal fun ComponentDsl.savedStateRegistryOwner(
    instance: SavedStateRegistryOwner,
    qualifier: KClass<*>? = null
) {
    instance(qualifier = qualifier, instance = instance)
    factory(qualifier = qualifier) { instance.savedStateRegistry }
}

internal fun ComponentDsl.context(
    instance: Context,
    qualifier: KClass<*>? = null
) {
    instance(qualifier = qualifier, instance = instance)
    resources(instance.resources, qualifier)
}

internal fun ComponentDsl.resources(
    instance: Resources,
    qualifier: KClass<*>? = null
) {
    instance(qualifier = qualifier, instance = instance)
}
