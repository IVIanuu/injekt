package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.properties.ReadOnlyProperty

fun interface MembersInjector<T> {
    fun inject(instance: T)
}

fun <T> Any.inject(): ReadOnlyProperty<Any?, T> = injektIntrinsic()
