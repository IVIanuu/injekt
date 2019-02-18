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
fun configureInjekt(definition: InjektDefinition) {
    InjektPlugins.apply(definition)
}

private var _factoryFinder: FactoryFinder = DefaultFactoryFinder()
/**
 * The factory finder
 */
var InjektPlugins.factoryFinder: FactoryFinder
    get() = _factoryFinder
    set(value) {
        _factoryFinder = value
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

private val componentExtensions = mutableSetOf<ComponentExtension>()

/**
 * Adds the [extension]
 */
fun InjektPlugins.registerComponentExtension(extension: ComponentExtension) {
    logger?.info("Registering extension: $extension")
    componentExtensions.add(extension)
}

internal fun InjektPlugins.getComponentExtensions(): Iterable<ComponentExtension> =
    componentExtensions.toList()