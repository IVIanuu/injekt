package com.ivianuu.injekt.codegen

/**
 * Module declaration
 */
annotation class Module(
    val packageName: String = "",
    val moduleName: String = "",
    val internal: Boolean = false,
    val override: Boolean = false,
    val createOnStart: Boolean = false
)