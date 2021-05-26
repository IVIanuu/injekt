package com.ivianuu.injekt.ide.quickfixes

import com.intellij.codeInsight.intention.*
import com.intellij.codeInsight.intention.impl.*
import com.intellij.ide.util.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.ui.popup.util.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.ui.popup.list.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.InjektErrors.Companion.UNRESOLVED_INJECTION
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.idea.quickfix.*
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import org.jetbrains.kotlin.idea.util.application.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.awt.*
import javax.swing.*

fun QuickFixes.importInjectable() = register(
  UNRESOLVED_INJECTION,
  object : KotlinIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
      diagnostic as DiagnosticWithParameters1<*, *>
      val graph = diagnostic.a as InjectionGraph.Error
      val (unwrappedFailureRequest, unwrappedFailure) =
        graph.failure.unwrapDependencyFailure(graph.failureRequest)
      if (unwrappedFailure !is ResolutionResult.Failure.NoCandidates) return emptyList()
      return listOf(importInjectableQuickFix(
        diagnostic.psiElement as KtElement, unwrappedFailureRequest.type, graph.scope))
    }
  }
)

private fun importInjectableQuickFix(
  call: KtElement,
  type: TypeRef,
  scope: InjectablesScope
) = object : BaseIntentionAction() {
  override fun getFamilyName(): String = "Import injectable for ${type.renderKotlinLikeToString()}"

  override fun getText(): String = "Import injectable for ${type.renderKotlinLikeToString()}"

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    val candidates = scope.context.injectablesForType(type, scope.allStaticTypeParameters)
    if (candidates.size == 1) {
      addInjectableImport(call, project, candidates.single())
      return
    }
    object : ListPopupImpl(
      object : BaseListPopupStep<CallableRef>(
        "Pick injectable to import for ${type.renderKotlinLikeToString()}", candidates) {
        override fun isAutoSelectionEnabled() = false

        override fun isSpeedSearchEnabled() = true

        override fun onChosen(selectedValue: CallableRef?, finalChoice: Boolean): PopupStep<String>? {
          if (selectedValue == null || !finalChoice) return null
          addInjectableImport(call, project, selectedValue)
          return null
        }

        override fun hasSubstep(selectedValue: CallableRef?) = false
        override fun getTextFor(value: CallableRef) = value.callable.fqNameSafe.asString()
        override fun getIconFor(value: CallableRef) = null
      }
    ) {
      override fun getListElementRenderer(): ListCellRenderer<CallableRef> {
        val baseRenderer = super.getListElementRenderer() as PopupListElementRenderer
        val psiRenderer = DefaultPsiElementCellRenderer()
        return ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
          JPanel(BorderLayout()).apply {
            baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            add(baseRenderer.nextStepLabel, BorderLayout.EAST)
            add(
              psiRenderer.getListCellRendererComponent(
                list,
                value.callable.fqNameSafe.asString(),
                index,
                isSelected,
                cellHasFocus
              )
            )
          }
        }
      }
    }.showInBestPositionFor(editor)
  }

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
}

private fun addInjectableImport(
  call: KtElement,
  project: Project,
  injectable: CallableRef
) {
  val importTarget = call.parents
    .firstOrNull { parent ->
      parent is KtClassOrObject ||
          parent is KtFunction ||
          parent is KtProperty
    }.safeAs<KtModifierListOwner>() ?: return

  val existingImports = call.parents
    .toList()
    .firstNotNullResult {
      it.safeAs<KtAnnotated>()?.findAnnotation(InjektFqNames.Providers)
    }
    ?.safeAs<KtAnnotationEntry>()

  val newImportPaths = (listOf(
    "\"${injectable.callable.fqNameSafe}\""
  ) + (existingImports
    ?.valueArgumentList
    ?.arguments
    ?.mapNotNull { it.getArgumentExpression() }
    ?.map { it.text!! } ?: emptyList()))
    .sorted()

  project.executeWriteCommand("Add injectable import") {
    call.containingKtFile.addImportIfNeeded(InjektFqNames.Providers)
    existingImports?.delete()
    importTarget.addAnnotationEntry(
      KtPsiFactory(project)
        .createAnnotationEntry("@Providers(${newImportPaths.joinToString(",\n")})")
    )
  }
}

private fun InjektContext.injectablesForType(
  type: TypeRef,
  staticTypeParameters: List<ClassifierRef>
): List<CallableRef> = getAllInjectables()
  .filter { candidate ->
    val context = candidate.type.buildContext(this, staticTypeParameters, type)
    context.isOk
  }

private fun InjektContext.getAllInjectables(): List<CallableRef> {
  val injectables = mutableListOf<CallableRef>()
  fun collect(fqName: FqName) {
    val packageView = module.getPackage(fqName)
    injectables += packageView.memberScope
      .collectInjectables(this, trace, false)
    module.getSubPackagesOf(fqName) { true }
      .forEach { collect(it) }
  }
  collect(FqName.ROOT)
  return injectables
}
