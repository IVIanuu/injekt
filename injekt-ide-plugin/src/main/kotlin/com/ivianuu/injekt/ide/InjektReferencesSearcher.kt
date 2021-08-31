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

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.expressionRecursiveVisitor

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
