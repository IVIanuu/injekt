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

import com.ivianuu.injekt.compiler.index.IndexStore
import com.ivianuu.injekt.compiler.resolution.*
import com.squareup.moshi.Moshi
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.Base64

@Suppress("NewApi")
class DeclarationStore(
    private val indexStore: IndexStore,
    val module: ModuleDescriptor
) {

    private val allIndices by unsafeLazy { indexStore.indices }

    private val classIndices by unsafeLazy {
        allIndices
            .filter { it.type == "class" }
            .mapNotNull {
                try {
                    classDescriptorForFqName(it.fqName)
                } catch (e: Throwable) {
                    null
                }
            }
    }

    private val functionIndices by unsafeLazy {
        allIndices
            .filter { it.type == "function" }
            .flatMap { functionDescriptorsForFqName(it.fqName) }
    }

    private val propertyIndices by unsafeLazy {
        allIndices
            .filter { it.type == "property" }
            .flatMap { propertyDescriptorsForFqName(it.fqName) }
    }

    val globalGivens by unsafeLazy {
        classIndices
            .flatMap { it.getGivenConstructors(this) } +
                functionIndices
                    .map { it.toCallableRef(this) }
                    .filter { it.isGiven } +
                propertyIndices
                    .map { it.toCallableRef(this) }
                    .filter { it.isGiven }
    }

    val moshi = Moshi.Builder().build()!!

    val isAssignableCache = mutableMapOf<Any, Boolean>()
    val isSubTypeCache = mutableMapOf<Any, Boolean>()
    val uniqueKeys = mutableMapOf<DeclarationDescriptor, String>()

    private val callableInfosByDeclaration = mutableMapOf<Any, PersistedCallableInfo?>()
    fun callableInfoFor(callable: CallableDescriptor): PersistedCallableInfo? =
        callableInfosByDeclaration.getOrPut(callable.original) {
            val annotations = if (callable is ConstructorDescriptor &&
                    callable.constructedClass.unsubstitutedPrimaryConstructor?.original == callable.original) {
                callable.constructedClass.annotations
            } else {
                callable.annotations
            }
            annotations
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
                ?: classifier.descriptor
                    .containingDeclaration
                    .safeAs<CallableDescriptor>()
                    ?.let { callableInfoFor(it) }
                    ?.typeParameters
                    ?.singleOrNull {
                        val fqName = FqName(it.key.split(":")[1])
                        fqName == classifier.fqName
                    }
                    ?.toPersistedClassifierInfo()
        }

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
                ?: functionDescriptorsForFqName(fqName.parent())
                    .flatMap { it.typeParameters }
                    .firstOrNull {
                        it.uniqueKey(this) == key
                    }
                ?: propertyDescriptorsForFqName(fqName.parent())
                    .flatMap { it.typeParameters }
                    .firstOrNull {
                        it.uniqueKey(this) == key
                    }
                ?: classifierDescriptorForFqName(fqName.parent())
                    .safeAs<ClassifierDescriptorWithTypeParameters>()
                    ?.declaredTypeParameters
                    ?.firstOrNull { it.uniqueKey(this) == key }
                ?: error("Could not get for $fqName $key")
        }
    }

    private fun classDescriptorForFqName(fqName: FqName): ClassDescriptor =
        classifierDescriptorForFqName(fqName) as ClassDescriptor

    private val functionDescriptorsByFqName = mutableMapOf<FqName, List<FunctionDescriptor>>()
    fun functionDescriptorsForFqName(fqName: FqName): List<FunctionDescriptor> {
        return functionDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())?.getContributedFunctions(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            )?.toList() ?: emptyList()
        }
    }

    private val propertyDescriptorsByFqName = mutableMapOf<FqName, List<PropertyDescriptor>>()
    private fun propertyDescriptorsForFqName(fqName: FqName): List<PropertyDescriptor> {
        return propertyDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())?.getContributedVariables(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            )?.toList() ?: emptyList()
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

    val setType by unsafeLazy {
        module.builtIns.set.defaultType.toTypeRef(this)
    }

}
