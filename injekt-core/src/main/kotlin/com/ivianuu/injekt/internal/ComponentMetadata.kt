package com.ivianuu.injekt.internal

@Target(AnnotationTarget.CLASS)
annotation class ComponentMetadata(
    val bindingKeys: Array<String> = [],
    val bindingNames: Array<String> = []
)
