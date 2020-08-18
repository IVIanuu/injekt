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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.isSuspendFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

fun createContext(
    owner: IrDeclarationWithName,
    parentFunction: IrFunction?,
    injektContext: InjektContext,
    isReaderContext: Boolean
) = buildClass {
    kind = ClassKind.INTERFACE
    name = injektContext.uniqueClassNameProvider(
        (getJoinedName(
            owner.getPackageFragment()!!.fqName,
            owner.descriptor.fqNameSafe
                .parent().child(owner.name.asString().asNameId())
        ).asString() + "${owner.uniqueKey().hashCode()}Context")
            .asNameId(),
        owner.getPackageFragment()!!.fqName
    )
    visibility = Visibilities.INTERNAL
}.apply {
    parent = owner.file
    createImplicitParameterDeclarationWithWrappedDescriptor()
    addMetadataIfNotLocal()
    if (owner is IrTypeParametersContainer) copyTypeParametersFrom(owner)
    //parentFunction?.let { copyTypeParametersFrom(it) }
    recordLookup(this, owner)

    if (isReaderContext) {
        superTypes += injektContext.injektSymbols.context.defaultType
    }

    annotations += DeclarationIrBuilder(injektContext, symbol).run {
        if (isReaderContext) {
            irCall(injektContext.injektSymbols.readerContextMarker.constructors.single())
        } else {
            irCall(injektContext.injektSymbols.contextMarker.constructors.single())
        }
    }
}

fun IrType.isNotTransformedReaderLambda() =
    (isFunctionOrKFunction() || isSuspendFunctionOrKFunction()) && hasAnnotation(InjektFqNames.Reader)

fun IrType.isTransformedReaderLambda() =
    (isFunctionOrKFunction() || isSuspendFunctionOrKFunction()) && typeArguments
        .mapNotNull { it.typeOrNull?.classOrNull?.owner }
        .any { it.hasAnnotation(InjektFqNames.ContextMarker) }

val IrType.lambdaContext
    get() = typeArguments.firstOrNull {
        it.typeOrNull?.classOrNull?.owner?.hasAnnotation(InjektFqNames.ContextMarker) == true
    }?.typeOrFail?.classOrNull?.owner

fun IrType.hasTransformedReaderContextSubType(): Boolean =
    classOrNull?.owner?.hasAnnotation(InjektFqNames.ReaderContextMarker) == true ||
            (this is IrSimpleType && superTypes().any { it.hasTransformedReaderContextSubType() })

fun IrType.isContextSubType(processedSuperTypes: MutableSet<IrType> = mutableSetOf()): Boolean {
    if (this in processedSuperTypes) return false
    processedSuperTypes += this
    return classOrNull?.descriptor?.fqNameSafe == InjektFqNames.Context ||
            (this is IrSimpleType && superTypes().any { it.isContextSubType(processedSuperTypes) })
}

fun IrType.isNotTransformedReaderContext(): Boolean =
    isContextSubType()

fun IrType.isTransformedReaderContext(): Boolean =
    classOrNull?.owner?.hasAnnotation(InjektFqNames.ReaderContextMarker) == true &&
            isContextSubType()
