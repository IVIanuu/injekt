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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Binding(GenerationComponent::class)
class IndexGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager
) : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            val indices = mutableListOf<Index>()
            val effectOrInterceptorDescriptors = mutableListOf<DeclarationDescriptor>()
            file.accept(
                object : KtTreeVisitorVoid() {
                    var moduleLikeScope: KtClassOrObject? = null

                    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                        val descriptor = classOrObject.descriptor<DeclarationDescriptor>(bindingContext)
                            ?: return
                        val prevModuleLikeScope = moduleLikeScope
                        val isModuleLikeScope = descriptor.hasAnnotation(InjektFqNames.Module) ||
                                descriptor.hasAnnotation(InjektFqNames.Component) ||
                                descriptor.hasAnnotation(InjektFqNames.ChildComponent) ||
                                descriptor.hasAnnotation(InjektFqNames.MergeComponent) ||
                                descriptor.hasAnnotation(InjektFqNames.MergeChildComponent)
                        moduleLikeScope = if (isModuleLikeScope) classOrObject else null
                        super.visitClassOrObject(classOrObject)
                        moduleLikeScope = prevModuleLikeScope
                    }

                    override fun visitDeclaration(declaration: KtDeclaration) {
                        super.visitDeclaration(declaration)
                        if (moduleLikeScope != null &&
                                declaration != moduleLikeScope) return

                        if (declaration !is KtClassOrObject &&
                            declaration !is KtNamedFunction &&
                            declaration !is KtConstructor<*> &&
                            declaration !is KtProperty &&
                            declaration !is KtTypeAlias) return

                        val descriptor = declaration.descriptor<DeclarationDescriptor>(bindingContext)
                            ?: return

                        val effects = descriptor.getAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Effect)
                        val interceptors = descriptor.getAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Interceptor)
                        val needsIndexing = effects.isNotEmpty() || interceptors.isNotEmpty() ||
                                descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) ||
                                descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.Interceptor) ||
                                descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.FunBinding) ||
                                descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.ImplBinding) ||
                                descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.MapEntries) ||
                                descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.SetElements) ||
                                descriptor.hasAnnotation(InjektFqNames.MergeComponent) ||
                                descriptor.hasAnnotation(InjektFqNames.MergeChildComponent) ||
                                descriptor.hasAnnotation(InjektFqNames.MergeInto)

                        if (!needsIndexing) return

                        val owner = when (descriptor) {
                            is ConstructorDescriptor -> descriptor.constructedClass
                            is PropertyAccessorDescriptor -> descriptor.correspondingProperty
                            else -> descriptor
                        }

                        val annotationTypes = (effects + interceptors).map {
                            val annotationType = declarationStore.typeTranslator.toTypeRef(it.type, file)
                            annotationType.classifier.fqName to annotationType
                        }.toMap()

                        val index = Index(
                            owner.fqNameSafe,
                            when (owner) {
                                is ClassDescriptor -> "class"
                                is FunctionDescriptor -> "function"
                                is PropertyDescriptor -> "property"
                                is TypeAliasDescriptor -> "typealias"
                                else -> error("Unexpected declaration ${declaration.text}")
                            },
                            annotationTypes
                        )
                        indices += index
                        declarationStore.addInternalIndex(index)
                        if (effects.isNotEmpty() || interceptors.isNotEmpty())
                            effectOrInterceptorDescriptors += descriptor
                    }
                }
            )

            effectOrInterceptorDescriptors.forEach { descriptor ->
                // we instantiate the callable for the descriptor
                // which then checks for correctness
                when (descriptor) {
                    is ClassDescriptor -> declarationStore.callableForDescriptor(descriptor.getInjectConstructor()!!)
                    is FunctionDescriptor -> declarationStore.callableForDescriptor(descriptor)
                    is PropertyDescriptor -> declarationStore.callableForDescriptor(descriptor.getter!!)
                    is TypeAliasDescriptor -> {
                    }
                    else -> error("$descriptor is not a valid effect target")
                }
            }

            if (indices.isNotEmpty()) {
                val nameProvider = UniqueNameProvider()
                val fileName = file.packageFqName.pathSegments().joinToString("_") + "_${file.name}"
                fileManager.generateFile(
                    packageFqName = InjektFqNames.IndexPackage,
                    fileName = fileName,
                    code = buildCodeString {
                        emitLine("@file:Suppress(\"UNCHECKED_CAST\", \"NOTHING_TO_INLINE\")")
                        emitLine("package ${InjektFqNames.IndexPackage}")
                        emitLine("import ${InjektFqNames.Index}")
                        indices
                            .distinct()
                            .forEach { index ->
                                if (index.annotationTypes.isNotEmpty()) {
                                    val annotationTypesName = nameProvider(
                                        index.fqName.pathSegments().joinToString("_") +
                                                "_AnnotationTypes"
                                    )

                                    emitLine("annotation class $annotationTypesName(")
                                    indented {
                                        index.annotationTypes.toList()
                                            .forEachIndexed { i, (fqName, type) ->
                                                emit("val ${
                                                    fqName.pathSegments().joinToString("_")
                                                }: ${type.render()}")
                                                if (i != index.annotationTypes.size - 1) emitLine(",")
                                            }
                                        emitLine()
                                    }
                                    emitLine(")")
                                    emitLine()
                                    emitLine("@${InjektFqNames.AnnotationTypesName}(\"${InjektFqNames.IndexPackage}.$annotationTypesName\")")
                                }
                                val indexName = nameProvider(
                                    index.fqName.pathSegments().joinToString("_")
                                )
                                emitLine("@Index(fqName = \"${index.fqName}\", type = \"${index.type}\")")
                                emitLine("internal val $indexName = Unit")
                        }
                    }
                )
            }
        }
    }

}