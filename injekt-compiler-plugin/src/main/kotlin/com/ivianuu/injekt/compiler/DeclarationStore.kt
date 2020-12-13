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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class DeclarationStore {

    lateinit var module: ModuleDescriptor

    var generatedCode = false

    private val allIndices by unsafeLazy {
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

    val globalGivenCollectionElements by unsafeLazy {
        functionIndices
            .filter {
                it.hasAnnotation(InjektFqNames.GivenMap) ||
                        it.hasAnnotation(InjektFqNames.GivenSet)
            }
            .filter {
                val receiverClass =
                    it.dispatchReceiverParameter?.value?.type?.constructor?.declarationDescriptor
                        ?.safeAs<ClassDescriptor>()
                receiverClass == null || receiverClass.kind == ClassKind.OBJECT
            } +
                propertyIndices
                    .filter {
                        it.hasAnnotation(InjektFqNames.GivenMap) ||
                                it.hasAnnotation(InjektFqNames.GivenSet)
                    }
                    .filter {
                        val receiverClass =
                            it.dispatchReceiverParameter?.value?.type?.constructor?.declarationDescriptor
                                ?.safeAs<ClassDescriptor>()
                        receiverClass == null || receiverClass.kind == ClassKind.OBJECT
                    }
    }

    private val allGivenInfos: Map<String, GivenInfo> by unsafeLazy {
        check(generatedCode)
        (memberScopeForFqName(InjektFqNames.IndexPackage)
            ?.getContributedDescriptors(DescriptorKindFilter.VALUES)
            ?.filterIsInstance<PropertyDescriptor>()
            ?.filter { it.hasAnnotation(InjektFqNames.GivenInfo) }
            ?.map { givenInfoProperty ->
                val annotation =
                    givenInfoProperty.annotations.findAnnotation(InjektFqNames.GivenInfo)!!
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

                GivenInfo(key, requiredGivens, givensWithDefault)
            } ?: emptyList())
            .associateBy { it.key }
    }

    private val givenInfosByKey = mutableMapOf<String, GivenInfo>()

    fun givenInfoFor(declaration: DeclarationDescriptor): GivenInfo {
        return internalGivenInfoFor(declaration) ?: kotlin.run {
            val key = declaration.uniqueKey()
            givenInfosByKey.getOrPut(key) {
                allGivenInfos.getOrElse(key) {
                    createGivenInfoOrNull(declaration.original) ?: GivenInfo.Empty
                }
            }
        }
    }

    private val internalGivenInfosByKey = mutableMapOf<String, GivenInfo?>()
    fun internalGivenInfoFor(declaration: DeclarationDescriptor): GivenInfo? {
        val key = declaration.uniqueKey()
        return internalGivenInfosByKey.getOrPut(key) {
            createGivenInfoOrNull(declaration.original)
        }
    }

    private fun createGivenInfoOrNull(descriptor: DeclarationDescriptor): GivenInfo? {
        val declaration = descriptor.findPsi()
            ?.let { it as? KtDeclaration }
            ?: return null
        val givens: List<Pair<Name, String?>> = when (declaration) {
            is KtConstructor<*> -> declaration.valueParameters
                .filter {
                    it.defaultValue?.text == "given" ||
                            it.defaultValue?.text == "givenOrElse"
                }
                .map { it.nameAsSafeName to it.defaultValue?.text }
            is KtFunction -> {
                (if (declaration.receiverTypeReference?.hasAnnotation(InjektFqNames.Given) == true)
                    listOf("_receiver".asNameId() to null) else emptyList()) +
                        declaration.valueParameters
                            .filter {
                                it.defaultValue?.text == "given" ||
                                        it.defaultValue?.text == "givenOrElse"
                            }
                            .map { it.nameAsSafeName to it.defaultValue?.text }
            }
            is KtProperty -> listOfNotNull(
                if (declaration.receiverTypeReference?.hasAnnotation(InjektFqNames.Given) == true)
                    "_receiver".asNameId() to null
                else null
            )
            else -> return null
        }

        val (requiredGivens, givensWithDefault) = givens
            .partition { (_, defaultValue) -> defaultValue == null || defaultValue == "given" }

        if (requiredGivens.isNotEmpty() || givensWithDefault.isNotEmpty()) {
            return GivenInfo(
                descriptor.uniqueKey(),
                requiredGivens.map { it.first },
                givensWithDefault.map { it.first },
            )
        }

        return null
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
