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

package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.IrFileStore
import com.ivianuu.injekt.compiler.unsafeLazy
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given(IrContext::class)
class Indexer {

    private val fileStore = given<IrFileStore>()

    val classIndices by unsafeLazy {
        val internalClasses = internalDeclarationsByIndices.keys
            .filter { it.type == "class" }
            .map { internalDeclarationsByIndices[it]!! as IrClass }

        (internalClasses + externalClassIndices)
            .distinct()
    }

    val externalClassIndices by unsafeLazy {
        allExternalIndices
            .filter { it.type == "class" }
            .mapNotNull { index ->
                if (index.indexIsDeclaration) index.indexClass
                else pluginContext.referenceClass(index.fqName)?.owner
            }
    }

    val functionIndices by unsafeLazy {
        val internalFunctions = internalDeclarationsByIndices.keys
            .filter { it.type == "function" }
            .map { internalDeclarationsByIndices[it]!! }
            .filterIsInstance<IrFunction>()

        val externalFunctions = allExternalIndices
            .filter { it.type == "function" }
            .flatMap { index ->
                pluginContext.referenceFunctions(index.fqName)
                    .map { it.owner }
            }

        (internalFunctions + externalFunctions)
            .distinct()
    }

    val propertyIndices by unsafeLazy {
        val internalProperties = internalDeclarationsByIndices.keys
            .filter { it.type == "property" }
            .map { internalDeclarationsByIndices[it]!! }
            .filterIsInstance<IrProperty>()

        val externalProperties = allExternalIndices
            .filter { it.type == "property" }
            .flatMap { index ->
                pluginContext.referenceProperties(index.fqName)
                    .map { it.owner }
            }

        (internalProperties + externalProperties)
            .distinct()
    }

    private val allExternalIndices by unsafeLazy {
        val memberScope = irModule.descriptor.getPackage(InjektFqNames.IndexPackage).memberScope
        (memberScope.getClassifierNames() ?: emptySet())
            .mapNotNull {
                memberScope.getContributedClassifier(
                    it,
                    NoLookupLocation.FROM_BACKEND
                )
            }
            .mapNotNull { pluginContext.referenceClass(it.fqNameSafe)?.owner }
            .map {
                Index(
                    FqName(
                        it.getConstantFromAnnotationOrNull<String>(
                            InjektFqNames.Index,
                            1
                        )!!
                    ),
                    it,
                    it.getConstantFromAnnotationOrNull<String>(InjektFqNames.Index, 0)!!,
                    it.getConstantFromAnnotationOrNull<Boolean>(InjektFqNames.Index, 2)!!
                )
            }
    }

    private data class Index(
        val fqName: FqName,
        val indexClass: IrClass,
        val type: String,
        val indexIsDeclaration: Boolean
    )

    private val internalDeclarationsByIndices = mutableMapOf<Index, IrDeclaration>()

    fun index(
        originatingDeclaration: IrDeclarationWithName,
        originatingFile: IrFile,
        name: Name = originatingDeclaration.name.asString().hashCode().toString().asNameId(),
        classBuilder: IrClass.() -> Unit
    ) {
        val name = (getJoinedName(
            originatingDeclaration.getPackageFragment()!!.fqName,
            originatingDeclaration.descriptor.fqNameSafe
                .parent().child(originatingDeclaration.name.asString().asNameId())
        ).asString() + "$name${originatingDeclaration.uniqueKey().hashCode()}Index")
            .removeIllegalChars()
            .asNameId()

        val indexFilePath = fileStore.get(originatingFile.path)!!
        val indexFile = irModule.files.single { it.path == indexFilePath }
        indexFile.addChildAndUpdateMetadata(
            buildClass {
                this.name = name
                kind = ClassKind.INTERFACE
                visibility = Visibilities.INTERNAL
            }.apply {
                parent = indexFile
                createImplicitParameterDeclarationWithWrappedDescriptor()
                addMetadataIfNotLocal()
                val index = Index(
                    descriptor.fqNameSafe,
                    this,
                    "class",
                    true
                )
                annotations += irBuilder().run {
                    irCall(injektSymbols.index.constructors.single()).apply {
                        putValueArgument(
                            0,
                            irString(index.type)
                        )
                        putValueArgument(
                            1,
                            irString(index.fqName.asString())
                        )
                        putValueArgument(
                            2,
                            irBoolean(index.indexIsDeclaration)
                        )
                    }
                }

                classBuilder()
                internalDeclarationsByIndices[index] = this
            }
        )
    }

    fun index(
        declaration: IrDeclarationWithName,
        originatingFile: IrFile
    ) {
        val name = (getJoinedName(
            declaration.getPackageFragment()!!.fqName,
            declaration.descriptor.fqNameSafe
                .parent().child(declaration.name.asString().asNameId())
        ).asString() + "${declaration.uniqueKey().hashCode()}Index").removeIllegalChars().asNameId()

        val indexFilePath = fileStore.get(originatingFile.path)!!
        val indexFile = irModule.files.singleOrNull { it.path == indexFilePath }
            ?: error("Not found for ${originatingFile.path} index is $indexFilePath in ${irModule.files.map { it.path }}")
        indexFile.addChildAndUpdateMetadata(
            buildClass {
                this.name = name
                kind = ClassKind.INTERFACE
                visibility = Visibilities.INTERNAL
            }.apply {
                parent = indexFile
                val index = Index(
                    declaration.descriptor.fqNameSafe,
                    this,
                    when (declaration) {
                        is IrClass -> "class"
                        is IrFunction -> "function"
                        is IrProperty -> "property"
                        else -> error("Unsupported declaration ${declaration.render()}")
                    },
                    false
                )
                internalDeclarationsByIndices[index] = declaration

                createImplicitParameterDeclarationWithWrappedDescriptor()
                addMetadataIfNotLocal()
                annotations += irBuilder().run {
                    irCall(injektSymbols.index.constructors.single()).apply {
                        putValueArgument(
                            0,
                            irString(index.type)
                        )
                        putValueArgument(
                            1,
                            irString(index.fqName.asString())
                        )
                        putValueArgument(
                            2,
                            irBoolean(index.indexIsDeclaration)
                        )
                    }
                }
            }
        )
    }

}
