package com.ivianuu.injekt

@Target(AnnotationTarget.FUNCTION)
annotation class IntoSet

@Target(AnnotationTarget.FUNCTION)
annotation class IntoMap

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class MapKey
