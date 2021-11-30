/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.isIde
import com.ivianuu.injekt.compiler.readPrivateFinalField
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.container.ComponentStorage
import org.jetbrains.kotlin.container.SingletonTypeComponentDescriptor
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.synthetic.JavaSyntheticScopes

class InjektStorageComponentContainerContributor(
  private val injektFqNames: (ModuleDescriptor) -> InjektFqNames
) : StorageComponentContainerContributor {
  override fun registerModuleComponents(
    container: StorageComponentContainer,
    platform: TargetPlatform,
    moduleDescriptor: ModuleDescriptor,
  ) {
    @Provide val ctx = Context(moduleDescriptor, injektFqNames(moduleDescriptor), null)

    val hasSyntheticScopesExtension = container.readPrivateFinalField<ComponentStorage>(
      StorageComponentContainer::class,
      "componentStorage"
    )
      .readPrivateFinalField<Set<Any>>(
        ComponentStorage::class,
        "descriptors"
      )
      .let { descriptors ->
        descriptors.any {
          it is SingletonTypeComponentDescriptor &&
              it.klass == JavaSyntheticScopes::class.java
        }
      }

    if (!hasSyntheticScopesExtension) {
      container.useInstance(ctx)
      container.useImpl<InjectSyntheticScopes>()
    }

    container.useInstance(InjectableChecker())
    container.useInstance(TagChecker())
    container.useInstance(ProviderImportsChecker())
    if (!isIde)
      container.useInstance(InfoPatcher())
  }
}
