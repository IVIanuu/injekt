package com.ivianuu.injekt.internal

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class ModuleMetadata(
    val scopes: Array<String> = [],
    val parents: Array<String> = [],
    val bindingKeys: Array<String> = [],
    val bindingProviders: Array<String> = [],
    val includes: Array<KClass<*>> = []
)
