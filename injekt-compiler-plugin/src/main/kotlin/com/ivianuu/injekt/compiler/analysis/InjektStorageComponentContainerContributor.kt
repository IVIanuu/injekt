/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.platform.*
import org.jetbrains.kotlin.synthetic.*

class InjektStorageComponentContainerContributor(
  private val injektFqNames: (ModuleDescriptor) -> InjektFqNames
) : StorageComponentContainerContributor {
  override fun registerModuleComponents(
    container: StorageComponentContainer,
    platform: TargetPlatform,
    moduleDescriptor: ModuleDescriptor,
  ) {
    val ctx = Context(moduleDescriptor, injektFqNames(moduleDescriptor), null)

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

    container.useInstance(InjectableChecker(ctx))
    container.useInstance(TagChecker(ctx))
    container.useInstance(ProviderImportsChecker(ctx))
    if (!isIde)
      container.useInstance(InfoPatcher(ctx))
  }
}
