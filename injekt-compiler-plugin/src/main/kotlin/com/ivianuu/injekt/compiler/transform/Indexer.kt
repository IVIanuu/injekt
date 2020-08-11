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
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.backend.common.ir.addChild
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
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult

class Indexer(
    private val injektContext: InjektContext,
    private val module: IrModuleFragment,
    private val symbols: InjektSymbols
) {

    private val classIndicesByTagAndKey = mutableMapOf<String, List<IrClass>>()
    fun classIndices(
        tag: String,
        key: String
    ): List<IrClass> {
        val finalTag = tag.removeIllegalChars()
        val finalKey = key.removeIllegalChars()
        return classIndicesByTagAndKey.getOrPut(finalTag + finalKey) {
            val internalClasses = measureTimeMillisWithResult {
                internalDeclarationsByIndices.keys
                    .filter { it.tag == finalTag && it.key == finalKey && it.type == "class" }
                    .map { internalDeclarationsByIndices[it]!! as IrClass }
            }.let {
                println("computing internal classes for $tag and $key took ${it.first} ms")
                it.second
            }

            (internalClasses + externalClassIndices(finalTag, finalKey))
                .distinct()
        }
    }

    private val externalClassIndicesByTagAndKey = mutableMapOf<String, List<IrClass>>()
    fun externalClassIndices(
        tag: String,
        key: String
    ): List<IrClass> {
        val finalTag = tag.removeIllegalChars()
        val finalKey = key.removeIllegalChars()
        return externalClassIndicesByTagAndKey.getOrPut(finalTag + finalKey) {
            measureTimeMillisWithResult {
                externalIndicesByTagAndKey(finalTag, finalKey)
                    .filter { it.type == "class" }
                    .map { index ->
                        if (index.indexIsDeclaration) index.indexClass
                        else injektContext.referenceClass(index.fqName)?.owner!!
                    }
            }.let {
                println("computing external classes for $tag and $key took ${it.first} ms")
                it.second
            }
        }
    }

    private val functionIndicesByTagAndKey = mutableMapOf<String, List<IrFunction>>()
    fun functionIndices(tag: String, key: String): List<IrFunction> {
        val finalTag = tag.removeIllegalChars()
        val finalKey = key.removeIllegalChars()
        return functionIndicesByTagAndKey.getOrPut(finalTag + finalKey) {
            val internalFunctions = measureTimeMillisWithResult {
                internalDeclarationsByIndices.keys
                    .filter { it.tag == finalTag && it.key == finalKey && it.type == "function" }
                    .map { internalDeclarationsByIndices[it]!! }
                    .filterIsInstance<IrFunction>()
            }.let {
                println("computing internal functions for $tag and $key took ${it.first} ms")
                it.second
            }

            val externalFunctions = measureTimeMillisWithResult {
                externalIndicesByTagAndKey(finalTag, finalKey)
                    .filter { it.type == "function" }
                    .flatMapFix { index ->
                        injektContext.referenceFunctions(index.fqName)
                            .map { it.owner }
                    }
            }.let {
                println("computing external functions for $tag and $key took ${it.first} ms")
                it.second
            }

            (internalFunctions + externalFunctions)
                .distinct()
        }
    }

    private val propertyIndicesByTagAndKey = mutableMapOf<String, List<IrProperty>>()
    fun propertyIndices(tag: String, key: String): List<IrProperty> {
        val finalTag = tag.removeIllegalChars()
        val finalKey = key.removeIllegalChars()
        return propertyIndicesByTagAndKey.getOrPut(finalTag + finalKey) {
            val internalProperties = measureTimeMillisWithResult {
                internalDeclarationsByIndices.keys
                    .filter { it.tag == finalTag && it.key == finalKey && it.type == "property" }
                    .map { internalDeclarationsByIndices[it]!! }
                    .filterIsInstance<IrProperty>()
            }.let {
                println("computing internal properties for $tag and $key took ${it.first} ms")
                it.second
            }

            val externalProperties = measureTimeMillisWithResult {
                externalIndicesByTagAndKey(finalTag, finalKey)
                    .filter { it.type == "property" }
                    .flatMapFix { index ->
                        injektContext.referenceProperties(index.fqName)
                            .map { it.owner }
                    }
            }.let {
                println("computing external properties for $tag and $key took ${it.first} ms")
                it.second
            }

            (internalProperties + externalProperties)
                .distinct()
        }
    }

    private val externalIndicesByTagAndKey = mutableMapOf<String, List<Index>>()
    private fun externalIndicesByTagAndKey(tag: String, key: String): List<Index> {
        val finalTag = tag.removeIllegalChars()
        val finalKey = key.removeIllegalChars()
        return externalIndicesByTagAndKey.getOrPut(finalTag + finalKey) {
            measureTimeMillisWithResult {
                val memberScope = module.descriptor.getPackage(
                    InjektFqNames.IndexPackage
                        .child(finalTag.asNameId())
                        .child(finalKey.asNameId())
                ).memberScope
                (memberScope.getClassifierNames() ?: emptySet())
                    .mapNotNull {
                        memberScope.getContributedClassifier(
                            it,
                            NoLookupLocation.FROM_BACKEND
                        )
                    }
                    .map { injektContext.referenceClass(it.fqNameSafe)!!.owner }
                    .map {
                        Index(
                            finalTag,
                            finalKey,
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
            }.let {
                println("computing indices for tag $finalTag and key $finalKey took ${it.first} ms")
                it.second
            }
        }
    }

    private data class Index(
        val tag: String,
        val key: String,
        val fqName: FqName,
        val indexClass: IrClass,
        val type: String,
        val indexIsDeclaration: Boolean
    )

    private val internalDeclarationsByIndices = mutableMapOf<Index, IrDeclaration>()

    fun index(
        originatingDeclaration: IrDeclarationWithName,
        tag: String,
        key: String,
        classBuilder: IrClass.() -> Unit
    ) {
        val finalTag = tag.removeIllegalChars()
        val finalKey = key.removeIllegalChars()
        val packageFqName = InjektFqNames.IndexPackage
            .child(finalTag.removeIllegalChars().asNameId())
            .child(finalKey.removeIllegalChars().asNameId())

        val name = injektContext.uniqueClassNameProvider(
            (getJoinedName(
                originatingDeclaration.getPackageFragment()!!.fqName,
                originatingDeclaration.descriptor.fqNameSafe
                    .parent().child(originatingDeclaration.name.asString().asNameId())
            ).asString() + "${originatingDeclaration.uniqueKey().hashCode()}Index")
                .asNameId(),
            packageFqName
        )

        module.addFile(
            injektContext,
            packageFqName
                .child(name)
        ).apply file@{
            recordLookup(this, originatingDeclaration)

            addChild(
                buildClass {
                    this.name = name
                    kind = ClassKind.INTERFACE
                    visibility = Visibilities.INTERNAL
                }.apply {
                    parent = this@file
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                    addMetadataIfNotLocal()
                    val index = Index(
                        finalTag,
                        finalKey,
                        descriptor.fqNameSafe,
                        this,
                        "class",
                        true
                    )
                    annotations += DeclarationIrBuilder(injektContext, symbol).run {
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
        tag: String,
        key: String,
        declaration: IrDeclarationWithName
    ) {
        val finalTag = tag.removeIllegalChars()
        val finalKey = key.removeIllegalChars()
        val packageFqName = InjektFqNames.IndexPackage
            .child(finalTag.asNameId())
            .child(finalKey.asNameId())

        val name = injektContext.uniqueClassNameProvider(
            (getJoinedName(
                declaration.getPackageFragment()!!.fqName,
                declaration.descriptor.fqNameSafe
                    .parent().child(declaration.name.asString().asNameId())
            ).asString() + "${declaration.uniqueKey().hashCode()}Index").asNameId(),
            packageFqName
        )
        module.addFile(
            injektContext,
            packageFqName.child(name)
        ).apply {
            recordLookup(this, declaration)

            addChild(
                buildClass {
                    this.name = name
                    kind = ClassKind.INTERFACE
                    visibility = Visibilities.INTERNAL
                }.apply {
                    val index = Index(
                        finalTag,
                        finalKey,
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
                    annotations += DeclarationIrBuilder(injektContext, symbol).run {
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
