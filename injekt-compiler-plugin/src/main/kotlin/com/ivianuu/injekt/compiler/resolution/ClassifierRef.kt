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

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.apply
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class ClassifierRef(
    val fqName: FqName,
    val typeParameters: List<ClassifierRef> = emptyList(),
    val superTypes: List<TypeRef> = emptyList(),
    val isTypeParameter: Boolean = false,
    val isObject: Boolean = false,
    val isTypeAlias: Boolean = false,
    val isTypeWrapper: Boolean = false,
    val descriptor: ClassifierDescriptor? = null,
    val typeWrappers: List<TypeRef> = emptyList(),
    val isGivenConstraint: Boolean = false,
    val primaryConstructorPropertyParameters: List<Name> = emptyList(),
    val forTypeKeyTypeParameters: List<Name> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (other !is ClassifierRef) return false
        if (fqName != other.fqName) return false
        if (isTypeWrapper && other.isTypeWrapper) return superTypes == other.superTypes
        return true
    }

    override fun hashCode(): Int {
        var result = fqName.hashCode()
        if (isTypeWrapper) result = 31 * result + superTypes.hashCode()
        return result
    }

    val defaultType: TypeRef by unsafeLazy {
        SimpleTypeRef(
            this,
            arguments = typeParameters.map { it.defaultType }
        )
    }
}

fun ClassifierDescriptor.toClassifierRef(
    context: InjektContext,
    trace: BindingTrace?
): ClassifierRef {
    trace?.get(InjektWritableSlices.CLASSIFIER_REF_FOR_CLASSIFIER, this)?.let { return it }
    val expandedType = (original as? TypeAliasDescriptor)?.underlyingType
        ?.toTypeRef(context, trace)
    val typeWrappers = getAnnotatedAnnotations(InjektFqNames.TypeWrapper)
        .map { it.type.toTypeRef(context, trace) }
    return ClassifierRef(
        fqName = original.fqNameSafe,
        typeParameters = (original as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
            ?.map { it.toClassifierRef(context, trace) } ?: emptyList(),
        superTypes = if (expandedType != null) listOf(expandedType)
        else typeConstructor.supertypes.map { it.toTypeRef(context, trace) },
        isTypeParameter = this is TypeParameterDescriptor,
        isObject = this is ClassDescriptor && kind == ClassKind.OBJECT,
        isTypeAlias = this is TypeAliasDescriptor,
        isTypeWrapper = hasAnnotation(InjektFqNames.TypeWrapper),
        descriptor = this,
        typeWrappers = typeWrappers,
        isGivenConstraint = this is TypeParameterDescriptor && hasAnnotation(InjektFqNames.Given),
        primaryConstructorPropertyParameters = this
            .takeIf { !it.isExternalDeclaration() }
            .safeAs<ClassDescriptor>()
            ?.unsubstitutedPrimaryConstructor
            ?.valueParameters
            ?.filter { it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true }
            ?.map { it.name }
            ?: emptyList(),
        forTypeKeyTypeParameters = this
            .takeIf { !it.isExternalDeclaration() }
            .safeAs<ClassDescriptor>()
            ?.findPsi()
            ?.safeAs<KtClass>()
            ?.typeParameters
            ?.filter { it.hasAnnotation(InjektFqNames.ForTypeKey) }
            ?.map { it.nameAsSafeName }
            ?: emptyList()
    ).let {
        if (original.isExternalDeclaration()) it.apply(
            context,
            trace,
            context.classifierInfoFor(it, trace)
        ) else it
    }.also {
        trace?.record(InjektWritableSlices.CLASSIFIER_REF_FOR_CLASSIFIER, this, it)
    }
}
