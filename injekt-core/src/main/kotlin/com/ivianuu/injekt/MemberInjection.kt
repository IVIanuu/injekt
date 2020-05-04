package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.properties.ReadOnlyProperty

// todo implement

@Retention(AnnotationRetention.SOURCE)
@Qualifier
@Target(AnnotationTarget.TYPE, AnnotationTarget.EXPRESSION)
annotation class MembersInjector

fun <T> Any.inject(): ReadOnlyProperty<Any?, T> = injektIntrinsic()
