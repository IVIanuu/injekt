package com.ivianuu.injekt.android

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.keyOf
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private val componentsByLifecycle = ConcurrentHashMap<Lifecycle, Component>()

fun Lifecycle.component(initializer: () -> Component): Component {
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

@Module
internal fun lifecycleOwner(
    instanceKey: Key<out LifecycleOwner>,
    qualifier: KClass<*>? = null
) {
    alias(originalKey = instanceKey, aliasKey = keyOf(qualifier))
    factory(qualifier = qualifier) { get(instanceKey).lifecycle }
    factory<CoroutineScope>(qualifier = qualifier) { get(instanceKey).lifecycleScope }
}

@Module
internal fun viewModelStoreOwner(
    instanceKey: Key<out ViewModelStoreOwner>,
    qualifier: KClass<*>? = null
) {
    alias(originalKey = instanceKey, aliasKey = keyOf(qualifier))
    factory(qualifier = qualifier) { get(instanceKey).viewModelStore }
}

@Module
internal fun savedStateRegistryOwner(
    instanceKey: Key<out SavedStateRegistryOwner>,
    qualifier: KClass<*>? = null
) {
    alias(originalKey = instanceKey, aliasKey = keyOf(qualifier))
    factory(qualifier = qualifier) { get(instanceKey).savedStateRegistry }
}

@Module
internal fun context(
    instanceKey: Key<out Context>,
    qualifier: KClass<*>? = null
) {
    alias(originalKey = instanceKey, aliasKey = keyOf(qualifier))
    factory(qualifier) { get(instanceKey).resources!! }
}
