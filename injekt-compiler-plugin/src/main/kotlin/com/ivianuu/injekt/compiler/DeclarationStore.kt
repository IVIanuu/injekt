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
import com.ivianuu.injekt.Scoped
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.isAssignableTo
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

@Scoped(ApplicationComponent::class)
@Binding class DeclarationStore {

    lateinit var module: ModuleDescriptor

    private val internalIndices = mutableListOf<Index>()
    fun addInternalIndex(index: Index) {
        internalIndices += index
    }

    private val allIndices by unsafeLazy {
        internalIndices + (memberScopeForFqName(InjektFqNames.IndexPackage)
            ?.getContributedDescriptors(DescriptorKindFilter.VALUES)
            ?.filterIsInstance<PropertyDescriptor>()
            ?.filter { it.hasAnnotation(InjektFqNames.Index) }
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

    private val globalGivens by unsafeLazy {
        (classIndices
            .mapNotNull { it.getGivenConstructor() } +
                functionIndices +
                propertyIndices)
            .filter { it.hasAnnotationWithPropertyAndClass(InjektFqNames.Given) }
    }

    private val givensByType = mutableMapOf<TypeRef, List<CallableDescriptor>>()
    fun givensForType(type: TypeRef): List<CallableDescriptor> = givensByType.getOrPut(type) {
        globalGivens
            .filter { it.returnType!!.toTypeRef().isAssignableTo(type) }
            .distinct()
    }

    private val allGivenInfos: Map<String, GivenInfo> by unsafeLazy {
        (memberScopeForFqName(InjektFqNames.IndexPackage)
            ?.getContributedDescriptors(DescriptorKindFilter.VALUES)
            ?.filterIsInstance<PropertyDescriptor>()
            ?.filter { it.hasAnnotation(InjektFqNames.GivenInfo) }
            ?.map { givenInfoProperty ->
                val annotation =
                    givenInfoProperty.annotations.findAnnotation(InjektFqNames.GivenInfo)!!
                val fqName = annotation.allValueArguments["fqName".asNameId()]!!.value as String
                val key = annotation.allValueArguments["key".asNameId()]!!.value as String
                val requiredGivens =
                    annotation.allValueArguments["requiredGivens".asNameId()]!!
                        .let { it as ArrayValue }
                        .value
                        .filterIsInstance<StringValue>()
                        .map { it.value.asNameId() }
                val givensWithDefault =
                    annotation.allValueArguments["givensWithDefault".asNameId()]!!
                        .let { it as ArrayValue }
                        .value
                        .filterIsInstance<StringValue>()
                        .map { it.value.asNameId() }

                GivenInfo(FqName(fqName), key, requiredGivens, givensWithDefault)
            } ?: emptyList())
            .associateBy { it.key }
    }

    private val givenInfosByKey = mutableMapOf<String, GivenInfo>()
    fun addGivenInfoForKey(key: String, info: GivenInfo) {
        givenInfosByKey[key] = info
    }

    fun givenInfoForCallable(callable: CallableDescriptor): GivenInfo {
        val key = when (callable) {
            is PropertyDescriptor -> callable.uniqueKey()
            is ConstructorDescriptor -> callable.constructedClass.uniqueKey()
            is PropertyAccessorDescriptor -> callable.correspondingProperty.uniqueKey()
            is FunctionDescriptor -> callable.uniqueKey()
            is ReceiverParameterDescriptor -> callable.uniqueKey()
            is ValueParameterDescriptor -> callable.uniqueKey()
            else -> error("Unexpected callable $callable")
        }
        return givenInfosByKey.getOrPut(key) {
            allGivenInfos.getOrElse(key) {
                GivenInfo(
                    callable.fqNameSafe,
                    key,
                    emptyList(),
                    emptyList()
                )
            }
        }
    }

    private val classifierDescriptorByFqName = mutableMapOf<FqName, ClassifierDescriptor>()
    private fun classifierDescriptorForFqName(fqName: FqName): ClassifierDescriptor {
        return classifierDescriptorByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedClassifier(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ) ?: error("Could not get for $fqName")
        }
    }

    private fun classDescriptorForFqName(fqName: FqName): ClassDescriptor =
        classifierDescriptorForFqName(fqName) as ClassDescriptor

    private val functionDescriptorsByFqName = mutableMapOf<FqName, List<FunctionDescriptor>>()
    private fun functionDescriptorForFqName(fqName: FqName): List<FunctionDescriptor> {
        return functionDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedFunctions(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).toList()
        }
    }

    private val propertyDescriptorsByFqName = mutableMapOf<FqName, List<PropertyDescriptor>>()
    private fun propertyDescriptorsForFqName(fqName: FqName): List<PropertyDescriptor> {
        return propertyDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedVariables(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).toList()
        }
    }

    private val memberScopeByFqName = mutableMapOf<FqName, MemberScope?>()
    private fun memberScopeForFqName(fqName: FqName): MemberScope? {
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

}
