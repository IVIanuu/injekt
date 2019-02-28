package com.ivianuu.injekt

/**
 * Global configurations
 */
object InjektPlugins

/**
 * Defines inject configuration
 */
typealias InjektDefinition = InjektPlugins.() -> Unit

/**
 * Configure injekt
 */
inline fun configureInjekt(definition: InjektDefinition) {
    InjektPlugins.apply(definition)
}

private var _logger: Logger? = null

/**
 * The logger to use
 */
var InjektPlugins.logger: Logger?
    get() = _logger
    set(value) {
        _logger = value
    }