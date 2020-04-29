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
import com.ivianuu.injekt.instanceKeyOf
import com.ivianuu.injekt.internal.injektIntrinsic
import com.ivianuu.injekt.keyOf
import com.ivianuu.injekt.plus
import kotlin.reflect.KClass

val ComponentActivity.activityComponent: Component
    get() = lifecycle.component {
        retainedActivityComponent.plus<ActivityScoped> {
            activity(this@activityComponent)
        }
    }

@Module
fun activity(instance: ComponentActivity) {
    val instanceKey = instanceKeyOf(instance)
    instance(instance, instanceKey)
    alias(instanceKey, keyOf<Activity>())
    context(instanceKey, ForActivity::class)
    lifecycleOwner(instanceKey, ForActivity::class)
    savedStateRegistryOwner(instanceKey, ForActivity::class)
    viewModelStoreOwner(instanceKey, ForActivity::class)
}

inline fun <reified T> ComponentActivity.inject(
    qualifier: KClass<*>? = null,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = injektIntrinsic()

inline fun <T> ComponentActivity.inject(
    key: Key<T>,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { activityComponent.get(key, parameters()) }

@Scope
annotation class ActivityScoped

@Qualifier
annotation class ForActivity
