package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic

@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
annotation class CompositionFactory

fun <T> compositionFactoryOf(): T = injektIntrinsic()
