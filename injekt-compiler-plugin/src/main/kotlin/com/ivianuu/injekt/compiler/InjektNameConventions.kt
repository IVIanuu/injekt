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
import org.jetbrains.kotlin.ir.declarations.name
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
        getNameAtSourcePositionWithSuffix(file, call, "FactoryImpl")

    fun getCompositionFactoryTypeNameForCall(file: IrFile, call: IrCall): Name =
        getNameAtSourcePositionWithSuffix(file, call, "Type")

    fun getCompositionFactoryImplNameForCall(file: IrFile, call: IrCall): Name =
        getNameAtSourcePositionWithSuffix(file, call, "Factory")

    fun getObjectGraphGetNameForCall(file: IrFile, call: IrCall): Name =
        getNameAtSourcePositionWithSuffix(file, call, "Get")

    fun getObjectGraphInjectNameForCall(file: IrFile, call: IrCall): Name =
        getNameAtSourcePositionWithSuffix(file, call, "Inject")

    fun getEntryPointModuleNameForCall(file: IrFile, call: IrCall): Name =
        getNameAtSourcePositionWithSuffix(file, call, "EntryPointModule")

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
        return if (name.isSpecial) getSignatureHashNameWithSuffix(this, suffix)
        else Name.identifier("$name\$${getSignatureHashNameWithSuffix(this, suffix)}")
    }

    private fun getNameAtSourcePositionWithSuffix(
        file: IrFile,
        call: IrCall,
        suffix: String
    ) = Name.identifier("${sourceLocationHash(file, call.startOffset)}\$$suffix")

    private fun getSignatureHashNameWithSuffix(
        function: IrFunction,
        suffix: String
    ) = Name.identifier("${generateSignatureUniqueHash(function)}\$$suffix")

    private fun sourceLocationHash(file: IrFile, startOffset: Int): Int {
        var result = (file.fqName.asString() + file.name).hashCode()
        result = 31 * result + startOffset.hashCode()
        return result.absoluteValue
    }

    private fun generateSignatureUniqueHash(function: IrFunction): Int {
        var result = function.descriptor.fqNameSafe.hashCode()
        result = 31 * result + valueParametersHash(function)
        result = 31 * result + function.returnType.hashCode()
        return result.absoluteValue
    }

    private fun valueParametersHash(function: IrFunction): Int =
        function.valueParameters.map { it.name.asString() + it.type.render() }.hashCode()
            .absoluteValue

}
