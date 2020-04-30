package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object InjektNameConventions {
    fun getProviderNameForClass(className: FqName): FqName {
        return className.child(Name.identifier("${className.shortName()}_Provider"))
    }

    fun getModuleNameForFunction(moduleFunction: FqName): FqName {
        return moduleFunction.parent().child(Name.identifier("${moduleFunction.shortName()}_Impl"))
    }
}
