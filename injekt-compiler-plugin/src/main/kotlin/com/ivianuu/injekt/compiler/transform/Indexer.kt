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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.addFile
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class Indexer(
    private val pluginContext: IrPluginContext,
    private val module: IrModuleFragment,
    private val symbols: InjektSymbols
) {

    private val externalIndices by lazy {
        val memberScope = pluginContext.moduleDescriptor.getPackage(InjektFqNames.IndexPackage)
            .memberScope

        (memberScope.getClassifierNames() ?: emptySet())
            .mapNotNull {
                memberScope.getContributedClassifier(
                    it,
                    NoLookupLocation.FROM_BACKEND
                )
            }
            .map { pluginContext.referenceClass(it.fqNameSafe)!!.owner }
            .map { it.toIndex() }
            .distinct()
    }

    private val internalIndices by lazy {
        (module.files
            .filter { it.fqName == InjektFqNames.IndexPackage }
            .flatMapFix { it.declarations }
            .filterIsInstance<IrClass>())
            .map { it.toIndex() }
            .distinct()
    }

    private fun IrClass.toIndex(): Index {
        return Index(
            FqName(getConstantFromAnnotationOrNull<String>(InjektFqNames.Index, 0)!!),
            getConstantFromAnnotationOrNull<String>(InjektFqNames.Index, 1)!!
        )
    }

    val classIndices by lazy { internalClassIndices + externalClassIndices }

    private val internalClassIndices by lazy {
        internalIndices.mapNotNull { it.toClassOrNull() }
    }

    val externalClassIndices by lazy {
        externalIndices
            .mapNotNull { it.toClassOrNull() }
    }

    private fun Index.toClassOrNull() =
        takeIf { it.type == "class" }
            ?.let { pluginContext.referenceClass(it.fqName) }
            ?.owner

    val functionIndices by lazy {
        (externalIndices + internalIndices)
            .filter { it.type == "function" }
            .flatMapFix { pluginContext.referenceFunctions(it.fqName) }
            .map { it.owner }
    }

    val propertyIndices: List<IrProperty> by lazy {
        (externalIndices + internalIndices)
            .filter { it.type == "property" }
            .flatMapFix { pluginContext.referenceProperties(it.fqName) }
            .map { it.owner }
    }

    private data class Index(
        val fqName: FqName,
        val type: String
    )

    fun index(declaration: IrDeclarationWithName) {
        val name = (getJoinedName(
            declaration.getPackageFragment()!!.fqName,
            declaration.descriptor.fqNameSafe
                .parent().child(declaration.name.asString().asNameId())
        ).asString() + "${declaration.uniqueKey().hashCode()}Index")
            .removeIllegalChars()
            .asNameId()
        module.addFile(
            pluginContext,
            InjektFqNames.IndexPackage
                .child(name)
        ).apply {
            addChild(
                buildClass {
                    this.name = name
                    kind = ClassKind.INTERFACE
                    visibility = Visibilities.INTERNAL
                }.apply {
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                    addMetadataIfNotLocal()
                    annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                        irCall(symbols.index.constructors.single()).apply {
                            putValueArgument(
                                0,
                                irString(declaration.descriptor.fqNameSafe.asString())
                            )
                            putValueArgument(
                                1,
                                irString(
                                    when (declaration) {
                                        is IrClass -> "class"
                                        is IrFunction -> "function"
                                        is IrProperty -> "property"
                                        else -> error("Unsupported declaration ${declaration.render()}")
                                    }
                                )
                            )
                        }
                    }
                }
            )
        }
    }

}
