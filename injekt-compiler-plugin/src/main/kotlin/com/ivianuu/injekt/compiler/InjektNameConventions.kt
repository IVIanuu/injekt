package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.Name

object InjektNameConventions {
    fun getProviderNameForClass(className: Name): Name {
        return Name.identifier("${className}_Provider")
    }

    fun getModuleNameForModuleFunction(moduleFunction: Name): Name {
        return Name.identifier("${moduleFunction.asString().capitalize()}_Impl")
    }

    fun getImplementationNameForFactoryFunction(factoryFunction: Name): Name {
        return Name.identifier("${factoryFunction.asString().capitalize()}_Impl")
    }

    fun getModuleNameForFactoryBlock(factoryFunction: Name): Name {
        return Name.identifier("${factoryFunction.asString().capitalize()}_Module")
    }
}
