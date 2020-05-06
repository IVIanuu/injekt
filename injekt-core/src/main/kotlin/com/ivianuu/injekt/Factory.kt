package com.ivianuu.injekt

@Target(AnnotationTarget.FUNCTION)
annotation class Factory

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Qualifier
annotation class ChildFactory
