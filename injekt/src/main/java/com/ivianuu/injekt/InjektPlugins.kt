package com.ivianuu.injekt

/**
 * Global configurations
 */
object InjektPlugins {

    /**
     * The logger to use
     */
    var logger: Logger? = null

}

/**
 * Defines inject configuration
 */
typealias InjektDefinition = InjektPlugins.() -> Unit

/**
 * Configure injekt
 */
fun configureInjekt(definition: InjektDefinition) {
    InjektPlugins.apply(definition)
}