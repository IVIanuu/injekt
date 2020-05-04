package com.ivianuu.injekt

@Retention(AnnotationRetention.SOURCE)
@Qualifier
@Target(AnnotationTarget.TYPE, AnnotationTarget.EXPRESSION)
annotation class MembersInjector

@Target(AnnotationTarget.PROPERTY)
annotation class Inject
