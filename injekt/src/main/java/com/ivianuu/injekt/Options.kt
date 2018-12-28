package com.ivianuu.injekt

/**
 * Options for [Declaration]s
 */
data class Options(
    var createOnStart: Boolean = false,
    var override: Boolean = false
)