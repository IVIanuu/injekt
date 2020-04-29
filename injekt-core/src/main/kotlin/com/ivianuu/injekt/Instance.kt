package com.ivianuu.injekt

import com.ivianuu.injekt.internal.InstanceBinding
import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

@Module
inline fun <reified T> instance(
    instance: T,
    qualifier: KClass<*>? = null
): Unit = injektIntrinsic()

/**
 * Adds the [instance] as a binding for [key]
 */
@Module
fun <T> instance(
    instance: T,
    key: Key<T>
) {
    addBinding(key, InstanceBinding(instance))
}
