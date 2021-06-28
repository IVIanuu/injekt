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

package com.ivianuu.injekt.ide.refs

import com.intellij.openapi.application.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.psi.search.searches.*
import com.intellij.util.*
import com.ivianuu.injekt.ide.*
import com.siyeh.ig.psiutils.ConstructionUtils.*
import org.jetbrains.kotlin.idea.*
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.psi.*

class InjektReferencesSearcher :
  QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
  override fun processQuery(
    params: ReferencesSearch.SearchParameters,
    processor: Processor<in PsiReference>
  ) {
    if (!params.elementToSearch.isInjektEnabled()) return

    params.project.runReadActionInSmartMode {
      val ktElement = params.elementToSearch.ktElementOrNull() ?: return@runReadActionInSmartMode

      val isProvideOrInjectDeclaration = ktElement.isProvideOrInjectDeclaration()
      val isObjectDeclaration = ktElement is KtObjectDeclaration

      if (!isProvideOrInjectDeclaration && !isObjectDeclaration)
        return@runReadActionInSmartMode

      val psiManager = PsiManager.getInstance(params.project)

      fun search(scope: SearchScope) {
        if (scope is LocalSearchScope) {
          for (element in scope.scope) {
            element.accept(
              expressionRecursiveVisitor { expression ->
                when {
                  expression is KtStringTemplateExpression &&
                  expression.isProviderImport() -> {
                    expression.references
                      .filterIsInstance<ImportElementReference>()
                      .filter { it.isReferenceTo(ktElement) }
                      .forEach { processor.process(it) }
                  }
                  isProvideOrInjectDeclaration && expression is KtCallExpression -> {
                    expression.references
                      .filterIsInstance<InjectReference>()
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
              search(LocalSearchScope(psiFile))
          }
        }
      }

      search(params.effectiveSearchScope)
    }
  }
}
