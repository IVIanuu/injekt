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

import com.ivianuu.injekt.compiler.resolution.TypeCheckerContext
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt_shaded.Inject1
import com.ivianuu.injekt_shaded.inject
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

typealias WithInjektContext = Inject1<InjektContext>

@Inject1<InjektContext> inline val _context: InjektContext
  get() = inject()

@Inject1<InjektContext> inline val injektFqNames: InjektFqNames
  get() = _context.injektFqNames

@Inject1<InjektContext> inline val trace: BindingTrace?
  get() = _context.trace

@Inject1<InjektContext> inline val module: ModuleDescriptor
  get() = _context.module

@Suppress("NewApi")
class InjektContext(
  val module: ModuleDescriptor,
  val injektFqNames: InjektFqNames,
  val trace: BindingTrace?
) : TypeCheckerContext {
  fun withTrace(trace: BindingTrace?) = InjektContext(module, injektFqNames, trace)

  override val injektContext: InjektContext
    get() = this

  override fun isDenotable(type: TypeRef): Boolean = true

  val listClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.list.toClassifierRef() }
  val collectionClassifier by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.collection.toClassifierRef() }
  val nullableNothingType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.nullableNothingType.toTypeRef() }
  val anyType by lazy(LazyThreadSafetyMode.NONE) { module.builtIns.anyType.toTypeRef() }
  val nullableAnyType by lazy(LazyThreadSafetyMode.NONE) {
    anyType.copy(isMarkedNullable = true)
  }
  val typeKeyType by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(
      ClassId.topLevel(injektFqNames.typeKey)
    )!!.toClassifierRef()
  }
  val componentObserverType by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(
      ClassId.topLevel(injektFqNames.componentObserver)
    )!!.toClassifierRef()
  }
  val disposableType by lazy(LazyThreadSafetyMode.NONE) {
    module.findClassAcrossModuleDependencies(
      ClassId.topLevel(injektFqNames.disposable)
    )!!.toClassifierRef()
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
    val classifier = memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)
    ?.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
      ?.takeIf { it.uniqueKey() == key }
      ?: functionDescriptorsForFqName(fqName.parent())
        .flatMap { it.typeParameters }
        .firstOrNull {
          it.uniqueKey() == key
        }
      ?: propertyDescriptorsForFqName(fqName.parent())
        .flatMap { it.typeParameters }
        .firstOrNull {
          it.uniqueKey() == key
        }
      ?: classifierDescriptorForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)
        .safeAs<ClassifierDescriptorWithTypeParameters>()
        ?.declaredTypeParameters
        ?.firstOrNull { it.uniqueKey() == key }
      ?: error("Could not get for $fqName $key")
    classifierForKey[key] = classifier
    return classifier
  }

  private fun functionDescriptorsForFqName(fqName: FqName): Collection<FunctionDescriptor> =
    memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)?.getContributedFunctions(
      fqName.shortName(), NoLookupLocation.FROM_BACKEND
    ) ?: emptyList()

  private fun propertyDescriptorsForFqName(fqName: FqName): Collection<PropertyDescriptor> =
    memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND)?.getContributedVariables(
      fqName.shortName(), NoLookupLocation.FROM_BACKEND
    ) ?: emptyList()

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
