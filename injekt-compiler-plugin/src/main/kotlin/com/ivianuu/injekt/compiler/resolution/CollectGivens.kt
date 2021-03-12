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

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.apply
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getGivenParameters
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.lazy.LazyImportScope
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class CallableRef(
    val callable: CallableDescriptor,
    val type: TypeRef,
    val originalType: TypeRef,
    val typeParameters: List<ClassifierRef>,
    val parameterTypes: Map<String, TypeRef>,
    val givenParameters: Set<String>,
    val qualifiers: List<AnnotationRef>,
    val typeArguments: Map<ClassifierRef, TypeRef>,
    val isGiven: Boolean,
    val fromGivenConstraint: Boolean,
    val depth: Int,
    val callContext: CallContext
)

fun CallableRef.substitute(map: Map<ClassifierRef, TypeRef>): CallableRef {
    if (map.isEmpty()) return this
    return copy(
        type = type.substitute(map),
        parameterTypes = parameterTypes
            .mapValues { it.value.substitute(map) },
        typeArguments = typeArguments
            .mapValues { it.value.substitute(map) },
        qualifiers = qualifiers
            .map { it.substitute(map) }
    )
}

fun CallableRef.substituteInputs(map: Map<ClassifierRef, TypeRef>): CallableRef {
    if (map.isEmpty()) return this
    return copy(
        parameterTypes = parameterTypes.mapValues { it.value.substitute(map) },
        typeArguments = typeArguments
            .mapValues { it.value.substitute(map) }
    )
}

fun CallableDescriptor.toCallableRef(declarationStore: DeclarationStore): CallableRef =
    declarationStore.callablesCache.getOrPut(this) {
        val qualifiers = getAnnotatedAnnotations(InjektFqNames.Qualifier)
            .map { it.toAnnotationRef(declarationStore) }
        val type = returnType!!.toTypeRef(declarationStore).let {
            if (qualifiers.isNotEmpty()) it.copy(qualifiers = qualifiers + it.qualifiers)
            else it
        }
        val typeParameters = typeParameters
            .map { it.toClassifierRef(declarationStore) }
        CallableRef(
            callable = this,
            type = type,
            originalType = type,
            typeParameters = typeParameters,
            parameterTypes = (if (this is ConstructorDescriptor) valueParameters else allParameters)
                .map { it.injektName() to it.type.toTypeRef(declarationStore) }
                .toMap(),
            givenParameters = (if (this is ConstructorDescriptor) valueParameters else allParameters)
                .filter { it.isGiven(declarationStore) }
                .mapTo(mutableSetOf()) { it.injektName() },
            qualifiers = qualifiers,
            typeArguments = typeParameters
                .map { it to it.defaultType }
                .toMap(),
            isGiven = isGiven(declarationStore),
            fromGivenConstraint = false,
            depth = 0,
            callContext = callContext
        ).let {
            if (original.isExternalDeclaration()) it.apply(
                declarationStore,
                declarationStore.callableInfoFor(it.callable)
            ) else it
        }
    }

fun MemberScope.collectGivens(
    declarationStore: DeclarationStore,
    type: TypeRef?,
    ownerDescriptor: DeclarationDescriptor?,
    depth: Int,
    substitutionMap: Map<ClassifierRef, TypeRef>
): List<CallableRef> {
    val givenPrimaryConstructorParameters = (type?.classifier?.descriptor
        ?.safeAs<ClassDescriptor>()
        ?.unsubstitutedPrimaryConstructor
        ?.valueParameters
        ?.filter { it.isGiven(declarationStore) }
        ?.map { it.name } ?: emptyList())
    return getContributedDescriptors()
        .flatMap { declaration ->
            when (declaration) {
                is ClassDescriptor -> declaration.getGivenConstructors(declarationStore)
                    .map {
                        it
                            .copy(isGiven = true, depth = depth)
                            .substitute(substitutionMap)
                    }
                is PropertyDescriptor -> if (declaration.isGiven(declarationStore) ||
                        declaration.name in givenPrimaryConstructorParameters) {
                    listOf(
                        declaration.toCallableRef(declarationStore)
                            .copy(isGiven = true, depth = depth)
                            .substitute(substitutionMap)
                    )
                } else emptyList()
                is FunctionDescriptor -> if (declaration.isGiven(declarationStore)) {
                    listOf(
                        declaration.toCallableRef(declarationStore)
                            .copy(isGiven = true, depth = depth)
                            .substitute(substitutionMap)
                    )
                } else emptyList()
                else -> emptyList()
            }
        }
        .filter { callable ->
            callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
                    callable.callable.visibility == DescriptorVisibilities.LOCAL ||
                    (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
                            !callable.callable.original.isExternalDeclaration()) ||
                    (callable.callable is ClassConstructorDescriptor &&
                            callable.type.classifier.isObject) ||
                    callable.callable.parents.any {
                        it == ownerDescriptor
                    }
        }
}

fun Annotated.isGiven(declarationStore: DeclarationStore): Boolean =
    hasAnnotation(InjektFqNames.Given) ||
            (this is ClassConstructorDescriptor && constructedClass.isGiven(declarationStore))

fun CallableDescriptor.collectGivens(declarationStore: DeclarationStore): List<CallableRef> {
    val declarations = mutableListOf<CallableRef>()

    declarations += allParameters
        .filter { it.isGiven(declarationStore) || it == extensionReceiverParameter }
        .map { it.toCallableRef(declarationStore).copy(isGiven = true) }

    return declarations
}

fun ParameterDescriptor.isGiven(declarationStore: DeclarationStore): Boolean {
    return (this as Annotated).isGiven(declarationStore) ||
            type.isGiven(declarationStore) ||
            getUserData(DslMarkerUtils.FunctionTypeAnnotationsKey)
                ?.hasAnnotation(InjektFqNames.Given) == true ||
            getGivenParameters(declarationStore)
                .firstOrNull { it.callable == this }
                ?.isGiven == true ||
            containingDeclaration.safeAs<FunctionDescriptor>()
                ?.takeIf { it.isExternalDeclaration() }
                ?.let { declarationStore.callableInfoFor(it) }
                ?.let { name.asString() in it.givenParameters } == true
}

fun ClassDescriptor.getGivenConstructors(
    declarationStore: DeclarationStore
): List<CallableRef> = constructors
    .filter { constructor ->
        constructor.isGiven(declarationStore) ||
                (constructor.isPrimary && isGiven(declarationStore))
    }
    .map {
        it.toCallableRef(declarationStore)
            .copy(isGiven = true)
    }
    .map {
        if (it.type.classifier.qualifiers.isNotEmpty()) {
            val typeWithQualifiers = it.type.copy(qualifiers = it.type.classifier.qualifiers + it.type.qualifiers)
            it.copy(type = typeWithQualifiers, originalType = typeWithQualifiers)
        } else {
            it
        }
    }

fun CallableRef.collectGivens(
    declarationStore: DeclarationStore,
    ownerDescriptor: DeclarationDescriptor?,
    depth: Int,
    substitutionMap: Map<ClassifierRef, TypeRef>,
    addGiven: (CallableRef) -> Unit,
    addConstrainedGiven: (CallableRef) -> Unit
) {
    if (!fromGivenConstraint && typeParameters.any { it.isGivenConstraint }) {
        addConstrainedGiven(this)
        return
    }
    addGiven(this)
    val combinedSubstitutionMap = substitutionMap + type.classifier.typeParameters
        .zip(type.arguments)
    callable
        .returnType!!
        .memberScope
        .collectGivens(declarationStore, type, ownerDescriptor, depth, combinedSubstitutionMap)
        .forEach {
            it.collectGivens(
                declarationStore,
                ownerDescriptor,
                depth,
                combinedSubstitutionMap,
                addGiven,
                addConstrainedGiven
            )
        }
}

fun HierarchicalScope.collectGivens(
    declarationStore: DeclarationStore
): List<CallableRef> {
    val allScopes = parentsWithSelf.toList()

    val importScopes = allScopes
        .filterIsInstance<LazyImportScope>()
        .filter { scope ->
            val scopeString = scope.toString()
            "LazyImportScope: Explicit imports in LazyFileScope for file" in scopeString
                    || ("LazyImportScope: All under imports in LazyFileScope for file" in scopeString &&
                    !scopeString.endsWith("(invisible classes only)"))
        }
        .toList()

    var depth = 0
    return parentsWithSelf
        .toList()
        .reversed()
        .filter { it !is ImportingScope || it in importScopes }
        .flatMap { scope ->
            depth++
            scope.collectGivensInScope(
                if (scope is ImportingScope) 0 else depth,
                declarationStore
            ) { depth++ }
        }
        .distinctBy { it.callable }
}

private fun HierarchicalScope.collectGivensInScope(
    depth: Int,
    declarationStore: DeclarationStore,
    bumpDepth: () -> Unit
): List<CallableRef> {
    return if (this is LexicalScope &&
            ownerDescriptor is ClassDescriptor &&
            kind == LexicalScopeKind.CLASS_MEMBER_SCOPE) {
        val clazz = ownerDescriptor as ClassDescriptor
        listOfNotNull(
            clazz.companionObjectDescriptor?.thisAsReceiverParameter
                ?.toCallableRef(declarationStore)
                ?.copy(isGiven = true, depth = depth)
                ?.also { bumpDepth() },
            clazz.thisAsReceiverParameter.toCallableRef(declarationStore)
                .copy(isGiven = true, depth = depth + 1)
        )
    } else if (this is LexicalScope &&
        ownerDescriptor is FunctionDescriptor &&
        kind == LexicalScopeKind.FUNCTION_INNER_SCOPE) {
        val function = ownerDescriptor as FunctionDescriptor
        function.collectGivens(declarationStore)
            .map { it.copy(depth = depth) }
    } else {
        getContributedDescriptors()
            .flatMap { declaration ->
                when (declaration) {
                    is ClassDescriptor -> declaration.getGivenConstructors(declarationStore)
                    is PropertyDescriptor -> if (declaration.isGiven(declarationStore)) {
                        listOf(
                            declaration.toCallableRef(declarationStore)
                                .copy(isGiven = true)
                        )
                    } else emptyList()
                    is FunctionDescriptor -> if (declaration.isGiven(declarationStore)) {
                        listOf(declaration.toCallableRef(declarationStore))
                    } else emptyList()
                    is VariableDescriptor -> if (declaration.isGiven(declarationStore)) {
                        listOf(declaration.toCallableRef(declarationStore))
                    } else emptyList()
                    else -> emptyList()
                }
            }
            .map { it.copy(depth = depth) }
    }
}
