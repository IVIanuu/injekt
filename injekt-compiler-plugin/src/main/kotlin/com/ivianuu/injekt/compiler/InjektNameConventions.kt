package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name
import kotlin.math.absoluteValue

object InjektNameConventions {
    fun getFactoryNameForClass(className: Name): Name = Name.identifier("${className}\$Factory")

    fun getMembersInjectorNameForClass(className: Name): Name =
        Name.identifier("${className}\$MembersInjector")

    fun getModuleClassNameForModuleFunction(moduleFunction: IrFunction): Name =
        moduleFunction.nameOrUniqueName("Impl")

    fun getImplNameForFactoryFunction(factoryFunction: IrFunction): Name =
        factoryFunction.nameOrUniqueName("Impl")

    fun getModuleNameForFactoryFunction(factoryFunction: IrFunction): Name =
        factoryFunction.nameOrUniqueName("Module")

    fun classParameterNameForTypeParameter(typeParameter: IrTypeParameter): Name =
        Name.identifier("class\$${typeParameter.descriptor.name}")

    fun typeParameterNameForClassParameterName(name: Name): Name =
        Name.identifier(name.asString().removePrefix("class\$"))

    private fun IrFunction.nameOrUniqueName(
        suffix: String
    ): Name {
        return Name.identifier(
            (if (name.isSpecial)
                "Lambda\$${generateSignatureUniqueHash()}"
            else "${name.asString()}\$${valueParametersHash()}") + "\$$suffix"
        )
    }

    private fun IrFunction.generateSignatureUniqueHash(): Int {
        var result = startOffset.hashCode()
        result = 31 * result + endOffset.hashCode()
        result = 31 * result + valueParametersHash()
        result = 31 * result + returnType.hashCode()
        return result.absoluteValue
    }

    private fun IrFunction.valueParametersHash(): Int =
        valueParameters.map { it.name.asString() + it.type.render() }.hashCode()
            .absoluteValue

}
