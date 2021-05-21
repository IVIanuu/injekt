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

import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.utils.addToStdlib.*

@Suppress("NewApi")
class InjektContext(val module: ModuleDescriptor) : TypeCheckerContext {
  override val injektContext: InjektContext
    get() = this

  override fun isDenotable(type: TypeRef): Boolean = true

  val trace = CliBindingTrace()

  val setClassifier by unsafeLazy {
    module.builtIns.set.toClassifierRef(this, trace)
  }
  val collectionClassifier by unsafeLazy {
    module.builtIns.collection.toClassifierRef(this, trace)
  }
  val nothingType by unsafeLazy {
    module.builtIns.nothingType.toTypeRef(this, trace)
  }
  val nullableNothingType by unsafeLazy {
    nothingType.copy(isMarkedNullable = true)
  }
  val anyType by unsafeLazy {
    module.builtIns.anyType.toTypeRef(this, trace)
  }
  val nullableAnyType by unsafeLazy {
    anyType.copy(isMarkedNullable = true)
  }

  fun classifierDescriptorForFqName(
    fqName: FqName,
    lookupLocation: LookupLocation
  ): ClassifierDescriptor? = memberScopeForFqName(fqName.parent(), lookupLocation)
    ?.getContributedClassifier(fqName.shortName(), lookupLocation)

  fun classifierDescriptorForKey(key: String, trace: BindingTrace): ClassifierDescriptor {
    trace.get(InjektWritableSlices.CLASSIFIER_FOR_KEY, key)?.let { return it }
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
    trace.record(InjektWritableSlices.CLASSIFIER_FOR_KEY, key, classifier)
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
