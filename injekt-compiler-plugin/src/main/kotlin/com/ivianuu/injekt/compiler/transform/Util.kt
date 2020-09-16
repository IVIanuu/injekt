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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.addChildAndUpdateMetadata
import com.ivianuu.injekt.compiler.addFile
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.remapTypeParametersByName
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

fun createContext(
    owner: IrDeclarationWithName,
    origin: FqName,
    parentTypeParametersContainer: IrTypeParametersContainer?,
    pluginContext: IrPluginContext,
    module: IrModuleFragment,
    injektSymbols: InjektSymbols
) = buildClass {
    kind = ClassKind.INTERFACE
    name = (getJoinedName(
        owner.getPackageFragment()!!.fqName,
        owner.descriptor.fqNameSafe
            .parent().child(owner.name.asString().asNameId())
    ).asString().removeIllegalChars() + "${owner.uniqueKey().hashCode()}Context")
        .removeIllegalChars()
        .asNameId()
    visibility = Visibilities.INTERNAL
}.apply {
    module.addFile(
        pluginContext,
        owner.getPackageFragment()!!.fqName
            .child(name)
    ).also { it.addChildAndUpdateMetadata(this) }
    createImplicitParameterDeclarationWithWrappedDescriptor()
    addMetadataIfNotLocal()
    if (owner is IrTypeParametersContainer) copyTypeParametersFrom(owner)
    parentTypeParametersContainer?.let { copyTypeParametersFrom(it) }
    recordLookup(parent, owner)

    annotations += DeclarationIrBuilder(pluginContext, symbol)
        .irCall(injektSymbols.contextMarker.constructors.single())
    annotations += DeclarationIrBuilder(pluginContext, symbol).run {
        irCall(injektSymbols.origin.constructors.single()).apply {
            putValueArgument(0, irString(origin.asString()))
        }
    }
}

fun createContextFactory(
    contextType: IrType,
    typeParametersContainer: IrTypeParametersContainer?,
    file: IrFile,
    inputTypes: List<IrType>,
    startOffset: Int,
    pluginContext: IrPluginContext,
    module: IrModuleFragment,
    injektSymbols: InjektSymbols,
    isChild: Boolean
) = buildClass {
    name = "${contextType.classOrNull!!.owner.name}${startOffset}Factory"
        .removeIllegalChars().asNameId()
    kind = ClassKind.INTERFACE
    visibility = Visibilities.INTERNAL
}.apply clazz@{
    module.addFile(
        pluginContext,
        file.fqName
            .child(name)
    ).also { it.addChildAndUpdateMetadata(this) }
    createImplicitParameterDeclarationWithWrappedDescriptor()
    addMetadataIfNotLocal()
    recordLookup(parent, contextType.classOrNull!!.owner)

    if (typeParametersContainer != null)
        copyTypeParametersFrom(typeParametersContainer)

    addFunction {
        this.name = "create".asNameId()
        returnType = contextType
            .remapTypeParametersByName(typeParametersContainer ?: this@clazz, this@clazz)
        modality = Modality.ABSTRACT
    }.apply {
        dispatchReceiverParameter = thisReceiver!!.copyTo(this)
        parent = this@clazz
        addMetadataIfNotLocal()
        val parameterUniqueNameProvider = UniqueNameProvider()
        inputTypes
            .map { it.remapTypeParametersByName(typeParametersContainer ?: this@clazz, this@clazz) }
            .forEach {
                addValueParameter(
                    parameterUniqueNameProvider(it.uniqueTypeName().asString()),
                    it
                )
            }
    }

    annotations += DeclarationIrBuilder(pluginContext, symbol).run {
        if (!isChild) {
            irCall(injektSymbols.rootContextFactory.constructors.single()).apply {
                putValueArgument(
                    0,
                    irString(
                        file.fqName.child((name.asString() + "Impl").asNameId()).asString()
                    )
                )
            }
        } else {
            irCall(injektSymbols.childContextFactory.constructors.single())
        }
    }
}
