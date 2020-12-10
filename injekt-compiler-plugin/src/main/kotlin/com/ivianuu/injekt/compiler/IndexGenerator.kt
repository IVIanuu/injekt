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

import com.ivianuu.injekt.Binding
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Binding class IndexGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
) : Generator {

    override fun generate(files: List<KtFile>) {
        files
            .map { file ->
                val indices = mutableListOf<Index>()
                val givenInfos = mutableListOf<GivenInfo>()

                file.accept(
                    object : KtTreeVisitorVoid() {
                        override fun visitDeclaration(declaration: KtDeclaration) {
                            super.visitDeclaration(declaration)

                            if (declaration !is KtNamedFunction &&
                                declaration !is KtClassOrObject &&
                                declaration !is KtProperty &&
                                declaration !is KtConstructor<*>
                            ) return

                            val descriptor = declaration.descriptor<DeclarationDescriptor>(
                                bindingContext
                            ) ?: return

                            if (descriptor is LocalVariableDescriptor) return

                            val owner = when (descriptor) {
                                is ClassDescriptor -> descriptor
                                is ConstructorDescriptor -> descriptor.constructedClass
                                is PropertyAccessorDescriptor -> descriptor.correspondingProperty
                                else -> descriptor
                            }

                            if (descriptor.hasAnnotation(InjektFqNames.Given)) {
                                val index = Index(
                                    owner!!.fqNameSafe,
                                    when (owner) {
                                        is ClassDescriptor -> "class"
                                        is FunctionDescriptor -> "function"
                                        is PropertyDescriptor -> "property"
                                        else -> error("Unexpected declaration ${declaration.text}")
                                    }
                                )
                                indices += index
                            }

                            val givens = when (descriptor) {
                                is ConstructorDescriptor -> descriptor.valueParameters
                                    .filter { it.type.hasAnnotation(InjektFqNames.Given) }
                                is ClassDescriptor -> descriptor.getGivenConstructor()
                                    ?.valueParameters
                                    ?.filter { it.type.hasAnnotation(InjektFqNames.Given) }
                                    ?: emptyList()
                                is PropertyDescriptor -> emptyList()
                                is FunctionDescriptor -> descriptor.valueParameters
                                    .filter { it.type.hasAnnotation(InjektFqNames.Given) }
                                else -> error("Unexpected descriptor $descriptor")
                            }

                            val (requiredGivens, givensWithDefault) = givens
                                .partition { givenParameter ->
                                    (givenParameter.findPsi() as KtParameter)
                                        .defaultValue?.text == "given"
                                }

                            if (requiredGivens.isNotEmpty() || givensWithDefault.isNotEmpty()) {
                                val givenInfo = GivenInfo(
                                    descriptor.fqNameSafe,
                                    owner.uniqueKey(),
                                    requiredGivens
                                        .map { it.name },
                                    givensWithDefault
                                        .map { it.name }
                                )
                                givenInfos += givenInfo
                                declarationStore.addGivenInfoForKey(
                                    owner.uniqueKey(),
                                    givenInfo
                                )
                            }
                        }
                    }
                )
                Triple(file, indices, givenInfos)
            }
            .filter { it.second.isNotEmpty() || it.third.isNotEmpty() }
            .forEach { (file, indices, givenInfos) ->
                indices.forEach { declarationStore.addInternalIndex(it) }
                val fileName = file.packageFqName.pathSegments().joinToString("_") + "_${file.name}"
                val nameProvider = UniqueNameProvider()
                fileManager.generateFile(
                    originatingFile = file,
                    packageFqName = InjektFqNames.IndexPackage,
                    fileName = fileName,
                    code = buildCodeString {
                        emitLine("@file:Suppress(\"UNCHECKED_CAST\", \"NOTHING_TO_INLINE\")")
                        emitLine("package ${InjektFqNames.IndexPackage}")
                        if (indices.isNotEmpty()) emitLine("import ${InjektFqNames.Index}")
                        if (givenInfos.isNotEmpty()) emitLine("import ${InjektFqNames.GivenInfo}")

                        indices
                            .distinct()
                            .forEach { index ->
                                val indexName = nameProvider(
                                    index.fqName.pathSegments().joinToString("_")
                                ).asNameId()
                                emitLine("@Index(fqName = \"${index.fqName}\", type = \"${index.type}\")")
                                emitLine("internal val $indexName = Unit")
                            }

                        givenInfos
                            .forEach { info ->
                                val infoName = nameProvider(
                                    info.fqName.pathSegments().joinToString("_")
                                ).asNameId()
                                emitLine("@GivenInfo(fqName = \"${info.fqName}\",\nkey = \"${info.key}\",\n" +
                                        "requiredGivens = [${info.requiredGivens.joinToString(", ") { "\"$it\"" }}],\n" +
                                        "givensWithDefault = [${
                                            info.givensWithDefault.joinToString(", ") { "\"$it\"" }
                                        }]\n" +
                                        ")")
                                emitLine("internal val $infoName = Unit")
                            }
                    }
                )
            }
    }
}