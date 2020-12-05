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

package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.UniqueNameProvider
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassifierAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.constants.StringValue

@Binding(GenerationComponent::class)
class IndexGenerator(
    private val fileManager: FileManager,
    private val module: ModuleDescriptor,
    private val packageFragmentProvider: InjektPackageFragmentProviderExtension
) : Generator {

    private val indexType = module.findClassifierAcrossModuleDependencies(
        ClassId.topLevel(InjektFqNames.Index)
    )!!

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            val indices = mutableListOf<Pair<Index, Name>>()
            val nameProvider = UniqueNameProvider()

            file.accept(
                object : KtTreeVisitorVoid() {
                    var moduleLikeScope: KtClassOrObject? = null

                    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                        val prevModuleLikeScope = moduleLikeScope
                        val isModuleLikeScope = classOrObject.hasAnnotation(InjektFqNames.Module) ||
                                classOrObject.hasAnnotation(InjektFqNames.Component) ||
                                classOrObject.hasAnnotation(InjektFqNames.ChildComponent) ||
                                classOrObject.hasAnnotation(InjektFqNames.MergeComponent) ||
                                classOrObject.hasAnnotation(InjektFqNames.MergeChildComponent) ||
                                classOrObject !is KtObjectDeclaration
                        moduleLikeScope = if (isModuleLikeScope) classOrObject else null
                        super.visitClassOrObject(classOrObject)
                        moduleLikeScope = prevModuleLikeScope
                    }

                    override fun visitDeclaration(declaration: KtDeclaration) {
                        super.visitDeclaration(declaration)
                        if (moduleLikeScope != null &&
                            declaration != moduleLikeScope &&
                            declaration !is KtConstructor<*> &&
                            declaration !is KtClassOrObject) return

                        if (declaration !is KtClassOrObject &&
                            declaration !is KtNamedFunction &&
                            declaration !is KtConstructor<*> &&
                            declaration !is KtProperty) return

                        val needsIndexing = declaration.hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) ||
                                declaration.hasAnnotationWithPropertyAndClass(InjektFqNames.Interceptor) ||
                                declaration.hasAnnotationWithPropertyAndClass(InjektFqNames.FunBinding) ||
                                declaration.hasAnnotationWithPropertyAndClass(InjektFqNames.MapEntries) ||
                                declaration.hasAnnotationWithPropertyAndClass(InjektFqNames.SetElements) ||
                                declaration.hasAnnotation(InjektFqNames.MergeComponent) ||
                                declaration.hasAnnotation(InjektFqNames.MergeChildComponent) ||
                                declaration.hasAnnotation(InjektFqNames.MergeInto) ||
                                declaration.hasAnnotation(InjektFqNames.Module)

                        if (!needsIndexing) return

                        val owner = when (declaration) {
                            is KtConstructor<*> -> declaration.containingClass()!!
                            is KtPropertyAccessor -> declaration.property
                            else -> declaration as KtNamedDeclaration
                        }

                        val index = Index(
                            owner.fqName!!,
                            when (owner) {
                                is KtClassOrObject -> "class"
                                is KtNamedFunction -> "function"
                                is KtProperty -> "property"
                                else -> error("Unexpected declaration ${declaration.text}")
                            }
                        )
                        val indexName = nameProvider(
                            index.fqName.pathSegments().joinToString("_")
                        ).asNameId()
                        indices += index to indexName
                        //declarationStore.addInternalIndex(index)
                        packageFragmentProvider.addGeneratedProperty(InjektFqNames.IndexPackage) {
                            object : PropertyDescriptorImpl(
                                it,
                                null,
                                Annotations.create(
                                    listOf(
                                        AnnotationDescriptorImpl(
                                            indexType.defaultType,
                                            mapOf(
                                                "fqName".asNameId() to StringValue(index.fqName.asString()),
                                                "type".asNameId() to StringValue(index.type)
                                            ),
                                            SourceElement.NO_SOURCE
                                        )
                                    )
                                ),
                                Modality.FINAL,
                                DescriptorVisibilities.INTERNAL,
                                false,
                                indexName,
                                CallableMemberDescriptor.Kind.DECLARATION,
                                SourceElement.NO_SOURCE,
                                false,
                                false,
                                false,
                                false,
                                false,
                                false
                            ) {
                                init {
                                    initialize(
                                        object : PropertyGetterDescriptorImpl(
                                            this,
                                            Annotations.EMPTY,TODO("Not yet implemented")
                                            Modality.FINAL,
                                            DescriptorVisibilities.INTERNAL,
                                            false,
                                            false,
                                            false,
                                            CallableMemberDescriptor.Kind.DECLARATION,
                                            null,
                                            SourceElement.NO_SOURCE
                                        ) {
                                            init {
                                                initialize(module.builtIns.unitType)
                                            }
                                          },
                                        null as PropertySetterDescriptorImpl?
                                    )
                                }
                            }
                        }
                    }
                }
            )

            if (indices.isNotEmpty()) {
                val fileName = file.packageFqName.pathSegments().joinToString("_") + "_${file.name}"
                fileManager.generateFile(
                    originatingFile = file,
                    packageFqName = InjektFqNames.IndexPackage,
                    fileName = fileName,
                    code = buildCodeString {
                        emitLine("@file:Suppress(\"UNCHECKED_CAST\", \"NOTHING_TO_INLINE\")")
                        emitLine("package ${InjektFqNames.IndexPackage}")
                        emitLine("import ${InjektFqNames.Index}")
                        indices
                            .distinct()
                            .forEach { (index, indexName) ->
                                emitLine("@Index(fqName = \"${index.fqName}\", type = \"${index.type}\")")
                                emitLine("internal val $indexName = Unit")
                        }
                    }
                )
            }
        }
    }

}