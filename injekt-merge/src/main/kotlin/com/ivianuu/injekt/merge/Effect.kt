package com.ivianuu.injekt.merge

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Effect(val installIn: KClass<*>)
