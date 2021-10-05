/*
 * Copyright 2021 Manuel Wrage
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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@Suppress("NewApi")
class InjektContext(val module: ModuleDescriptor) {
  val typeKey by lazy {
    module.findClassAcrossModuleDependencies(
      ClassId.topLevel(InjektFqNames.TypeKey)
    )!!
  }

  fun classifierDescriptorForFqName(
    fqName: FqName,
    lookupLocation: LookupLocation
  ): ClassifierDescriptor? {
    return if (fqName.isRoot) null
    else memberScopeForFqName(fqName.parent(), lookupLocation)
      ?.getContributedClassifier(fqName.shortName(), lookupLocation)
  }

  private val classifierForKey = mutableMapOf<String, ClassifierDescriptor>()

  fun classifierDescriptorForKey(key: String): ClassifierDescriptor {
    classifierForKey[key]?.let { return it }
    val fqName = FqName(key.split(":")[1])
    val classifier = memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)?.getContributedClassifier(
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
      ?: classifierDescriptorForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)
        .safeAs<ClassifierDescriptorWithTypeParameters>()
        ?.declaredTypeParameters
        ?.firstOrNull { it.uniqueKey(this) == key }
      ?: error("Could not get for $fqName $key")
    classifierForKey[key] = classifier
    return classifier
  }

  private fun functionDescriptorsForFqName(fqName: FqName): List<FunctionDescriptor> =
    memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)?.getContributedFunctions(
      fqName.shortName(), NoLookupLocation.FROM_BACKEND
    )?.toList() ?: emptyList()

  private fun propertyDescriptorsForFqName(fqName: FqName): List<PropertyDescriptor> =
    memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)?.getContributedVariables(
      fqName.shortName(), NoLookupLocation.FROM_BACKEND
    )?.toList() ?: emptyList()

  fun memberScopeForFqName(fqName: FqName, lookupLocation: LookupLocation): MemberScope? {
    val pkg = module.getPackage(fqName)

    if (fqName.isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope

    val parentMemberScope = memberScopeForFqName(fqName.parent(), lookupLocation) ?: return null

    val classDescriptor =
      parentMemberScope.getContributedClassifier(
        fqName.shortName(),
        lookupLocation
      ) as? ClassDescriptor ?: return null

    return classDescriptor.unsubstitutedMemberScope
  }

  fun packageFragmentsForFqName(fqName: FqName): List<PackageFragmentDescriptor> =
    module.getPackage(fqName).fragments
}

val InjektContextModuleCapability = ModuleCapability<InjektContext>("InjektContext")

val ModuleDescriptor.injektContext
  get() = getCapability(InjektContextModuleCapability)
    ?: InjektContext(this)
      .also { newContext ->
        updatePrivateFinalField<Map<ModuleCapability<*>, Any?>>(
          ModuleDescriptorImpl::class,
          "capabilities"
        ) {
          toMutableMap()
            .also { it[InjektContextModuleCapability] = newContext }
        }
      }
