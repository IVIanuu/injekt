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

package com.ivianuu.injekt.ide.hints

import com.intellij.codeHighlighting.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.*

class InjectionHintsTextEditorHighlightingPassFactory : TextEditorHighlightingPassFactory,
  TextEditorHighlightingPassFactoryRegistrar {
  override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? =
    if (file is KtFile) InjectionHintsHighlightingPass(file, editor) else null

  override fun registerHighlightingPassFactory(
    registrar: TextEditorHighlightingPassRegistrar,
    project: Project
  ) {
    val runAfterAnnotator = intArrayOf(Pass.UPDATE_ALL)
    registrar.registerTextEditorHighlightingPass(this, runAfterAnnotator, null, false, -1)
  }
}
