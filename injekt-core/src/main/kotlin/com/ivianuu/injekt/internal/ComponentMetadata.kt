package com.ivianuu.injekt.internal

@Target(AnnotationTarget.CLASS)
annotation class ComponentMetadata(
    val scopes: Array<String> = [],
    val bindingKeys: Array<String> = [],
    val bindingNames: Array<String> = []
)
