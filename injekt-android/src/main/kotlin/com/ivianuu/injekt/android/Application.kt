package com.ivianuu.injekt.android

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ForApplication
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.emptyParameters
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.instanceKeyOf
import com.ivianuu.injekt.internal.injektIntrinsic
import com.ivianuu.injekt.keyOf
import com.ivianuu.injekt.simpleKeyOf
import kotlin.reflect.KClass

val Application.applicationComponent: Component
    get() = ProcessLifecycleOwner.get().lifecycle.component {
        Component {
            application(this@applicationComponent)
        }
    }

@Module
fun application(instance: Application) {
    val instanceKey = instanceKeyOf(instance)
    instance(instance, instanceKey)
    if (instance.javaClass != Application::class.java) {
        alias(instanceKey, keyOf())
    }
    context(instanceKey, ForApplication::class)
    val lifecycleOwnerKey = simpleKeyOf<LifecycleOwner>(ForApplication::class)
    factory(lifecycleOwnerKey) { ProcessLifecycleOwner.get() }
    lifecycleOwner(lifecycleOwnerKey, ForApplication::class)
}

inline fun <reified T> Application.inject(
    qualifier: KClass<*>? = null,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = injektIntrinsic()

inline fun <T> Application.inject(
    key: Key<T>,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { applicationComponent.get(key, parameters()) }
