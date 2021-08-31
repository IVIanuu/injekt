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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.compiler.injektContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.sam.SamConversionOracle
import org.jetbrains.kotlin.resolve.sam.SamConversionResolver
import org.jetbrains.kotlin.resolve.scopes.SyntheticScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.synthetic.FunInterfaceConstructorsScopeProvider
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.synthetic.JavaSyntheticPropertiesScope
import org.jetbrains.kotlin.synthetic.SyntheticScopeProviderExtension
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class InjectSyntheticScopeProviderExtension(
  private val isEnabled: (ModuleDescriptor) -> Boolean = { true }
) : SyntheticScopeProviderExtension {
  override fun getScopes(
    moduleDescriptor: ModuleDescriptor,
    javaSyntheticPropertiesScope: JavaSyntheticPropertiesScope
  ): List<SyntheticScope> =
    if (isEnabled(moduleDescriptor))
      listOf(InjectSyntheticScope(AnalysisContext(moduleDescriptor.injektContext, null)))
    else emptyList()
}

class InjectSyntheticScopes(
  storageManager: StorageManager,
  lookupTracker: LookupTracker,
  samResolver: SamConversionResolver,
  samConversionOracle: SamConversionOracle,
  moduleDescriptor: ModuleDescriptor
) : SyntheticScopes {
  private val delegate = FunInterfaceConstructorsScopeProvider(
    storageManager, lookupTracker, samResolver, samConversionOracle)
  override val scopes: Collection<SyntheticScope> = delegate.scopes +
      InjectSyntheticScope(AnalysisContext(moduleDescriptor.injektContext, null))
}

private class InjectSyntheticScope(
  @Inject private val context: AnalysisContext
) : SyntheticScope.Default() {
  override fun getSyntheticConstructor(constructor: ConstructorDescriptor): ConstructorDescriptor? =
    constructor.toInjectFunctionDescriptor() as? ConstructorDescriptor

  override fun getSyntheticConstructors(
    contributedClassifier: ClassifierDescriptor,
    location: LookupLocation
  ): Collection<FunctionDescriptor> = contributedClassifier.safeAs<ClassDescriptor>()
    ?.constructors
    ?.mapNotNull { it.toInjectFunctionDescriptor() } ?: emptyList()

  override fun getSyntheticMemberFunctions(receiverTypes: Collection<KotlinType>): Collection<FunctionDescriptor> =
    receiverTypes
      .flatMap { receiverType ->
        receiverType.memberScope.getContributedDescriptors()
          .flatMap { declaration ->
            if (declaration is ClassDescriptor && declaration.isInner) {
              declaration.constructors
            } else listOfNotNull(declaration as? FunctionDescriptor)
          }
      }
      .mapNotNull { it.toInjectFunctionDescriptor() }

  override fun getSyntheticMemberFunctions(
    receiverTypes: Collection<KotlinType>,
    name: Name,
    location: LookupLocation
  ): Collection<FunctionDescriptor> = receiverTypes
    .flatMap { receiverType ->
      receiverType.memberScope.getContributedFunctions(name, location) +
          (receiverType.memberScope.getContributedClassifier(name, location)
            ?.safeAs<ClassDescriptor>()
            ?.takeIf { it.isInner }
            ?.constructors
            ?: emptyList())
    }
    .mapNotNull { it.toInjectFunctionDescriptor() }

  override fun getSyntheticStaticFunctions(
    contributedFunctions: Collection<FunctionDescriptor>,
    location: LookupLocation
  ): Collection<FunctionDescriptor> = contributedFunctions
    .mapNotNull { it.toInjectFunctionDescriptor() }
}
