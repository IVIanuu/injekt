package com.ivianuu.injekt.internal

@Target(AnnotationTarget.CLASS)
annotation class ComponentMetadata(
    val scopes: Array<String> = [],
    val parents: Array<String> = [],
    val bindings: Array<String> = []
)
