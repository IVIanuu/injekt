package com.ivianuu.injekt

import com.ivianuu.injekt.internal.TypeAnnotation

@TypeAnnotation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Qualifier
annotation class Module
