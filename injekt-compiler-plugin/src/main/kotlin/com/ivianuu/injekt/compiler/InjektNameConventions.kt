/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import kotlin.math.absoluteValue

object InjektNameConventions {
    fun getFactoryNameForClass(className: Name): Name = Name.identifier("${className}\$Factory")

    fun getMembersInjectorNameForClass(className: Name): Name =
        Name.identifier("${className}\$MembersInjector")

    fun getModuleClassNameForModuleFunction(moduleFunction: IrFunction): Name =
        moduleFunction.nameOrUniqueName("ModuleImpl")

    fun getImplNameForFactoryFunction(factoryFunction: IrFunction): Name =
        factoryFunction.nameOrUniqueName("FactoryImpl")

    fun getImplNameForFactoryCall(file: IrFile, call: IrCall): Name =
        Name.identifier("${file.fqName.asString().hashCode() xor call.startOffset}\$Factory")

    fun getCompositionFactoryTypeNameForCall(file: IrFile, call: IrCall): Name =
        Name.identifier("${file.fqName.asString().hashCode() xor call.startOffset}\$Type")

    fun getCompositionFactoryImplNameForCall(file: IrFile, call: IrCall): Name =
        Name.identifier("${file.fqName.asString().hashCode() xor call.startOffset}\$Factory")

    fun getModuleNameForFactoryFunction(factoryFunction: IrFunction): Name =
        factoryFunction.nameOrUniqueName("FactoryModule")

    fun classParameterNameForTypeParameter(typeParameter: IrTypeParameter): Name =
        Name.identifier("class\$${typeParameter.descriptor.name}")

    fun typeParameterNameForClassParameterName(name: Name): Name =
        Name.identifier(name.asString().removePrefix("class\$"))

    fun getCompositionElementNameForFunction(
        compositionFqName: FqName,
        moduleFunction: IrFunction
    ): Name {
        return Name.identifier(
            compositionFqName.asString()
                .replace(".", "_") + "__" + moduleFunction.descriptor.fqNameSafe.asString()
                .replace(".", "_")
        )
    }

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
