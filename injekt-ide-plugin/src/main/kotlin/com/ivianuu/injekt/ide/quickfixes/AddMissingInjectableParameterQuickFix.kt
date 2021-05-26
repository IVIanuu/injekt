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

fun QuickFixes.addMissingInjectableAsParameter() = register(
  InjektErrors.UNRESOLVED_INJECTION,
  object : KotlinIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
      diagnostic as DiagnosticWithParameters1<*, *>
      val graph = diagnostic.a as InjectionGraph.Error
      val (unwrappedFailureRequest, unwrappedFailure) =
        graph.failure.unwrapDependencyFailure(graph.failureRequest)
      if (unwrappedFailure !is ResolutionResult.Failure.NoCandidates) return emptyList()
      val parentFunction = diagnostic.psiElement.getParentOfType<KtFunction>(false)
        ?: return emptyList()
      if (parentFunction.hasModifier(KtTokens.OVERRIDE_KEYWORD))
        return emptyList()
      return listOf(addInjectableParameterQuickFix(parentFunction, unwrappedFailureRequest.type))
    }
  }
)

private fun addInjectableParameterQuickFix(
  function: KtFunction,
  type: TypeRef
) = object : BaseIntentionAction() {
  override fun getFamilyName(): String = "Add injectable parameter for $type"

  override fun getText(): String = "Add injectable parameter for $type"

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    (file as KtFile).addImportIfNeeded(InjektFqNames.Inject)
    function.valueParameterList!!.addParameter(
      KtPsiFactory(project)
        .createParameter("@Inject _: ${type.renderToString()}")
    )
  }

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
}
