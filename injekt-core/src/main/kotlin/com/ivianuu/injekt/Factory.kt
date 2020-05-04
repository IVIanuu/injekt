package com.ivianuu.injekt

@Target(AnnotationTarget.FUNCTION)
annotation class Factory

// todo implement

@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class ChildFactory
