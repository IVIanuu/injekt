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

    container.useInstance(TypeFixer())
    container.useInstance(InjectableChecker())
    container.useInstance(TagChecker())
    container.useInstance(ProviderImportsChecker())
    if (!isIde)
      container.useInstance(InfoPatcher())
  }
}
