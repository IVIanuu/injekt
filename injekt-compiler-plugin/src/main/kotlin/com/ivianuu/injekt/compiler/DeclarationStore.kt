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

import com.ivianuu.injekt.compiler.analysis.Index
import com.ivianuu.injekt.compiler.resolution.allGivenTypes
import com.ivianuu.injekt.compiler.resolution.getGivenConstructors
import com.ivianuu.injekt.compiler.resolution.overrideType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class DeclarationStore {

    lateinit var module: ModuleDescriptor

    var generatedCode = false
        set(value) {
            field = value
            memberScopeByFqName.clear()
        }

    private val allIndices by unsafeLazy {
        check(generatedCode)
        (memberScopeForFqName(InjektFqNames.IndexPackage)
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

    val globalGivens: List<CallableDescriptor> by unsafeLazy {
        classIndices
            .flatMap { it.getGivenConstructors() }
            .flatMap { constructor ->
                constructor.allGivenTypes()
                    .map { constructor.overrideType(it) }
            } +
                functionIndices
                    .filter { it.hasAnnotationWithPropertyAndClass(InjektFqNames.Given) }
                    .filter {
                        val receiverClass =
                            it.dispatchReceiverParameter?.value?.type?.constructor?.declarationDescriptor
                                ?.safeAs<ClassDescriptor>()
                        receiverClass == null || receiverClass.kind == ClassKind.OBJECT
                    } +
                propertyIndices
                    .filter { it.hasAnnotationWithPropertyAndClass(InjektFqNames.Given) }
                    .filter {
                        val receiverClass =
                            it.dispatchReceiverParameter?.value?.type?.constructor?.declarationDescriptor
                                ?.safeAs<ClassDescriptor>()
                        receiverClass == null || receiverClass.kind == ClassKind.OBJECT
                    }
    }

    val globalGivenSetElements by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.GivenSetElement) }
            .filter {
                val receiverClass =
                    it.dispatchReceiverParameter?.value?.type?.constructor?.declarationDescriptor
                        ?.safeAs<ClassDescriptor>()
                receiverClass == null || receiverClass.kind == ClassKind.OBJECT
            } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.GivenSetElement) }
                    .filter {
                        val receiverClass =
                            it.dispatchReceiverParameter?.value?.type?.constructor?.declarationDescriptor
                                ?.safeAs<ClassDescriptor>()
                        receiverClass == null || receiverClass.kind == ClassKind.OBJECT
                    }
    }

    private val classifierDescriptorByFqName = mutableMapOf<FqName, ClassifierDescriptor>()
    private fun classifierDescriptorForFqName(fqName: FqName): ClassifierDescriptor {
        check(generatedCode)
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
        check(generatedCode)
        return functionDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedFunctions(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).toList()
        }
    }

    private val propertyDescriptorsByFqName = mutableMapOf<FqName, List<PropertyDescriptor>>()
    private fun propertyDescriptorsForFqName(fqName: FqName): List<PropertyDescriptor> {
        check(generatedCode)
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
