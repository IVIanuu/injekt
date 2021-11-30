/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.ide

import com.intellij.openapi.application.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.psi.search.searches.*
import com.intellij.util.*
import org.jetbrains.kotlin.idea.*
import org.jetbrains.kotlin.psi.*

class InjektReferencesSearcher :
  QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
  override fun processQuery(
    params: ReferencesSearch.SearchParameters,
    processor: Processor<in PsiReference>
  ) {
    if (!params.elementToSearch.isInjektEnabled()) return

    val tasks = mutableListOf<() -> Unit>()
    runReadAction {
      val ktElement = params.elementToSearch.ktElementOrNull() ?: return@runReadAction

      val isProvideOrInjectDeclaration = ktElement.isProvideOrInjectDeclaration()
      val isObjectDeclaration = ktElement is KtObjectDeclaration

      if (!isProvideOrInjectDeclaration && !isObjectDeclaration)
        return@runReadAction

      val psiManager = PsiManager.getInstance(params.project)

      fun collectTasks(scope: SearchScope) {
        if (scope is LocalSearchScope) {
          for (element in scope.scope) {
            element.accept(
              expressionRecursiveVisitor { expression ->
                if (expression is KtStringTemplateExpression &&
                  expression.isProviderImport()) {
                  tasks += {
                    expression.references
                      .filterIsInstance<ImportElementReference>()
                      .filter { it.isReferenceTo(ktElement) }
                      .forEach { processor.process(it) }
                  }
                }
              }
            )
          }
        } else if (scope is GlobalSearchScope) {
          for (file in FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)) {
            val psiFile = psiManager.findFile(file) as? KtFile
            if (psiFile != null)
              collectTasks(LocalSearchScope(psiFile))
          }
        }
      }

      collectTasks(params.effectiveSearchScope)

      for (task in tasks)
        runReadAction { task() }
    }
  }
}
