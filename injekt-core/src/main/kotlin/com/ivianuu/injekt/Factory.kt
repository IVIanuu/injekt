package com.ivianuu.injekt

@Target(AnnotationTarget.TYPEALIAS)
annotation class RootFactory

@Target(AnnotationTarget.TYPEALIAS)
annotation class ChildFactory

fun <T> rootFactory(): T = error("Must be compiled with the Injekt compiler")
