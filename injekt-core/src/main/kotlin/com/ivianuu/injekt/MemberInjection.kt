package com.ivianuu.injekt

@Qualifier
@Target(AnnotationTarget.TYPE, AnnotationTarget.EXPRESSION)
annotation class MembersInjector

@Target(AnnotationTarget.PROPERTY)
annotation class Inject
