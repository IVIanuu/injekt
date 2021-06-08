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

package com.ivianuu.injekt.ide

import com.intellij.ide.*
import com.intellij.openapi.application.*
import com.intellij.openapi.extensions.*
import com.intellij.openapi.project.*
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.ide.quickfixes.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.idea.quickfix.*
import org.jetbrains.kotlin.resolve.diagnostics.*
import org.jetbrains.kotlin.synthetic.*

@Suppress("UnstableApiUsage")
class AppInitializer : ApplicationInitializedListener {
  override fun componentsInitialized() {
    val app = ApplicationManager.getApplication()
    app
      .messageBus.connect(app)
      .subscribe(
        ProjectManager.TOPIC,
        object : ProjectManagerListener {
          override fun projectOpened(project: Project) {
            StorageComponentContainerContributor.registerExtension(
              project,
              InjektStorageComponentContainerContributor { it.isInjektEnabled() }
            )
            SyntheticScopeProviderExtension.registerExtension(
              project,
              InjectSyntheticScopeProviderExtension { it.isInjektEnabled() }
            )
            @Suppress("DEPRECATION")
            Extensions.getRootArea().getExtensionPoint(QuickFixContributor.EP_NAME)
              .registerExtension(InjektQuickFixContributor())
            @Suppress("DEPRECATION")
            Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
              .registerExtension(InjektDiagnosticSuppressor())
          }
        }
      )
  }
}
