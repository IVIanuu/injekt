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

import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

object InjektNameConventions {

    fun getFactoryNameForClass(
        packageFqName: FqName,
        classFqName: FqName
    ): Name {
        return getJoinedName(
            packageFqName,
            classFqName.child("BindingFactory")
        )
    }

    fun getMembersInjectorNameForClass(
        packageFqName: FqName,
        classFqName: FqName
    ): Name {
        return getJoinedName(
            packageFqName,
            classFqName.child("MembersInjector")
        )
    }

    fun getModuleClassNameForModuleFunction(moduleFunction: IrFunction): Name =
        getUniqueNameForFunctionWithSuffix(moduleFunction, "Impl")

    fun getClassImplNameForFactoryFunction(factoryFunction: IrFunction): Name =
        getUniqueNameForFunctionWithSuffix(factoryFunction, "Impl")

    fun getFunctionImplNameForFactoryCall(file: IrFile, call: IrCall): Name =
        getNameAtSourcePositionWithSuffix(file, call, "Impl")

    fun getBindingAdapterModuleName(
        packageFqName: FqName,
        classFqName: FqName
    ): Name {
        return getJoinedName(
            packageFqName,
            classFqName.child("BindingAdapter")
        )
    }

    fun getCompositionFactoryTypeNameForCall(
        file: IrFile,
        call: IrCall,
        factoryFunction: IrFunctionSymbol
    ): Name {
        return getNameAtSourcePositionWithSuffix(
            file, call,
            factoryFunction.descriptor.name.asString() + "_Type"
        )
    }

    fun getCompositionFactoryImplNameForCall(
        file: IrFile,
        call: IrCall,
        factoryFunction: IrFunctionSymbol,
        childFactory: Boolean
    ): Name {
        return if (childFactory) {
            (factoryFunction.descriptor.name.asString() + "_Factory").asNameId()
        } else {
            getNameAtSourcePositionWithSuffix(
                file, call,
                factoryFunction.descriptor.name.asString() + "_Factory"
            )
        }
    }

    fun getObjectGraphGetNameForCall(file: IrFile, call: IrCall): Name =
        getNameAtSourcePositionWithSuffix(file, call, "Get")

    fun getObjectGraphInjectNameForCall(file: IrFile, call: IrCall): Name =
        getNameAtSourcePositionWithSuffix(file, call, "Inject")

    fun getEntryPointModuleNameForCall(file: IrFile, call: IrCall): Name =
        getNameAtSourcePositionWithSuffix(file, call, "EntryPointModule")

    fun getModuleNameForFactoryFunction(
        factoryFunction: IrFunction
    ): Name {
        return getUniqueNameForFunctionWithSuffix(
            factoryFunction,
            "FactoryModule"
        )
    }

    fun classParameterNameForTypeParameter(typeParameter: IrTypeParameter): Name =
        "class\$${typeParameter.descriptor.name}".asNameId()

    fun typeParameterNameForClassParameterName(name: Name): Name =
        name.asString().removePrefix("class\$").asNameId()

    fun getCompositionElementNameForFunction(
        compositionFqName: FqName,
        moduleFunction: IrFunction
    ): Name {
        return (compositionFqName.asString()
            .replace(".", "__") + "___" + moduleFunction.descriptor.fqNameSafe.asString()
            .replace(".", "__"))
            .asNameId()
    }

    fun nameWithoutIllegalChars(name: String): Name = name
        .replace(".", "")
        .replace("<", "")
        .replace(">", "")
        .replace(" ", "")
        .replace("[", "")
        .replace("]", "")
        .replace("@", "")
        .replace(",", "")
        .asNameId()

    private fun getUniqueNameForFunctionWithSuffix(
        function: IrFunction,
        suffix: String,
        hashValueParameters: Boolean = true
    ): Name {
        return getJoinedName(
            function.getPackageFragment()!!.fqName,
            function.descriptor.fqNameSafe
                .child(valueParametersHash(function).toString())
                .child(suffix)
        ).let { nameWithoutIllegalChars(it.asString()) }
    }

    fun getChildFactoryImplName(fqName: String): Name {
        return (fqName
            .split(".")
            .last() + "_FactoryImpl")
            .asNameId()
    }

    private fun getNameAtSourcePositionWithSuffix(
        file: IrFile,
        call: IrCall,
        suffix: String
    ): Name {
        return getJoinedName(
            file.fqName,
            file.fqName
                .child(file.name.replace(".kt", ""))
                .child(call.startOffset.toString())
                .child(suffix)
        )
    }

    private fun valueParametersHash(function: IrFunction): Int {
        return function.allParameters
            .map { it.getParameterName() }
            .hashCode()
    }

    private fun getJoinedName(
        packageFqName: FqName,
        fqName: FqName
    ): Name {
        val joinedSegments = fqName.asString()
            .removePrefix(packageFqName.asString() + ".")
            .split(".")
        return joinedSegments.joinToString("_").asNameId()
    }

}
