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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.ErrorType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

@Binding
class TypeTranslator(
    private val declarationStore: DeclarationStore,
    private val errorCollector: ErrorCollector
) {

    init {
        declarationStore.typeTranslator = this
    }

    fun toClassifierRef(
        descriptor: ClassifierDescriptor,
        fixType: Boolean = true
    ): ClassifierRef {
        return ClassifierRef(
            descriptor.original.fqNameSafe,
            (descriptor.original as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
                ?.map { toClassifierRef(it, fixType) } ?: emptyList(),
            (descriptor.original as? TypeParameterDescriptor)?.upperBounds?.map {
                toTypeRef(it, descriptor, Variance.INVARIANT)
            } ?: emptyList(),
            descriptor is TypeParameterDescriptor,
            descriptor is ClassDescriptor && descriptor.kind == ClassKind.OBJECT
        )
    }

    fun toTypeRef2(
        type: KotlinType,
        variance: Variance = Variance.INVARIANT,
        fixType: Boolean = true
    ): TypeRef = toTypeRef(type, null as? KtFile, variance, fixType)

    fun toTypeRef(
        type: KotlinType,
        file: KtFile?,
        variance: Variance = Variance.INVARIANT,
        fixType: Boolean = true
    ): TypeRef = KotlinTypeRef(type, this, variance)
        .let { if (fixType) fixType(it, file) else it }

    fun toTypeRef(
        type: KotlinType,
        fromDescriptor: DeclarationDescriptor?,
        variance: Variance = Variance.INVARIANT,
        fixType: Boolean = true
    ): TypeRef = toTypeRef(type, fromDescriptor?.findPsi()?.containingFile as? KtFile, variance, fixType)

    fun fixType(type: TypeRef, file: KtFile?): TypeRef {
        if (type is KotlinTypeRef) {
            val kotlinType = type.kotlinType
            if (kotlinType is ErrorType) {
                file ?: error("Cannot fix types without file context ${type.render()}")
                val simpleName = kotlinType.presentableName.substringBefore("<").asNameId()
                val imports = file.importDirectives
                val fqName = imports
                    .mapNotNull { it.importPath }
                    .singleOrNull { it.fqName.shortName() == simpleName }
                    ?.fqName
                    ?: file.packageFqName.child(simpleName)
                val generatedClassifier = declarationStore.generatedClassifierFor(fqName)
                if (generatedClassifier != null) {
                    return type.copy(
                        classifier = generatedClassifier,
                        typeArguments = type.typeArguments.map { fixType(it, file) },
                        expandedType = type.expandedType?.let { fixType(it, file) }
                    )
                } else {
                    errorCollector.add(
                        RuntimeException(
                            "Cannot resolve $type in ${file.virtualFilePath} guessed name '$fqName' " +
                                    "Do not use function aliases with '*' imports and import them explicitly"
                        )
                    )
                }
            }
        }
        return type.copy(
            classifier = type.classifier,
            typeArguments = type.typeArguments.map { fixType(it, file) },
            expandedType = type.expandedType?.let { fixType(it, file) }
        )
    }

}
