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

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

object InjektNameConventions {

    fun getMembersInjectorNameForClass(
        packageFqName: FqName,
        classFqName: FqName
    ): Name {
        return getJoinedName(
            packageFqName,
            classFqName.parent()
                .child("inject_${classFqName.pathSegments().joinToString("_")}")
        )
    }

    fun getFactoryNameForClass(
        packageFqName: FqName,
        classFqName: FqName
    ): Name {
        return getJoinedName(
            packageFqName,
            classFqName.parent()
                .child("create_${classFqName.pathSegments().joinToString("_")}")
        )
    }

    fun getCompositionModuleMetadataForModule(
        packageFqName: FqName,
        moduleFqName: FqName
    ): Name = getJoinedName(packageFqName, moduleFqName.child("CompositionMetadata"))

    fun getTransformedModuleFunctionNameForModule(
        packageFqName: FqName,
        moduleFqName: FqName
    ): Name = getJoinedName(packageFqName, moduleFqName)

    fun getModuleClassNameForModuleFunction(
        packageFqName: FqName,
        moduleFqName: FqName
    ): Name = getJoinedName(
        packageFqName,
        moduleFqName
            .parent()
            .child("${moduleFqName.shortName()}_Class")
    )

    fun getModuleFunctionNameForClass(moduleClass: IrClass): Name =
        moduleClass.name.asString().removeSuffix("_Class").asNameId()

    fun getBindingEffectModuleName(
        packageFqName: FqName,
        classFqName: FqName
    ): Name {
        return getJoinedName(
            packageFqName,
            classFqName.child("BindingEffect")
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
        suffix: String
    ): Name {
        return getJoinedName(
            function.getPackageFragment()!!.fqName,
            function.descriptor.fqNameSafe
                .child(suffix)
        ).let { nameWithoutIllegalChars(it.asString()) }
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
