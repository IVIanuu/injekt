package com.ivianuu.injekt.internal

@Target(AnnotationTarget.CLASS)
annotation class ModuleMetadata(
    val scopes: Array<String>,
    val parents: Array<String>,
    val bindings: Array<String>,
    val includedModules: Array<String>
)
