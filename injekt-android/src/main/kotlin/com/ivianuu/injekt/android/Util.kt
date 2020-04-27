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
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.instance
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@PublishedApi
internal val componentsByLifecycle = ConcurrentHashMap<Lifecycle, Component>()

internal inline fun Lifecycle.getComponent(initializer: () -> Component): Component {
    check(currentState != Lifecycle.State.DESTROYED) {
        "Cannot get component on destroyed lifecycles"
    }
    return componentsByLifecycle.getOrPut(this, initializer)
        .also {
            addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                        componentsByLifecycle -= this@getComponent
                    }
                }
            })
        }
}

internal fun LifecycleOwnerModule(
    instance: LifecycleOwner,
    qualifier: KClass<*>
) = Module {
    instance(qualifier = qualifier, instance = instance)
    factory(qualifier = qualifier) { instance.lifecycle }
    factory<CoroutineScope>(qualifier = qualifier) { instance.lifecycleScope }
}

internal fun ViewModelOwnerModule(
    instance: ViewModelStoreOwner,
    qualifier: KClass<*>
) = Module {
    instance(qualifier = qualifier, instance = instance)
    factory(qualifier = qualifier) { instance.viewModelStore }
}

internal fun SavedStateRegistryOwnerModule(
    instance: SavedStateRegistryOwner,
    qualifier: KClass<*>
) = Module {
    instance(qualifier = qualifier, instance = instance)
    factory(qualifier = qualifier) { instance.savedStateRegistry }
}

internal fun ContextModule(
    instance: Context,
    qualifier: KClass<*>
) = Module {
    instance(qualifier = qualifier, instance = instance)
    include(ResourcesModule(instance.resources, qualifier))
}

internal fun ResourcesModule(
    instance: Resources,
    qualifier: KClass<*>
) = Module {
    instance(qualifier = qualifier, instance = instance)
}
