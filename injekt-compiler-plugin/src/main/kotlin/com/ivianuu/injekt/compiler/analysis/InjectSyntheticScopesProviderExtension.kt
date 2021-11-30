/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.transform
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
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
  private val injektFqNames: (ModuleDescriptor) -> InjektFqNames,
  private val isEnabled: (ModuleDescriptor) -> Boolean = { true }
) : SyntheticScopeProviderExtension {
  override fun getScopes(
    moduleDescriptor: ModuleDescriptor,
    javaSyntheticPropertiesScope: JavaSyntheticPropertiesScope
  ): List<SyntheticScope> {
    @Provide val ctx = Context(
      moduleDescriptor, injektFqNames(moduleDescriptor),
      DelegatingBindingTrace(BindingContext.EMPTY, "synthetic scopes")
    )
    return if (isEnabled(moduleDescriptor))
      listOf(InjectSyntheticScope())
    else emptyList()
  }
}

class InjectSyntheticScopes(
  storageManager: StorageManager,
  lookupTracker: LookupTracker,
  samResolver: SamConversionResolver,
  samConversionOracle: SamConversionOracle,
  @Inject ctx: Context
) : SyntheticScopes {
  private val delegate = FunInterfaceConstructorsScopeProvider(
    storageManager, lookupTracker, samResolver, samConversionOracle)
  override val scopes: Collection<SyntheticScope> = delegate.scopes + InjectSyntheticScope(
    ctx.withTrace(
      DelegatingBindingTrace(BindingContext.EMPTY, "synthetic scopes")
    )
  )
}

private class InjectSyntheticScope(@Inject private val ctx: Context) : SyntheticScope.Default() {
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
      .transform { receiverType ->
        for (declaration in receiverType.memberScope.getContributedDescriptors()) {
          if (declaration is ClassDescriptor && declaration.isInner) {
            for (constructor in declaration.constructors)
              constructor.toInjectFunctionDescriptor()
                ?.let { add(it) }
          } else
            declaration.safeAs<FunctionDescriptor>()?.toInjectFunctionDescriptor()
              ?.let { add(it) }
        }
      }

  override fun getSyntheticMemberFunctions(
    receiverTypes: Collection<KotlinType>,
    name: Name,
    location: LookupLocation
  ): Collection<FunctionDescriptor> = receiverTypes
    .transform { receiverType ->
      for (function in receiverType.memberScope.getContributedFunctions(name, location))
        function.toInjectFunctionDescriptor()?.let { add(it) }
      receiverType.memberScope.getContributedClassifier(name, location)
        ?.safeAs<ClassDescriptor>()
        ?.takeIf { it.isInner }
        ?.constructors
        ?.forEach { constructor ->
          constructor.toInjectFunctionDescriptor()?.let { add(it) }
        }
    }

  override fun getSyntheticStaticFunctions(
    contributedFunctions: Collection<FunctionDescriptor>,
    location: LookupLocation
  ): Collection<FunctionDescriptor> = contributedFunctions
    .mapNotNull { it.toInjectFunctionDescriptor() }
}
