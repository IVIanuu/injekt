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

import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.squareup.moshi.Moshi
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.Base64

@Suppress("NewApi")
class InjektContext(val module: ModuleDescriptor) {

    val moshi = Moshi.Builder().build()!!

    val setType by unsafeLazy {
        module.builtIns.set.defaultType.toTypeRef(this, null)
    }

    fun callableInfoFor(
        callable: CallableDescriptor,
        trace: BindingTrace?
    ): PersistedCallableInfo? {
        trace?.get(InjektWritableSlices.CALLABLE_INFO, callable)?.let { return it.value }
        val annotations = if (callable is ConstructorDescriptor &&
            callable.constructedClass.unsubstitutedPrimaryConstructor?.original == callable.original) {
            callable.constructedClass.annotations
        } else {
            callable.annotations
        }
        return annotations
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
            .also { trace?.record(InjektWritableSlices.CALLABLE_INFO, callable, Tuple1(it)) }
    }

    fun classifierInfoFor(
        classifier: ClassifierRef,
        trace: BindingTrace?
    ): PersistedClassifierInfo? {
        trace?.get(InjektWritableSlices.CLASSIFIER_INFO, classifier)?.let { return it.value }
        val classifierInfo = classifier.descriptor!!
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
                ?.let { callableInfoFor(it, trace) }
                ?.typeParameters
                ?.singleOrNull {
                    val fqName = FqName(it.key.split(":")[1])
                    fqName == classifier.fqName
                }
                ?.toPersistedClassifierInfo()
        trace?.record(InjektWritableSlices.CLASSIFIER_INFO, classifier, Tuple1(classifierInfo))
        return classifierInfo
    }

    fun classifierDescriptorForFqName(fqName: FqName): ClassifierDescriptor? {
        return memberScopeForFqName(fqName.parent())!!.getContributedClassifier(
            fqName.shortName(), NoLookupLocation.FROM_BACKEND
        )
    }

    fun classifierDescriptorForKey(
        key: String,
        trace: BindingTrace?
    ): ClassifierDescriptor {
        trace?.get(InjektWritableSlices.CLASSIFIER_FOR_KEY, key)?.let { return it }
        val fqName = FqName(key.split(":")[1])
        val classifier = memberScopeForFqName(fqName.parent())?.getContributedClassifier(
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
        trace?.record(InjektWritableSlices.CLASSIFIER_FOR_KEY, key, classifier)
        return classifier
    }

    fun functionDescriptorsForFqName(fqName: FqName): List<FunctionDescriptor> {
        return memberScopeForFqName(fqName.parent())?.getContributedFunctions(
            fqName.shortName(), NoLookupLocation.FROM_BACKEND
        )?.toList() ?: emptyList()
    }

    private fun propertyDescriptorsForFqName(fqName: FqName): List<PropertyDescriptor> {
        return memberScopeForFqName(fqName.parent())?.getContributedVariables(
            fqName.shortName(), NoLookupLocation.FROM_BACKEND
        )?.toList() ?: emptyList()
    }

    fun memberScopeForFqName(fqName: FqName): MemberScope? {
        val pkg = module.getPackage(fqName)

        if (fqName.isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope

        val parentMemberScope = memberScopeForFqName(fqName.parent()) ?: return null

        val classDescriptor =
            parentMemberScope.getContributedClassifier(
                fqName.shortName(),
                NoLookupLocation.FROM_BACKEND
            ) as? ClassDescriptor ?: return null

        return classDescriptor.unsubstitutedMemberScope
    }

}
