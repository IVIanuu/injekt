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
import com.ivianuu.injekt.compiler.resolution.*
import com.squareup.moshi.Moshi
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.Base64

@Suppress("NewApi")
class DeclarationStore(val module: ModuleDescriptor) {

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

    val globalContributions by unsafeLazy {
        classIndices
            .flatMap { it.getContributionConstructors(this) } +
                functionIndices
                    .map { it.toCallableRef(this) }
                    .filter { it.contributionKind != null } +
                propertyIndices
                    .map { it.toCallableRef(this) }
                    .filter { it.contributionKind != null }
    }

    val givenFuns by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.GivenFun) }
            .map {
                it.toCallableRef(this) to classifierDescriptorForFqName(it.fqNameSafe)
                    .toClassifierRef(this)
            }
    }

    val moshi = Moshi.Builder().build()

    private val callableInfosByDeclaration = mutableMapOf<Any, PersistedCallableInfo?>()
    fun callableInfoFor(callable: CallableDescriptor): PersistedCallableInfo? =
        callableInfosByDeclaration.getOrPut(callable.original) {
            callable
                .annotations
                .findAnnotation(InjektFqNames.CallableInfo)
                ?.allValueArguments
                ?.get("value".asNameId())
                ?.value
                ?.cast<String>()
                ?.let { encoded ->
                    val json = Base64.getDecoder()
                        .decode(encoded)
                        .decodeToString()
                    val info = moshi.adapter(PersistedCallableInfo::class.java).fromJson(json)!!
                    info
                }
        }

    private val classifierInfosByDeclaration = mutableMapOf<Any, PersistedClassifierInfo?>()
    fun classifierInfoFor(classifier: ClassifierRef): PersistedClassifierInfo? =
        classifierInfosByDeclaration.getOrPut(classifier.descriptor!!.original) {
            classifier.descriptor
                .annotations
                .findAnnotation(InjektFqNames.ClassifierInfo)
                ?.allValueArguments
                ?.get("value".asNameId())
                ?.value
                ?.cast<String>()
                ?.let { encoded ->
                    val json = Base64.getDecoder()
                        .decode(encoded)
                        .decodeToString()
                    moshi.adapter(PersistedClassifierInfo::class.java).fromJson(json)!!
                }
                ?: classifier.toPersistedClassifierInfo(this@DeclarationStore)
        }
    fun classifierInfoFor(classifier: ClassifierDescriptor): PersistedClassifierInfo? =
        classifierInfoFor(classifier.toClassifierRef(this))

    private val classifierDescriptorByFqName = mutableMapOf<FqName, ClassifierDescriptor>()
    fun classifierDescriptorForFqName(fqName: FqName): ClassifierDescriptor {
        return classifierDescriptorByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedClassifier(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ) ?: error("Could not get for '$fqName'")
        }
    }

    private val classifierDescriptorByKey = mutableMapOf<String, ClassifierDescriptor>()
    fun classifierDescriptorForKey(key: String): ClassifierDescriptor {
        return classifierDescriptorByKey.getOrPut(key) {
            val fqName = FqName(key.split(":")[1])
            return@getOrPut memberScopeForFqName(fqName.parent())?.getContributedClassifier(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            )?.takeIf { it.uniqueKey(this) == key }
                ?: functionDescriptorForFqName(fqName.parent())
                    .flatMap { it.typeParameters }
                    .firstOrNull {
                        it.uniqueKey(this@DeclarationStore) == key
                    }
                ?: propertyDescriptorsForFqName(fqName.parent())
                    .flatMap { it.typeParameters }
                    .firstOrNull {
                        it.uniqueKey(this@DeclarationStore) == key
                    }
                ?: classifierDescriptorForFqName(fqName.parent())
                    .safeAs<ClassifierDescriptorWithTypeParameters>()
                    ?.declaredTypeParameters
                    ?.firstOrNull { it.uniqueKey(this@DeclarationStore) == key }
                ?: error("Could not get for $fqName $key")
        }
    }

    private fun classDescriptorForFqName(fqName: FqName): ClassDescriptor =
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
