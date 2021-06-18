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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.platform.*

class InjektStorageComponentContainerContributor(
  val isEnabled: (ModuleDescriptor) -> Boolean = { true }
) : StorageComponentContainerContributor {
  override fun registerModuleComponents(
    container: StorageComponentContainer,
    platform: TargetPlatform,
    moduleDescriptor: ModuleDescriptor,
  ) {
    if (!isEnabled(moduleDescriptor)) return

    val context = moduleDescriptor.injektContext
    if (platform.componentPlatforms.size > 1)
      container.useImpl<InjectSyntheticScopes>()
    container.useInstance(InjectableChecker(context))
    container.useInstance(TagChecker())
    container.useInstance(ProviderImportsChecker(context))
    container.useInstance(InjectionCallChecker(context))
    container.useInstance(InfoAnnotationPatcher(context))
  }
}
