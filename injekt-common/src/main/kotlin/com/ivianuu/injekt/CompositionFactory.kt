package com.ivianuu.injekt

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
annotation class CompositionFactory

inline fun <reified T, reified F> compositionFactoryOf(): F =
    CompositionFactories.get(T::class)
