package com.ivianuu.injekt.internal

@Target(AnnotationTarget.CLASS)
annotation class ComponentMetadata(
    val parents: Array<String> = [],
    val parentNames: Array<String> = [],
    val bindingKeys: Array<String> = [],
    val bindingNames: Array<String> = []
)
