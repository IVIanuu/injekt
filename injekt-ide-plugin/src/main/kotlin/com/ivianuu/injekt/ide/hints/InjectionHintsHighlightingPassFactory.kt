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
