package com.ivianuu.injekt

/**
 * Global configurations
 */
object InjektPlugins {

    private val componentExtensions = mutableSetOf<ComponentExtension>()

    /**
     * The factory finder
     */
    var factoryFinder: FactoryFinder = DefaultFactoryFinder()

    /**
     * The logger to use
     */
    var logger: Logger? = null

    /**
     * Adds the [extension]
     */
    fun addComponentExtension(extension: ComponentExtension) {
        componentExtensions.add(extension)
    }

    internal fun getComponentExtensions(): Iterable<ComponentExtension> =
        componentExtensions.toList()

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