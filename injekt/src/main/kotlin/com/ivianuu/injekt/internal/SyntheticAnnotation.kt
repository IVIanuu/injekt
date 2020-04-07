package com.ivianuu.injekt.internal

import kotlin.reflect.KClass

/** Only used in codegen */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SyntheticAnnotationMarker(val type: KClass<*>)

/** Only used in codegen */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SyntheticAnnotation
