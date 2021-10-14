package com.ivianuu.injekt.common

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Component

interface ComponentObserver<C : @Component Any> {
  fun onInit(component: C) {
  }

  fun onDispose(component: C) {
  }
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Scoped<C : @Component Any>
