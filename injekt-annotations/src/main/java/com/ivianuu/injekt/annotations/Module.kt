package com.ivianuu.injekt.annotations

/**
 * Module definition
 */
@Retention(AnnotationRetention.SOURCE)
annotation class Module(
    val packageName: String = "",
    val moduleName: String = "",
    val internal: Boolean = false,
    val scopeId: String = "",
    val override: Boolean = false,
    val eager: Boolean = false
)