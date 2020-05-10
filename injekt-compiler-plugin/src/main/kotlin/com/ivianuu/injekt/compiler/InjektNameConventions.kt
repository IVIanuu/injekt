package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name
import kotlin.math.absoluteValue

object InjektNameConventions {
    fun getFactoryNameForClass(className: Name): Name {
        return Name.identifier("${className}\$Factory")
    }

    fun getMembersInjectorNameForClass(className: Name): Name {
        return Name.identifier("${className}\$MembersInjector")
    }

    fun getModuleClassNameForModuleFunction(moduleFunction: IrFunction): Name {
        return Name.identifier(
            "${moduleFunction.name.asString()
                .capitalize()}${moduleFunction.valueParametersHash()}\$Impl"
        )
    }

    fun getImplNameForFactoryFunction(factoryFunction: IrFunction): Name {
        return Name.identifier(
            "${factoryFunction.name.asString()
                .capitalize()}${factoryFunction.valueParametersHash()}\$Impl"
        )
    }

    fun getModuleNameForFactoryFunction(factoryFunction: IrFunction): Name =
        Name.identifier(
            "${factoryFunction.name.asString()
                .capitalize()}${factoryFunction.valueParametersHash()}\$Module"
        )

    fun classParameterNameForTypeParameter(typeParameter: IrTypeParameter): Name =
        Name.identifier("class\$${typeParameter.descriptor.name}")

    fun typeParameterNameForClassParameterName(name: Name): Name =
        Name.identifier(name.asString().removePrefix("class\$"))

    private fun IrFunction.valueParametersHash(): Int =
        valueParameters.map { it.name.asString() + it.type.render() }.hashCode()
            .absoluteValue
}
