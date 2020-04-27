package com.ivianuu.injekt.android

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.ivianuu.injekt.ApplicationScoped
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.ForApplication
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.emptyParameters
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.internal.injektIntrinsic
import com.ivianuu.injekt.keyOf
import kotlin.reflect.KClass

val Application.applicationComponent: Component
    get() = ProcessLifecycleOwner.get().lifecycle.getComponent {
        Component<ApplicationScoped>(ApplicationModule(this))
    }

fun ApplicationModule(instance: Application) = Module {
    instance(instance, Key.SimpleKey(instance::class))
    if (instance.javaClass != Application::class.java) {
        alias(Key.SimpleKey(instance::class), keyOf<Application>())
    }
    include(ContextModule(instance, ForApplication::class))
    include(LifecycleOwnerModule(ProcessLifecycleOwner.get(), ForApplication::class))
}

inline fun <reified T> Application.getLazy(
    qualifier: KClass<*>? = null,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = injektIntrinsic()

inline fun <T> Application.getLazy(
    key: Key<T>,
    crossinline parameters: () -> Parameters = { emptyParameters() }
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { applicationComponent.get(key, parameters()) }

@Factory
class Hall