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
import com.ivianuu.injekt.compiler.addChildAndUpdateMetadata
import com.ivianuu.injekt.compiler.addFile
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class Indexer(
    private val pluginContext: IrPluginContext,
    private val module: IrModuleFragment,
    private val symbols: InjektSymbols
) {

    private val classIndicesByTagAndKey = mutableMapOf<List<String>, List<IrClass>>()
    fun classIndices(path: List<String>): List<IrClass> {
        val finalPath = path.map { it.removeIllegalChars() }
        return classIndicesByTagAndKey.getOrPut(finalPath) {
            val internalClasses = internalDeclarationsByIndices.keys
                .filter { it.path == finalPath && it.type == "class" }
                .map { internalDeclarationsByIndices[it]!! as IrClass }

            (internalClasses + externalClassIndices(path))
                .distinct()
        }
    }

    private val externalClassIndicesByTagAndKey = mutableMapOf<List<String>, List<IrClass>>()
    fun externalClassIndices(path: List<String>): List<IrClass> {
        val finalPath = path.map { it.removeIllegalChars() }
        return externalClassIndicesByTagAndKey.getOrPut(finalPath) {
            externalIndicesByTagAndKey(path)
                .filter { it.type == "class" }
                .mapNotNull { index ->
                    if (index.indexIsDeclaration) index.indexClass
                    else pluginContext.referenceClass(index.fqName)?.owner
                }
        }
    }

    private val functionIndicesByTagAndKey = mutableMapOf<List<String>, List<IrFunction>>()
    fun functionIndices(path: List<String>): List<IrFunction> {
        val finalPath = path.map { it.removeIllegalChars() }
        return functionIndicesByTagAndKey.getOrPut(finalPath) {
            val internalFunctions = internalDeclarationsByIndices.keys
                .filter { it.path == finalPath && it.type == "function" }
                .map { internalDeclarationsByIndices[it]!! }
                .filterIsInstance<IrFunction>()

            val externalFunctions = externalIndicesByTagAndKey(path)
                .filter { it.type == "function" }
                .flatMap { index ->
                    pluginContext.referenceFunctions(index.fqName)
                        .map { it.owner }
                }

            (internalFunctions + externalFunctions)
                .distinct()
        }
    }

    private val propertyIndicesByTagAndKey = mutableMapOf<List<String>, List<IrProperty>>()
    fun propertyIndices(path: List<String>): List<IrProperty> {
        val finalPath = path.map { it.removeIllegalChars() }
        return propertyIndicesByTagAndKey.getOrPut(finalPath) {
            val internalProperties = internalDeclarationsByIndices.keys
                .filter { it.path == finalPath && it.type == "property" }
                .map { internalDeclarationsByIndices[it]!! }
                .filterIsInstance<IrProperty>()

            val externalProperties = externalIndicesByTagAndKey(path)
                .filter { it.type == "property" }
                .flatMap { index ->
                    pluginContext.referenceProperties(index.fqName)
                        .map { it.owner }
                }

            (internalProperties + externalProperties)
                .distinct()
        }
    }

    private val externalIndicesByTagAndKey = mutableMapOf<List<String>, List<Index>>()
    private fun externalIndicesByTagAndKey(path: List<String>): List<Index> {
        val finalPath = path.map { it.removeIllegalChars() }
        return externalIndicesByTagAndKey.getOrPut(finalPath) {
            var packageFqName = InjektFqNames.IndexPackage
            finalPath.forEach { packageFqName = packageFqName.child(it.asNameId()) }
            val memberScope = module.descriptor.getPackage(packageFqName).memberScope
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
                        path,
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
    }

    private data class Index(
        val path: List<String>,
        val fqName: FqName,
        val indexClass: IrClass,
        val type: String,
        val indexIsDeclaration: Boolean
    )

    private val internalDeclarationsByIndices = mutableMapOf<Index, IrDeclaration>()

    fun index(
        originatingDeclaration: IrDeclarationWithName,
        name: Name = originatingDeclaration.name.asString().hashCode().toString().asNameId(),
        path: List<String>,
        classBuilder: IrClass.() -> Unit
    ) {
        val finalPath = path.map { it.removeIllegalChars() }
        var packageFqName = InjektFqNames.IndexPackage
        finalPath.forEach { packageFqName = packageFqName.child(it.asNameId()) }

        val name = (getJoinedName(
            originatingDeclaration.getPackageFragment()!!.fqName,
            originatingDeclaration.descriptor.fqNameSafe
                .parent().child(originatingDeclaration.name.asString().asNameId())
        ).asString() + "$name${originatingDeclaration.uniqueKey().hashCode()}Index")
            .removeIllegalChars()
            .asNameId()

        module.addFile(
            pluginContext,
            packageFqName
                .child(name)
        ).apply file@{
            recordLookup(this, originatingDeclaration)
            addChildAndUpdateMetadata(
                buildClass {
                    this.name = name
                    kind = ClassKind.INTERFACE
                    visibility = Visibilities.INTERNAL
                }.apply {
                    parent = this@file
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                    addMetadataIfNotLocal()
                    val index = Index(
                        finalPath,
                        descriptor.fqNameSafe,
                        this,
                        "class",
                        true
                    )
                    annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                        irCall(symbols.index.constructors.single()).apply {
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
    }

    fun index(
        path: List<String>,
        declaration: IrDeclarationWithName
    ) {
        val finalPath = path.map { it.removeIllegalChars() }
        var packageFqName = InjektFqNames.IndexPackage
        finalPath.forEach { packageFqName = packageFqName.child(it.asNameId()) }

        val name = (getJoinedName(
            declaration.getPackageFragment()!!.fqName,
            declaration.descriptor.fqNameSafe
                .parent().child(declaration.name.asString().asNameId())
        ).asString() + "${declaration.uniqueKey().hashCode()}Index").removeIllegalChars().asNameId()

        module.addFile(
            pluginContext,
            packageFqName.child(name)
        ).apply file@{
            recordLookup(this, declaration)
            addChildAndUpdateMetadata(
                buildClass {
                    this.name = name
                    kind = ClassKind.INTERFACE
                    visibility = Visibilities.INTERNAL
                }.apply {
                    parent = this@file
                    val index = Index(
                        finalPath,
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
                    annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                        irCall(symbols.index.constructors.single()).apply {
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

}
