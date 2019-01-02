package com.ivianuu.injekt

/**
 * A module is the container for declarations
 */
class Module internal constructor(
    val name: String? = null,
    val createOnStart: Boolean,
    val override: Boolean,
    val definition: ModuleDefinition
) {

    internal fun getDeclarations(componentContext: ComponentContext): Map<Key, Declaration<*>> {
        val moduleContext = ModuleContext(this, componentContext)
        definition.invoke(moduleContext)
        return moduleContext.declarations
    }

}