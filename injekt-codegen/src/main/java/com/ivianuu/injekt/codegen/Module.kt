package com.ivianuu.injekt.codegen

/**
 * Module declaration
 */
@Retention(AnnotationRetention.SOURCE)
annotation class Module(
    val packageName: String = "",
    val moduleName: String = "",
    val internal: Boolean = false,
    val override: Boolean = false,
    val createOnStart: Boolean = false
)