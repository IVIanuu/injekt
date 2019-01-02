package com.ivianuu.injekt

/**
 * A module provides the actual dependencies
 */
class Module internal constructor(
    val name: String? = null,
    val createOnStart: Boolean,
    val override: Boolean,
    val definition: ModuleDefinition
)