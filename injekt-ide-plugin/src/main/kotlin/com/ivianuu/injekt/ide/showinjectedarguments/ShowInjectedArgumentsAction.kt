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

package com.ivianuu.injekt.ide.showinjectedarguments

import com.intellij.icons.*
import com.intellij.ide.projectView.*
import com.intellij.ide.util.treeView.*
import com.intellij.openapi.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.util.*
import com.intellij.pom.Navigatable
import com.intellij.psi.util.*
import com.intellij.ui.*
import com.intellij.ui.tree.*
import com.intellij.ui.treeStructure.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.ide.refs.*
import org.jetbrains.kotlin.idea.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.tree.*

class ShowInjectedArgumentsAction : AnAction("Show injected arguments") {
  override fun actionPerformed(event: AnActionEvent) {
    val context = event.dataContext
    val editor = CommonDataKeys.EDITOR.getData(context) ?: return
    val project = CommonDataKeys.PROJECT.getData(context) ?: return

    val file = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return

    val selectedElement = file.findElementAt(editor.caretModel.offset)

    val call = selectedElement
      ?.getParentOfTypes3<KtCallExpression, KtBinaryExpression, KtSuperTypeCallEntry>()
      ?.cast<KtElement>()
      ?: return

    showInjectedArgumentsPopup(call, project, editor)
  }
}

fun showInjectedArgumentsPopup(
  call: KtElement,
  project: Project,
  editor: Editor,
  initialSelectionPredicate: ((InjectableRequest, ResolutionResult) -> Boolean)? = null
) {
  val bindingContext = call.getResolutionFacade().analyze(call)

  val graph = bindingContext[InjektWritableSlices.INJECTION_GRAPH_FOR_CALL, call]
    ?: return

  val results = when (graph) {
    is InjectionGraph.Success -> graph.results
    is InjectionGraph.Error -> mapOf(graph.failureRequest to graph.failure)
  }

  val jTree = Tree()
  val structure = InjectedArgumentsTreeStructure(project, graph.callee, results)

  val tmpDisposable = Disposable {
    jTree
  }
  val structureTreeModel = StructureTreeModel(structure, tmpDisposable)
  val asyncTreeModel = AsyncTreeModel(structureTreeModel, true, tmpDisposable)

  jTree.model = asyncTreeModel
  jTree.isRootVisible = true

  /*results
    ?.invoke(graph)
    ?.let { structureTreeModel.select(it) }*/

  val minSize = jTree.preferredSize

  val scrollPane = ScrollPaneFactory.createScrollPane(jTree, true)

  val panel = JPanel(BorderLayout())

  panel.add(scrollPane, BorderLayout.CENTER)

  val title = "Injected arguments"

  val popup = JBPopupFactory.getInstance()
    .createComponentPopupBuilder(panel, jTree)
    .setRequestFocus(true)
    .setResizable(true)
    .setMovable(true)
    .setTitle(title)
    .setMinSize(Dimension(minSize.width + 700, minSize.height))
    .createPopup()

  fun navigateToSelectedElement() {
    val selectedValue = jTree.lastSelectedPathComponent
      .safeAs<DefaultMutableTreeNode>()
      ?.userObject
      ?.safeAs<AbstractTreeNode<*>>()
      ?.value

    fun navigateToInjectable(injectable: Injectable) {
      val psiDeclaration = injectable
        .safeAs<CallableInjectable>()
        ?.callable
        ?.callable
        ?.findPsiDeclarations(project, call.resolveScope)
        ?.firstOrNull()
        ?: return
      (psiDeclaration.navigationElement as? Navigatable)
        ?.navigate(true)
      popup.cancel()
    }

    when (selectedValue) {
      is InjectableRequest -> {
        val psiDeclaration = selectedValue.parameterDescriptor
          ?.findPsiDeclarations(project, call.resolveScope)
          ?.firstOrNull()
          ?: return
        (psiDeclaration.navigationElement as? Navigatable)
          ?.navigate(true)
        popup.cancel()
      }
      is CallableRef -> {
        val psiDeclaration = selectedValue
          .callable
          .findPsiDeclarations(project, call.resolveScope)
          .firstOrNull()
          ?: return
        (psiDeclaration.navigationElement as? Navigatable)
          ?.navigate(true)
        popup.cancel()
      }
      ResolutionResult.Success.DefaultValue -> TODO()
      is ResolutionResult.Success.WithCandidate ->
        navigateToInjectable(selectedValue.candidate)
      is ResolutionResult.Failure.WithCandidate ->
        navigateToInjectable(selectedValue.candidate)
      is Injectable ->
        navigateToInjectable(selectedValue)
    }
  }

  object : ClickListener() {
    override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
      if (clickCount != 2) return false
      navigateToSelectedElement()
      return true
    }
  }.installOn(jTree)

  val enterShortcuts = CustomShortcutSet.fromString("ENTER")

  object : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
      navigateToSelectedElement()
    }
  }.registerCustomShortcutSet(enterShortcuts, panel)

  Disposer.register(popup, tmpDisposable)

  popup.showInBestPositionFor(editor)
}

class InjectedArgumentsTreeStructure(
  val project: Project,
  val callee: CallableRef,
  val results: Map<InjectableRequest, ResolutionResult>
): AbstractTreeStructure() {
  override fun getRootElement(): Any = RootNode()

  override fun getChildElements(value: Any): Array<Any> =
    value.cast<AbstractTreeNode<*>>().children.toTypedArray()

  override fun createDescriptor(value: Any, descriptor: NodeDescriptor<*>?): NodeDescriptor<*> =
    value.cast()

  override fun getParentElement(p0: Any): Any? = null

  override fun hasSomethingToCommit(): Boolean = false

  override fun commit() {
  }

  private inner class RootNode : AbstractTreeNode<Any>(project, callee) {
    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> =
      results.mapTo(mutableListOf()) { it.key.toRequestNode(project!!, it.value) }

    override fun update(data: PresentationData) {
      data.presentableText = callee.callable.name.asString()
      data.setIcon(KotlinDescriptorIconProvider.getIcon(callee.callable, null, 0))
    }
  }

  class DefaultRequestNode(
    project: Project,
    request: InjectableRequest,
    private val result: ResolutionResult
  ) : AbstractTreeNode<InjectableRequest>(project, request) {
    override fun getChildren() = listOf(result.toResultNode(project!!, value))

    override fun update(data: PresentationData) {
      data.presentableText = "${value.parameterName}: ${value.type.renderKotlinLikeToString()}"
      value.parameterDescriptor
        ?.let {
          data.setIcon(KotlinDescriptorIconProvider.getIcon(it, null, 0))
        }
    }
  }

  class SingleFailureRequestNode(
    project: Project,
    request: InjectableRequest,
    private val result: ResolutionResult.Failure.WithCandidate
  ) : AbstractTreeNode<InjectableRequest>(project, request) {
    override fun getChildren() =
      mutableListOf(ErrorMessageNode(project!!, result.message(), result.candidate))

    override fun update(data: PresentationData) {
      data.presentableText = "${value.parameterName}: ${value.type.renderKotlinLikeToString()}"
      value.parameterDescriptor
        ?.let {
          data.setIcon(KotlinDescriptorIconProvider.getIcon(it, null, 0))
        }
    }
  }

  class ErrorMessageNode(
    project: Project,
    message: String,
    private val injectable: Injectable
  ) : AbstractTreeNode<String>(project, message) {
    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> =
      mutableListOf(SingleFailureCandidateNode(project!!, injectable))

    override fun update(data: PresentationData) {
      data.presentableText = value
      data.setIcon(AllIcons.General.Error)
    }
  }

  class SingleFailureCandidateNode(
    project: Project,
    injectable: Injectable
  ) : AbstractTreeNode<Injectable>(project, injectable) {
    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> =
      mutableListOf()

    override fun update(data: PresentationData) {
      data.renderInjectable(value)
    }
  }

  class AmbiguousResultNode(
    project: Project,
    failure: ResolutionResult.Failure.CandidateAmbiguity
  ) : AbstractTreeNode<ResolutionResult.Failure.CandidateAmbiguity>(project, failure) {
    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> =
      value.candidateResults.mapTo(mutableListOf()) { it.toResultNode(project!!, value.request) }

    override fun update(data: PresentationData) {
      data.presentableText = "Ambiguous candidates"
      data.setIcon(AllIcons.General.Error)
    }
  }

  class NoCandidatesResultNode(
    project: Project
  ) : AbstractTreeNode<ResolutionResult.Failure.NoCandidates>(
    project,
    ResolutionResult.Failure.NoCandidates
  ) {
    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> = mutableListOf()

    override fun update(data: PresentationData) {
      data.presentableText = "No candidates found"
      data.setIcon(AllIcons.General.Error)
    }
  }

  class SuccessResultNode(
    project: Project,
    value: ResolutionResult.Success
  ) : AbstractTreeNode<ResolutionResult>(project, value) {
    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> =
      when(val value = value) {
        is ResolutionResult.Success.WithCandidate.Value -> value.dependencyResults
          .mapTo(mutableListOf()) { it.key.toRequestNode(project!!, it.value) }
        is ResolutionResult.Failure.CandidateAmbiguity ->
          mutableListOf(AmbiguousResultNode(project!!, value))
        is ResolutionResult.Failure.NoCandidates ->
          mutableListOf(NoCandidatesResultNode(project!!))
        else -> mutableListOf()
      }

    override fun update(data: PresentationData) {
      when (val value = value) {
        ResolutionResult.Success.DefaultValue -> {
          data.presentableText = "Default value"
        }
        is ResolutionResult.Success.WithCandidate ->
          data.renderInjectable(value.candidate)
        else -> throw AssertionError()
      }
    }
  }
}

private fun ResolutionResult.Failure.message(): String = when (this) {
  is ResolutionResult.Failure.CandidateAmbiguity -> throw AssertionError()
  ResolutionResult.Failure.NoCandidates -> throw AssertionError()
  is ResolutionResult.Failure.WithCandidate.CallContextMismatch ->
    "Call context mismatch: expected ${candidate.callContext} but was $actualCallContext"
  is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
  is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> "divergent injectable"
  is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch ->
    "Reified mismatch"
}

private fun PresentationData.renderInjectable(injectable: Injectable) {
  presentableText = injectable.callableFqName.asString()
  injectable
    .safeAs<CallableInjectable>()
    ?.callable
    ?.callable
    ?.let { setIcon(KotlinDescriptorIconProvider.getIcon(it, null, 0)) }
}

private fun InjectableRequest.toRequestNode(
  project: Project,
  result: ResolutionResult
) = when (result) {
  is ResolutionResult.Failure.WithCandidate ->
    InjectedArgumentsTreeStructure.SingleFailureRequestNode(project, this, result)
  else -> InjectedArgumentsTreeStructure.DefaultRequestNode(project, this, result)
}

private fun ResolutionResult.toResultNode(
  project: Project,
  request: InjectableRequest
) = when (this) {
  is ResolutionResult.Failure.CandidateAmbiguity ->
    InjectedArgumentsTreeStructure.AmbiguousResultNode(project, this)
  is ResolutionResult.Failure.NoCandidates ->
    InjectedArgumentsTreeStructure.NoCandidatesResultNode(project)
  is ResolutionResult.Failure.DependencyFailure ->
    dependencyRequest.toRequestNode(project, dependencyFailure)
  is ResolutionResult.Failure.WithCandidate ->
    InjectedArgumentsTreeStructure.SingleFailureRequestNode(
      project,
      request,
      this
    )
  is ResolutionResult.Success ->
    InjectedArgumentsTreeStructure.SuccessResultNode(project, this)
  else -> throw AssertionError("Unexpected result $this")
}
