package com.ivianuu.injekt.internal

@Target(AnnotationTarget.CLASS)
annotation class ModuleMetadata(
    val scopes: Array<String> = [],
    val parents: Array<String> = [],
    val parentNames: Array<String> = [],
    val bindingKeys: Array<String> = [],
    val bindingNames: Array<String> = [],
    val includedModuleTypes: Array<String> = [],
    val includedModuleNames: Array<String> = []
)
