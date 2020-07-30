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

package com.ivianuu.injekt.compiler.transform.implicit

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.uniqueName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

private val nameProvider = NameProvider()

fun createContext(
    owner: IrDeclarationWithName,
    parentFunction: IrFunction?,
    pluginContext: IrPluginContext,
    symbols: InjektSymbols
) = buildClass {
    kind = ClassKind.INTERFACE
    name = nameProvider.allocateForGroup(
        getJoinedName(
            owner.getPackageFragment()!!.fqName,
            owner.descriptor.fqNameSafe
                .parent().child(owner.name.asString().asNameId())
        ).asString() + "${owner.uniqueName().hashCode()}Context"
    ).asNameId()
    visibility = Visibilities.INTERNAL
}.apply {
    parent = owner.file
    createImplicitParameterDeclarationWithWrappedDescriptor()
    addMetadataIfNotLocal()
    if (owner is IrTypeParametersContainer) copyTypeParametersFrom(owner)
    parentFunction?.let { copyTypeParametersFrom(it) }

    annotations += DeclarationIrBuilder(pluginContext, symbol)
        .irCall(symbols.context.constructors.single())

    annotations += DeclarationIrBuilder(pluginContext, symbol).run {
        irCall(symbols.name.constructors.single()).apply {
            putValueArgument(
                0,
                irString(owner.uniqueName())
            )
        }
    }
}
