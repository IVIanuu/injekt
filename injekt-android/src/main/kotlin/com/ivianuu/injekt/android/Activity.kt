package com.ivianuu.injekt.android

import android.app.Activity
import androidx.activity.ComponentActivity
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.emptyParameters
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.internal.injektIntrinsic
import com.ivianuu.injekt.keyOf
import com.ivianuu.injekt.plus
import kotlin.reflect.KClass

val ComponentActivity.activityComponent: Component
    get() = lifecycle.getComponent {
        retainedActivityComponent
            .plus<ActivityScoped>(ActivityModule(this))
    }

fun ActivityModule(instance: ComponentActivity) = Module {
    instance(instance, Key.SimpleKey(instance::class))
    alias(Key.SimpleKey(instance::class), keyOf<Activity>())
    include(ContextModule(instance, ForActivity::class))
    include(LifecycleOwnerModule(instance, ForActivity::class))
    include(SavedStateRegistryOwnerModule(instance, ForActivity::class))
    include(ViewModelOwnerModule(instance, ForActivity::class))
}

inline fun <reified T> ComponentActivity.getLazy(
    qualifier: KClass<*>? = null,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = injektIntrinsic()

inline fun <T> ComponentActivity.getLazy(
    key: Key<T>,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { activityComponent.get(key, parameters()) }

@Scope
annotation class ActivityScoped

@Qualifier
annotation class ForActivity
