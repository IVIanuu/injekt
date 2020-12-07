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
import com.ivianuu.injekt.Scoped
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny

@Scoped(GenerationComponent::class)
@Binding class DeclarationStore(val module: ModuleDescriptor) {

    fun constructorForComponent(type: TypeRef): Callable? {
        return classDescriptorForFqName(type.classifier.fqName)
            .unsubstitutedPrimaryConstructor
            ?.let { callableForDescriptor(it) }
    }

    private val internalIndices = mutableListOf<Index>()
    fun addInternalIndex(index: Index) {
        internalIndices += index
    }

    private val allIndices by unsafeLazy {
        internalIndices + (memberScopeForFqName(InjektFqNames.IndexPackage)
            ?.getContributedDescriptors(DescriptorKindFilter.VALUES)
            ?.filterIsInstance<PropertyDescriptor>()
            ?.map { indexProperty ->
                val annotation = indexProperty.annotations.findAnnotation(InjektFqNames.Index)!!
                val fqName = annotation.allValueArguments["fqName".asNameId()]!!.value as String
                val type = annotation.allValueArguments["type".asNameId()]!!.value as String
                Index(FqName(fqName), type)
            } ?: emptyList())
    }

    private val classIndices by unsafeLazy {
        allIndices
            .filter { it.type == "class" }
            .map { classDescriptorForFqName(it.fqName) }
    }

    private val functionIndices by unsafeLazy {
        allIndices
            .filter { it.type == "function" }
            .flatMap { functionDescriptorForFqName(it.fqName) }
    }

    private val propertyIndices by unsafeLazy {
        allIndices
            .filter { it.type == "property" }
            .flatMap { propertyDescriptorsForFqName(it.fqName) }
    }

    private val allBindings by unsafeLazy {
        (classIndices
            .mapNotNull { it.getInjectConstructor() }
            .map { callableForDescriptor(it) } +
                functionIndices
                    .map { callableForDescriptor(it) } +
                propertyIndices
                    .map { callableForDescriptor(it.getter!!) })
            .filter {
                it.contributionKind == Callable.ContributionKind.BINDING ||
                        it.contributionKind == Callable.ContributionKind.MODULE
            }
    }

    private val bindingsByType = mutableMapOf<TypeRef, List<Callable>>()
    fun bindingsForType(type: TypeRef): List<Callable> = bindingsByType.getOrPut(type) {
        allBindings
            .filter { it.type.isAssignableTo(type) }
            .distinct()
    }

    val allModules: List<Callable> by unsafeLazy {
        classIndices
            .filter { it.hasAnnotation(InjektFqNames.Module) }
            .map { it.getInjectConstructor()!! }
            .map { callableForDescriptor(it) } +
                functionIndices
                    .filter { it.hasAnnotation(InjektFqNames.Module) }
                    .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.Module) }
                    .map { callableForDescriptor(it.getter!!) }
    }

    private val allInterceptors by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.Interceptor) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.Interceptor) }
                    .map { callableForDescriptor(it.getter!!) }
    }
    private val interceptorsForType = mutableMapOf<TypeRef, List<Callable>>()
    fun interceptorsByType(providerType: TypeRef): List<Callable> =
        interceptorsForType.getOrPut(providerType) {
            allInterceptors
                .filter { it.type.isAssignableTo(providerType) }
        }

    private val allMapEntries by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
                    .map { callableForDescriptor(it.getter!!) }
    }
    private val mapEntriesForType = mutableMapOf<TypeRef, List<Callable>>()
    fun mapEntriesByType(type: TypeRef): List<Callable> = mapEntriesForType.getOrPut(type) {
        allMapEntries
            .filter { it.type.isAssignableTo(type) }
    }

    private val allSetElements by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.SetElements) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.SetElements) }
                    .map { callableForDescriptor(it.getter!!) }
    }
    private val setElementsForType = mutableMapOf<TypeRef, List<Callable>>()
    fun setElementsByType(type: TypeRef): List<Callable> = setElementsForType.getOrPut(type) {
        allSetElements
            .filter { it.type.isAssignableTo(type) }
    }

    val mergeComponents: List<TypeRef> by unsafeLazy {
        classIndices
            .filter { it.hasAnnotation(InjektFqNames.MergeComponent) }
            .map { it.toClassifierRef().defaultType }
    }

    private val allMergeDeclarationsByFqName by unsafeLazy {
        buildMap<FqName, MutableList<TypeRef>> {
            classIndices
                .filter { it.hasAnnotation(InjektFqNames.MergeInto) }
                .groupBy { declaration ->
                    declaration.annotations.findAnnotation(InjektFqNames.MergeInto)!!
                        .allValueArguments["component".asNameId()]!!
                        .let { it as KClassValue }
                        .getArgumentType(module)
                        .constructor
                        .declarationDescriptor!!
                        .fqNameSafe
                }
                .forEach { (mergeComponent, declarations) ->
                    getOrPut(mergeComponent) { mutableListOf() } += declarations.map {
                        it.toClassifierRef().defaultType
                    }
                }
        }
    }

    fun mergeDeclarationsForMergeComponent(component: FqName): List<TypeRef> =
        allMergeDeclarationsByFqName[component] ?: emptyList()

    private val callablesByType = mutableMapOf<TypeRef, List<Callable>>()
    fun allCallablesForType(type: TypeRef): List<Callable> {
        return callablesByType.getOrPut(type) {
            val callables = mutableListOf<Callable>()

            fun TypeRef.collect(typeArguments: List<TypeRef>) {
                val substitutionMap = classifier.typeParameters
                    .zip(typeArguments)
                    .toMap()

                callables += classDescriptorForFqName(classifier.fqName)
                    .unsubstitutedMemberScope
                    .getContributedDescriptors(DescriptorKindFilter.CALLABLES)
                    .filterIsInstance<CallableDescriptor>()
                    .filter { it.dispatchReceiverParameter?.type?.isAnyOrNullableAny() != true }
                    .mapNotNull {
                        when (it) {
                            is FunctionDescriptor -> callableForDescriptor(it)
                            is PropertyDescriptor -> callableForDescriptor(it.getter!!)
                            else -> null
                        }
                    }
                    .map { it.substitute(substitutionMap) }

                classifier.superTypes
                    .map { it.substitute(substitutionMap) }
                    .forEach { it.collect(it.typeArguments) }
            }

            type.collect(type.typeArguments)

            callables
        }
    }

    private val classifierDescriptorByFqName = mutableMapOf<FqName, ClassifierDescriptor>()
    fun classifierDescriptorForFqName(fqName: FqName): ClassifierDescriptor {
        return classifierDescriptorByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedClassifier(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ) ?: error("Could not get for $fqName")
        }
    }

    fun classDescriptorForFqName(fqName: FqName): ClassDescriptor =
        classifierDescriptorForFqName(fqName) as ClassDescriptor

    private val functionDescriptorsByFqName = mutableMapOf<FqName, List<FunctionDescriptor>>()
    fun functionDescriptorForFqName(fqName: FqName): List<FunctionDescriptor> {
        return functionDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedFunctions(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).toList()
        }
    }

    private val propertyDescriptorsByFqName = mutableMapOf<FqName, List<PropertyDescriptor>>()
    fun propertyDescriptorsForFqName(fqName: FqName): List<PropertyDescriptor> {
        return propertyDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedVariables(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).toList()
        }
    }

    private val memberScopeByFqName = mutableMapOf<FqName, MemberScope?>()
    fun memberScopeForFqName(fqName: FqName): MemberScope? {
        return memberScopeByFqName.getOrPut(fqName) {
            val pkg = module.getPackage(fqName)

            if (fqName.isRoot || pkg.fragments.isNotEmpty()) return@getOrPut pkg.memberScope

            val parentMemberScope = memberScopeForFqName(fqName.parent()) ?: return@getOrPut null

            val classDescriptor =
                parentMemberScope.getContributedClassifier(
                    fqName.shortName(),
                    NoLookupLocation.FROM_BACKEND
                ) as? ClassDescriptor ?: return@getOrPut null

            classDescriptor.unsubstitutedMemberScope
        }
    }

    private val callablesByDescriptor = mutableMapOf<Any, Callable>()
    fun callableForDescriptor(descriptor: FunctionDescriptor): Callable =
        callablesByDescriptor.getOrPut(descriptor) {
            val owner = when (descriptor) {
                is ConstructorDescriptor -> descriptor.constructedClass
                is PropertyAccessorDescriptor -> descriptor.correspondingProperty
                else -> descriptor
            }

            Callable(
                name = owner.name,
                packageFqName = descriptor.findPackage().fqName,
                fqName = owner.fqNameSafe,
                type = descriptor.returnType!!.toTypeRef(),
                targetComponent = descriptor.targetComponent(module)
                    ?: owner.targetComponent(module),
                scoped = descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.Scoped),
                eager = descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.Eager),
                default = descriptor.hasAnnotationWithPropertyAndClass(InjektFqNames.Default),
                contributionKind = descriptor.contributionKind(),
                typeParameters = (when (owner) {
                    is FunctionDescriptor -> owner.typeParameters
                    is ClassDescriptor -> owner.declaredTypeParameters
                    is PropertyDescriptor -> owner.typeParameters
                    else -> error("Unexpected owner $owner")
                }).map { it.toClassifierRef() },
                valueParameters = listOfNotNull(
                    descriptor.dispatchReceiverParameter?.let {
                        val parameterType = it.type.toTypeRef()
                        ValueParameterRef(
                            type = parameterType,
                            originalType = parameterType,
                            parameterKind = ValueParameterRef.ParameterKind.DISPATCH_RECEIVER,
                            name = "\$dispatchReceiver".asNameId(),
                            hasDefault = false,
                            defaultExpression = null
                        )
                    },
                    descriptor.extensionReceiverParameter?.let {
                        val parameterType = it.type.toTypeRef()
                        ValueParameterRef(
                            type = parameterType,
                            originalType = parameterType,
                            parameterKind = ValueParameterRef.ParameterKind.EXTENSION_RECEIVER,
                            name = "\$receiver".asNameId(),
                            hasDefault = false,
                            defaultExpression = null
                        )
                    }
                ) + descriptor.valueParameters.map {
                    val parameterType = it.type.toTypeRef()
                    ValueParameterRef(
                        type = parameterType,
                        originalType = parameterType,
                        parameterKind = ValueParameterRef.ParameterKind.VALUE_PARAMETER,
                        name = it.name,
                        hasDefault = it.declaresDefaultValue(),
                        defaultExpression = if (!it.declaresDefaultValue()) null else ({
                            emit((it.findPsi() as KtParameter).defaultValue!!.text)
                        })
                    )
                },
                isCall = owner !is PropertyDescriptor &&
                        (owner !is ClassDescriptor || owner.kind != ClassKind.OBJECT),
                callableKind = if (owner is CallableDescriptor) {
                    when {
                        owner.isSuspend -> Callable.CallableKind.SUSPEND
                        owner.hasAnnotation(InjektFqNames.Composable) -> Callable.CallableKind.COMPOSABLE
                        else -> Callable.CallableKind.DEFAULT
                    }
                } else Callable.CallableKind.DEFAULT,
                isInline = descriptor.isInline,
                visibility = descriptor.visibility,
                modality = descriptor.modality
            )
        }

    private val moduleByType =
        mutableMapOf<TypeRef, com.ivianuu.injekt.compiler.generator.ModuleDescriptor>()

    fun moduleForType(type: TypeRef): com.ivianuu.injekt.compiler.generator.ModuleDescriptor {
        val finalType = type.fullyExpandedType
        return moduleByType.getOrPut(finalType) {
            val descriptor = classDescriptorForFqName(finalType.classifier.fqName)
            val moduleSubstitutionMap = finalType.classifier.typeParameters
                .zip(finalType.typeArguments)
                .toMap()

            val callables =
                if (finalType.contributionKind != null && (finalType.isFunction || finalType.isSuspendFunction)) {
                    val invokeDescriptor =
                        descriptor.unsubstitutedMemberScope.getContributedDescriptors()
                            .first { it.name.asString() == "invoke" } as FunctionDescriptor
                    val callable = callableForDescriptor(invokeDescriptor)
                    val substitutionMap = moduleSubstitutionMap.toMutableMap()
                    val finalCallable = callable.copy(
                        type = callable.type.substitute(substitutionMap),
                        valueParameters = callable.valueParameters.map {
                            val parameterType = if (it.parameterKind ==
                                ValueParameterRef.ParameterKind.DISPATCH_RECEIVER
                            ) {
                                finalType
                            } else it.type
                            it.copy(
                                type = parameterType.substitute(substitutionMap)
                            )
                        },
                        targetComponent = finalType.targetComponent,
                        scoped = finalType.scoped,
                        eager = finalType.eager,
                        default = finalType.default,
                        contributionKind = finalType.contributionKind,
                        callableKind = finalType.callableKind
                    )
                    listOf(finalCallable)
                } else {
                    descriptor.unsubstitutedMemberScope.getContributedDescriptors().filter {
                        it.hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) ||
                                it.hasAnnotationWithPropertyAndClass(InjektFqNames.Interceptor) ||
                                it.hasAnnotationWithPropertyAndClass(InjektFqNames.SetElements) ||
                                it.hasAnnotationWithPropertyAndClass(InjektFqNames.MapEntries) ||
                                it.hasAnnotationWithPropertyAndClass(InjektFqNames.Module)
                    }
                        .mapNotNull {
                            when (it) {
                                is PropertyDescriptor -> it.getter!!
                                is FunctionDescriptor -> it
                                else -> null
                            }
                        }
                        .map { callableDescriptor ->
                            val callable = callableForDescriptor(callableDescriptor)
                            val substitutionMap = moduleSubstitutionMap.toMutableMap()
                            callable.copy(
                                type = callable.type.substitute(substitutionMap),
                                valueParameters = callable.valueParameters.map {
                                    val parameterType = if (it.parameterKind ==
                                        ValueParameterRef.ParameterKind.DISPATCH_RECEIVER
                                    ) {
                                        finalType
                                    } else it.type
                                    it.copy(
                                        type = parameterType.substitute(substitutionMap)
                                    )
                                }
                            )
                        }
                }

            ModuleDescriptor(type = finalType, callables = callables)
        }
    }

}
