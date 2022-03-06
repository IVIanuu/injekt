/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.ide

import com.intellij.openapi.project.*
import com.ivianuu.injekt.compiler.analysis.*
import org.jetbrains.kotlin.extensions.*

class InjektProjectInitializer : ProjectManagerListener {
  override fun projectOpened(project: Project) {
    StorageComponentContainerContributor.registerExtension(
      project,
      InjektStorageComponentContainerContributor()
    )
  }
}
