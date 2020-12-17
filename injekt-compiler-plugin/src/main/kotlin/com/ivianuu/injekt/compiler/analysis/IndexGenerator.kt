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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.generator.Generator
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class IndexGenerator : Generator {
    override fun generate(context: Generator.Context, files: List<KtFile>) {
        files.forEach { file ->
            val indices = mutableListOf<Index>()
            file.accept(object : KtTreeVisitorVoid() {
                override fun visitDeclaration(declaration: KtDeclaration) {
                    super.visitDeclaration(declaration)
                    if (declaration !is KtNamedFunction &&
                        declaration !is KtClassOrObject &&
                        declaration !is KtProperty &&
                        declaration !is KtConstructor<*>
                    ) return

                    if (declaration is KtClassOrObject && declaration.isLocal) return
                    if (declaration is KtProperty && declaration.isLocal) return
                    if (declaration is KtFunction && declaration.isLocal) return

                    val owner = when (declaration) {
                        is KtConstructor<*> -> declaration.getContainingClassOrObject()
                        is KtPropertyAccessor -> declaration.property
                        else -> declaration
                    } as KtNamedDeclaration

                    if ((owner is KtNamedFunction ||
                                owner is KtProperty) &&
                        owner.parent.safeAs<KtClassBody>()?.parent is KtClass
                    ) return

                    if (declaration.hasAnnotation(InjektFqNames.Given) ||
                        declaration.hasAnnotation(InjektFqNames.GivenSetElement) ||
                        declaration.hasAnnotation(InjektFqNames.GivenGroup)
                    ) {
                        val index = Index(
                            owner.fqName!!,
                            when (owner) {
                                is KtClassOrObject -> "class"
                                is KtConstructor<*> -> "constructor"
                                is KtFunction -> "function"
                                is KtProperty -> "property"
                                else -> error("Unexpected declaration ${declaration.text}")
                            }
                        )
                        indices += index
                    }
                }
            })

            if (indices.isEmpty()) return@forEach

            val fileName = file.packageFqName.pathSegments().joinToString("_") +
                    "_${file.name.removeSuffix(".kt")}Indices.kt"
            val nameProvider = UniqueNameProvider()
            context.generateFile(
                originatingFile = file,
                packageFqName = InjektFqNames.IndexPackage,
                fileName = fileName,
                code = buildString {
                    appendLine("package ${InjektFqNames.IndexPackage}")
                    appendLine("import ${InjektFqNames.Index}")
                    indices
                        .distinct()
                        .forEach { index ->
                            val indexName = nameProvider(
                                index.fqName.pathSegments().joinToString("_") + "${file.name}_index"
                            ).asNameId()
                            appendLine("@Index(fqName = \"${index.fqName}\", type = \"${index.type}\")")
                            appendLine("internal val $indexName = Unit")
                        }
                }
            )
        }
    }
}