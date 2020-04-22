package com.ivianuu.injekt

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class KindMarker

interface Kind {
    fun <T> apply(provider: Provider<T>): Provider<T>
}
