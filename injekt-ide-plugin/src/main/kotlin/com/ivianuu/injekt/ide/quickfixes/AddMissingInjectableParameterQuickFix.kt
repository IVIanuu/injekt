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

package com.ivianuu.injekt.ide.quickfixes

import com.intellij.codeInsight.intention.*
import com.intellij.codeInsight.intention.impl.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.idea.quickfix.*
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import org.jetbrains.kotlin.lexer.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun QuickFixes.addMissingInjectableAsParameter() = register(
  InjektErrors.UNRESOLVED_INJECTION,
  object : KotlinIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
      diagnostic as DiagnosticWithParameters1<*, *>
      val graph = diagnostic.a as InjectionGraph.Error
      val (unwrappedFailureRequest, unwrappedFailure) =
        graph.failure.unwrapDependencyFailure(graph.failureRequest)
      if (unwrappedFailure !is ResolutionResult.Failure.NoCandidates) return emptyList()

      val target = diagnostic.psiElement.parents
        .firstOrNull {
          (it is KtNamedFunction && !it.hasModifier(KtTokens.OVERRIDE_KEYWORD)) ||
              it is KtClass
        }

      return when (target) {
        is KtNamedFunction, is KtClass -> listOf(
          addInjectableParameterQuickFix(
            target.cast(),
            unwrappedFailureRequest.type,
            diagnostic.psiElement.cast(),
            graph.scope.context
          )
        )
        else -> emptyList()
      }
    }
  }
)

private fun addInjectableParameterQuickFix(
  target: KtDeclaration,
  type: TypeRef,
  call: KtElement,
  context: InjektContext
) = object : BaseIntentionAction() {
  override fun getFamilyName(): String = ""

  override fun getText(): String =
    "Add injectable parameter for ${type.renderKotlinLikeToString()}"

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    val function = when (target) {
      is KtClass -> target.createPrimaryConstructorIfAbsent()
      is KtFunction -> target
      else -> throw AssertionError()
    }
    val injectText = if (function.hasAnnotation(InjektFqNames.Provide) ||
        target.hasAnnotation(InjektFqNames.Provide)) "" else "@Inject "
    if (injectText.isNotEmpty()) {
      file.cast<KtFile>().addImport(InjektFqNames.Inject, context)
    }
    val valText = if (target is KtClass &&
      (call.getParentOfType<KtNamedFunction>(false).let {
        it != null && it.getParentOfType<KtClassOrObject>(false) == target
      } || call.getParentOfType<KtPropertyAccessor>(false)?.property.let {
        it != null && it.getParentOfType<KtClassOrObject>(false) == target
      })
    ) "private val " else ""
    function.valueParameterList!!.addParameter(
      KtPsiFactory(project)
        .createParameter(
          "${injectText}${valText}" +
              type.classifier.fqName.shortName().asString().decapitalize() +
              ": ${type.renderKotlinLikeToString()}"
        )
    )
  }

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
}
