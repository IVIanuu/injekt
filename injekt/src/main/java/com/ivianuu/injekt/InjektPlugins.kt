package com.ivianuu.injekt

/**
 * Global configurations
 */
object InjektPlugins

/**
 * Configure injekt
 */
inline fun configureInjekt(definition: InjektPlugins.() -> Unit) {
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