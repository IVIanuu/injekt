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
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
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
            file.accept(
                object : KtTreeVisitorVoid() {
                    var inModuleLikeScope = false

                    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                        val descriptor = classOrObject.descriptor<DeclarationDescriptor>(bindingContext)
                            ?: return
                        val prevInModuleLikeScope = inModuleLikeScope
                        inModuleLikeScope = descriptor.hasAnnotation(InjektFqNames.Module) ||
                                descriptor.hasAnnotation(InjektFqNames.Component) ||
                                descriptor.hasAnnotation(InjektFqNames.ChildComponent) ||
                                descriptor.hasAnnotation(InjektFqNames.MergeComponent) ||
                                descriptor.hasAnnotation(InjektFqNames.MergeChildComponent) ||
                                descriptor.containingDeclaration?.hasAnnotation(InjektFqNames.Decorator) == true ||
                                descriptor.containingDeclaration?.hasAnnotation(InjektFqNames.Effect) == true
                        super.visitClassOrObject(classOrObject)
                        inModuleLikeScope = prevInModuleLikeScope
                    }

                    override fun visitDeclaration(declaration: KtDeclaration) {
                        super.visitDeclaration(declaration)
                        val descriptor = declaration.descriptor<DeclarationDescriptor>(bindingContext)
                            ?: return

                        if (((descriptor is ClassDescriptor || descriptor is ConstructorDescriptor || !inModuleLikeScope) &&
                                    (descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) ||
                                            descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.Decorator) ||
                                            descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.FunBinding) ||
                                            descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.ImplBinding) ||
                                            descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.MapEntries) ||
                                            descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.SetElements) ||
                                            descriptor.hasAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Decorator) ||
                                            descriptor.hasAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Effect))) ||
                            descriptor.hasAnnotation(InjektFqNames.MergeComponent) ||
                            descriptor.hasAnnotation(InjektFqNames.MergeChildComponent) ||
                            descriptor.hasAnnotation(InjektFqNames.MergeInto)) {
                            if (descriptor.hasAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Effect)) {
                                checkEffects(descriptor)
                            }
                            val owner = when (descriptor) {
                                is ConstructorDescriptor -> descriptor.constructedClass
                                is PropertyAccessorDescriptor -> descriptor.correspondingProperty
                                else -> descriptor
                            }
                            val index = Index(
                                owner.fqNameSafe,
                                when (owner) {
                                    is ClassDescriptor -> "class"
                                    is FunctionDescriptor -> "function"
                                    is PropertyDescriptor -> "property"
                                    is TypeAliasDescriptor -> "typealias"
                                    else -> error("Unexpected declaration ${declaration.text}")
                                }
                            )
                            indices += index
                            declarationStore.addInternalIndex(index)
                        }
                    }
                }
            )

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

    private fun checkEffects(descriptor: DeclarationDescriptor) {
        val effectCallables = when (descriptor) {
            is ClassDescriptor -> declarationStore.callableForDescriptor(
                descriptor.getInjectConstructor()!!
            ).effects
            is FunctionDescriptor -> declarationStore.callableForDescriptor(descriptor).effects
            is PropertyDescriptor -> declarationStore.callableForDescriptor(descriptor.getter!!).effects
            is TypeAliasDescriptor -> {
                val type = declarationStore.typeTranslator.toClassifierRef(descriptor)
                descriptor.getAnnotatedAnnotations(InjektFqNames.Effect)
                    .flatMap {
                        declarationStore.effectCallablesForAnnotation(
                            it,
                            descriptor,
                            type.defaultType
                        )
                    }
            }
            else -> error("$descriptor is not a valid effect target")
        }

        val file = (descriptor.findPsi() as KtElement).containingKtFile

        effectCallables.forEach { effectCallable ->
            val substitutionMap = getSubstitutionMap(
                effectCallable.typeParameters
                    .mapNotNull { (effectCallable.typeArgs[it] ?: return@mapNotNull null) to it.defaultType } +
                        listOf(effectCallable.type to effectCallable.originalType),
                effectCallable.typeParameters
            )
            substitutionMap.forEach { (typeParameter, typeArgument) ->
                if (!typeArgument.isAssignable(typeParameter)) {
                    error("'${typeArgument.render()}' is not a sub type of '${typeParameter.render()}' for effect\n" +
                            "@${effectCallable.fqName.parent().parent()} on\n" +
                            "$descriptor\n" +
                            " ${file.virtualFilePath}")
                }
            }
        }
    }
}