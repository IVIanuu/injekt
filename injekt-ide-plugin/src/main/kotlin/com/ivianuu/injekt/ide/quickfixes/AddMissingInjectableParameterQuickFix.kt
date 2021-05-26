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

      val parentFunction = diagnostic.psiElement.getParentOfType<KtNamedFunction>(false)
      if (parentFunction != null) {
        if (parentFunction.hasModifier(KtTokens.OVERRIDE_KEYWORD))
          return emptyList()
        return listOf(addInjectableParameterQuickFix(parentFunction, unwrappedFailureRequest.type))
      }

      val parentClass = diagnostic.psiElement.getParentOfType<KtClass>(false)
      if (parentClass != null) {
        return listOf(addInjectableConstructorParameterQuickFix(
          parentClass, unwrappedFailureRequest.type, diagnostic.psiElement.cast()))
      }

      return emptyList()
    }
  }
)

private fun addInjectableConstructorParameterQuickFix(
  clazz: KtClass,
  type: TypeRef,
  call: KtElement
) = object : BaseIntentionAction() {
  override fun getFamilyName(): String =
    "Add injectable constructor parameter for ${type.renderKotlinLikeToString()}"

  override fun getText(): String =
    "Add injectable constructor parameter for ${type.renderKotlinLikeToString()}"

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    (file as KtFile).addImportIfNeeded(InjektFqNames.Inject)
    val primaryConstructor = clazz.createPrimaryConstructorIfAbsent()
    val injectText = if (primaryConstructor.hasAnnotation(InjektFqNames.Provide) ||
        clazz.hasAnnotation(InjektFqNames.Provide)) "" else "@Inject "
    val valText = if (call.getParentOfType<KtNamedFunction>(false) == null) "" else "val "
    primaryConstructor.valueParameterList!!.addParameter(
      KtPsiFactory(project)
        .createParameter("${injectText}${valText}_: ${type.renderKotlinLikeToString()}")
    )
  }

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
}

private fun addInjectableParameterQuickFix(
  function: KtNamedFunction,
  type: TypeRef
) = object : BaseIntentionAction() {
  override fun getFamilyName(): String =
    "Add injectable parameter for ${type.renderKotlinLikeToString()}"

  override fun getText(): String =
    "Add injectable parameter for ${type.renderKotlinLikeToString()}"

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    (file as KtFile).addImportIfNeeded(InjektFqNames.Inject)
    val injectText = if (function.hasAnnotation(InjektFqNames.Provide)) "" else "@Inject "
    function.valueParameterList!!.addParameter(
      KtPsiFactory(project)
        .createParameter("${injectText}_: ${type.renderKotlinLikeToString()}")
    )
  }

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
}
F