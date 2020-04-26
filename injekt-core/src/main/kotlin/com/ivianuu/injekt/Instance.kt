package com.ivianuu.injekt

import com.ivianuu.injekt.internal.InstanceBinding
import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

inline fun <reified T> ModuleDsl.instance(
    instance: T,
    qualifier: KClass<*>? = null
): Unit = injektIntrinsic()

/**
 * Adds the [instance] as a binding for [key]
 */
fun <T> ModuleDsl.instance(
    instance: T,
    key: Key<T>
) {
    add(key, InstanceBinding(instance))
}
