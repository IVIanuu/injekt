/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.isIde
import com.ivianuu.injekt.compiler.readPrivateFinalField
import org.jetbrains.kotlin.container.ComponentStorage
import org.jetbrains.kotlin.container.SingletonTypeComponentDescriptor
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.synthetic.JavaSyntheticScopes

class InjektStorageComponentContainerContributor : StorageComponentContainerContributor {
  override fun registerModuleComponents(
    container: StorageComponentContainer,
    platform: TargetPlatform,
    moduleDescriptor: ModuleDescriptor,
  ) {
    val ctx = Context(moduleDescriptor, null)

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
      container.useImpl<ContextSyntheticScopes>()
    }

    container.useInstance(ProviderChecker(ctx))
    container.useInstance(TagChecker(ctx))
    container.useInstance(ProviderImportsChecker(ctx))
    if (!isIde)
      container.useInstance(InfoPatcher(ctx))
  }
}
