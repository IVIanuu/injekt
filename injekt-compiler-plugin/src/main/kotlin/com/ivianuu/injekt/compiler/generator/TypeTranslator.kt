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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

@Binding
class TypeTranslator(
    private val declarationStore: DeclarationStore
) {

    init {
        declarationStore.typeTranslator = this
    }

    fun toClassifierRef(
        descriptor: ClassifierDescriptor
    ): ClassifierRef {
        return ClassifierRef(
            descriptor.original.fqNameSafe,
            (descriptor.original as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
                ?.map { toClassifierRef(it) } ?: emptyList(),
            (descriptor.original as? TypeParameterDescriptor)?.upperBounds?.map {
                toTypeRef(it, Variance.INVARIANT)
            } ?: emptyList(),
            descriptor is TypeParameterDescriptor,
            descriptor is ClassDescriptor && descriptor.kind == ClassKind.OBJECT
        )
    }

    fun toTypeRef2(
        type: KotlinType,
        variance: Variance = Variance.INVARIANT
    ): TypeRef = toTypeRef(type, variance)

    fun toTypeRef(
        type: KotlinType,
        variance: Variance = Variance.INVARIANT
    ): TypeRef = KotlinTypeRef(type, this, variance)

}
