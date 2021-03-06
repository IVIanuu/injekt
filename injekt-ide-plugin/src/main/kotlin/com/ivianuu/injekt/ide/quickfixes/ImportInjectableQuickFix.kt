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
import com.intellij.find.findUsages.*
import com.intellij.ide.util.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.ui.popup.util.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.psi.search.searches.*
import com.intellij.psi.util.*
import com.intellij.psi.util.parents
import com.intellij.ui.popup.list.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.InjektErrors.Companion.UNRESOLVED_INJECTION
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.findAnnotation
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.idea.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.findUsages.*
import org.jetbrains.kotlin.idea.quickfix.*
import org.jetbrains.kotlin.idea.search.ideaExtensions.*
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.idea.util.application.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.lexer.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.awt.*
import javax.swing.*

fun org.jetbrains.kotlin.idea.quickfix.QuickFixes.importInjectable() = register(
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
  override fun getFamilyName(): String = ""

  override fun getText(): String = "Import injectable for ${type.renderKotlinLikeToString()}"

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    val candidates = scope.context.injectablesForType(type,
      scope.allStaticTypeParameters, project, file.resolveScope)
    if (candidates.size == 1) {
      addInjectableImport(call, project, candidates.single(), scope.context)
      return
    }
    ListPopupImpl(
      object : BaseListPopupStep<CallableRef>(
        "Pick injectable to import for ${type.renderKotlinLikeToString()}", candidates) {
        override fun isAutoSelectionEnabled() = false

        override fun isSpeedSearchEnabled() = true

        override fun onChosen(selectedValue: CallableRef?, finalChoice: Boolean): PopupStep<String>? {
          if (selectedValue == null || !finalChoice) return null
          addInjectableImport(call, project, selectedValue, scope.context)
          return null
        }

        override fun hasSubstep(selectedValue: CallableRef?) = false
        override fun getTextFor(value: CallableRef) = value.callable.fqNameSafe.asString()
        override fun getIconFor(value: CallableRef) =
          KotlinDescriptorIconProvider.getIcon(value.callable, null, 0)
      }
    ).showInBestPositionFor(editor)
  }

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
}

private fun addInjectableImport(
  call: KtElement,
  project: Project,
  injectable: CallableRef,
  context: AnalysisContext
) {
  val existingProvidersAnnotation = call.parents
    .toList()
    .firstNotNullResult {
      it.safeAs<KtAnnotated>()?.findAnnotation(InjektFqNames.Providers)
    }

  val newImportPath = "\"${injectable.callable.fqNameSafe}\""

  project.executeWriteCommand("Add injectable import") {
    val psiFactory = KtPsiFactory(project)

    if (existingProvidersAnnotation != null) {
      existingProvidersAnnotation
        .valueArgumentList
        ?.addArgument(psiFactory.createArgument(newImportPath))
        ?: psiFactory.createCallArguments("($newImportPath)")
        .also {
          existingProvidersAnnotation
            .addAfter(it, existingProvidersAnnotation.typeReference!!)
        }
    } else {
      val file = call.containingKtFile
      file.addImport(InjektFqNames.Provide, context)

      val annotationText = "Providers($newImportPath)"

      val fileAnnotationList: KtFileAnnotationList? = file.fileAnnotationList
      if (fileAnnotationList == null) {
        val newAnnotationList = psiFactory.createFileAnnotationListWithAnnotation(annotationText)
        val createdAnnotationList = replaceFileAnnotationList(file, newAnnotationList)
        file.addAfter(psiFactory.createWhiteSpace(), createdAnnotationList)
      } else {
        fileAnnotationList.add(psiFactory.createFileAnnotation(annotationText))
      }
    }
  }
}

private fun AnalysisContext.injectablesForType(
  type: TypeRef,
  staticTypeParameters: List<ClassifierRef>,
  project: Project,
  scope: GlobalSearchScope
): List<CallableRef> = getAllInjectables(project, scope)
  .filter { candidate ->
    val context = candidate.buildContext(staticTypeParameters, type, context = this)
    context.isOk
  }

fun AnalysisContext.getAllInjectables(project: Project, useScope: GlobalSearchScope): List<CallableRef> {
  val provideAnnotation = JavaPsiFacade.getInstance(project)
    .findClass(InjektFqNames.Provide.asString(), useScope) as KtLightClass

  val injectables = mutableListOf<CallableRef>()
  val scope = KotlinSourceFilterScope.sourcesAndLibraries(useScope, project)
  KotlinAnnotationsIndex.getInstance().get(provideAnnotation.name!!, project, scope)
    .forEach { annotation ->
      val annotatedDeclaration = annotation.getParentOfType<KtNamedDeclaration>(false)
      if (annotatedDeclaration !is KtClassOrObject &&
        annotatedDeclaration !is KtNamedFunction &&
        annotatedDeclaration !is KtProperty) return@forEach
      if (annotatedDeclaration is KtNamedFunction ||
        annotatedDeclaration is KtProperty) {
        val parentClass = annotatedDeclaration.getParentOfType<KtClassOrObject>(false)
        if (parentClass != null && parentClass !is KtObjectDeclaration)
          return@forEach
      }
      annotatedDeclaration.resolveToDescriptorIfAny()
        ?.let { descriptor ->
          when (descriptor) {
            is ClassDescriptor -> injectables += descriptor.injectableConstructors(this)
            is FunctionDescriptor -> injectables += descriptor.toCallableRef(this)
            is PropertyDescriptor -> injectables += descriptor.toCallableRef(this)
          }
        }
    }

  return injectables
    .filter {
      it.callable.containingDeclaration !is ClassDescriptor ||
          it.callable.containingDeclaration.cast<ClassDescriptor>().kind == ClassKind.OBJECT
    }
    .filterNot { it.typeParameters.any { it.isSpread } }
    .distinctBy { it.callable.fqNameSafe }
}
