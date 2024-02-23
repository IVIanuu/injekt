/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.frontend

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.platform.*

class InjektStorageComponentContainerContributor : StorageComponentContainerContributor {
  override fun registerModuleComponents(
    container: StorageComponentContainer,
    platform: TargetPlatform,
    moduleDescriptor: ModuleDescriptor,
  ) {
    val ctx = Context(moduleDescriptor, null)
    container.useInstance(InjektDeclarationChecker(ctx))
    container.useInstance(InfoPatcher(ctx))
  }
}
