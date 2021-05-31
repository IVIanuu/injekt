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

import com.intellij.ide.projectView.*
import com.intellij.ide.util.treeView.*
import com.intellij.openapi.*
import com.intellij.openapi.actionSystem.*
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
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.tree.*

class ShowInjectedArgumentsAction : AnAction(
  "Show injected arguments"
) {
  override fun actionPerformed(event: AnActionEvent) {
    val context = event.dataContext
    val editor = CommonDataKeys.EDITOR.getData(context) ?: return
    val project = CommonDataKeys.PROJECT.getData(context) ?: return

    val file = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return

    val call = file.findElementAt(editor.caretModel.offset)
      ?.getParentOfType<KtCallExpression>(false) ?: return

    val bindingContext = call.getResolutionFacade().analyze(call)

    val graph = bindingContext[InjektWritableSlices.INJECTION_GRAPH_FOR_CALL, call]
      ?: return

    if (graph !is InjectionGraph.Success) return

    val jTree = Tree()
    val structure = InjectedArgumentsTreeStructure(project, graph.results)

    val tmpDisposable = Disposable {
      jTree
    }
    val structureTreeModel = StructureTreeModel(structure, tmpDisposable)
    val asyncTreeModel = AsyncTreeModel(structureTreeModel, true, tmpDisposable)

    jTree.model = asyncTreeModel
    jTree.isRootVisible = true

    val minSize = jTree.preferredSize

    val scrollPane = ScrollPaneFactory.createScrollPane(jTree, true)

    val panel = JPanel(BorderLayout())

    panel.add(scrollPane, BorderLayout.CENTER)

    val title = "Injected arguments"

    val popup = JBPopupFactory.getInstance()
      .createComponentPopupBuilder(panel, jTree)
      .setRequestFocus(true)
      .setResizable(true)
      .setTitle(title)
      .setMinSize(Dimension(minSize.width + 700, minSize.height))
      .createPopup()

    fun navigateToSelectedElement() {
      popup.cancel()

      val selectedResult = jTree.lastSelectedPathComponent
        .safeAs<DefaultMutableTreeNode>()
        ?.userObject
        ?.cast<InjectedArgumentsTreeStructure.ResultNode>()
        ?.value
        ?: return

      when (selectedResult) {
        ResolutionResult.Success.DefaultValue -> TODO()
        is ResolutionResult.Success.WithCandidate.CircularDependency -> TODO()
        is ResolutionResult.Success.WithCandidate.Value -> {
          val psiDeclaration = selectedResult.candidate
            .safeAs<CallableInjectable>()
            ?.callable
            ?.callable
            ?.findPsiDeclarations(project, call.resolveScope)
            ?.firstOrNull()
            ?: return
          (psiDeclaration.navigationElement as? Navigatable)
            ?.navigate(true)
        }
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
}

class InjectedArgumentsTreeStructure(
  val project: Project,
  val results: Map<InjectableRequest, ResolutionResult.Success>
): AbstractTreeStructure() {
  override fun getRootElement(): Any = RootNode()

  override fun getChildElements(value: Any): Array<Any> = when (value) {
    is RootNode -> value.children.toTypedArray()
    is ResultNode -> value.children.toTypedArray()
    else -> throw AssertionError()
  }

  override fun createDescriptor(value: Any, descriptor: NodeDescriptor<*>?): NodeDescriptor<*> =
    value.cast()

  override fun getParentElement(p0: Any): Any? = null

  override fun hasSomethingToCommit(): Boolean = false

  override fun commit() {
  }

  private inner class RootNode : AbstractTreeNode<Any>(project, Any()) {
    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> =
      results.mapTo(mutableListOf()) { ResultNode(project!!, it.value) }

    override fun update(data: PresentationData) {
    }
  }

  class ResultNode(
    project: Project,
    value: ResolutionResult.Success
  ) : AbstractTreeNode<ResolutionResult.Success>(project, value) {
    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> =
      when(val value = value) {
        is ResolutionResult.Success.WithCandidate.Value -> value.dependencyResults
          .mapTo(mutableListOf()) { ResultNode(project!!, it.value) }
        else -> mutableListOf()
      }

    override fun update(data: PresentationData) {
      data.presentableText = when (val value = value) {
        ResolutionResult.Success.DefaultValue -> "default value"
        is ResolutionResult.Success.WithCandidate ->
          value.candidate.callableFqName.asString()
      }
    }
  }
}
