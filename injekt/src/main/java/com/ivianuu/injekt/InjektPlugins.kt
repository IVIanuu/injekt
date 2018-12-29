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
fun configureInjekt(configuration: InjektConfiguration) =
    InjektPlugins.apply(configuration)