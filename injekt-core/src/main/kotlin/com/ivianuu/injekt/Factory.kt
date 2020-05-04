package com.ivianuu.injekt

@Target(AnnotationTarget.FUNCTION)
annotation class Factory

@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class ChildFactory
