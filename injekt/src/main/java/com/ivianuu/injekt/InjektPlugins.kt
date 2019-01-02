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
 * Configure injekt
 */
fun configureInjekt(definition: InjektDefinition) =
    InjektPlugins.apply(definition)