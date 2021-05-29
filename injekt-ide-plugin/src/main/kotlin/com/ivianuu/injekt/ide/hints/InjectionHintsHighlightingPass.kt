package com.ivianuu.injekt.ide.hints

import com.intellij.codeHighlighting.*
import com.intellij.codeInsight.daemon.impl.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.refactoring.suggested.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjectionHintsHighlightingPass(
  private val file: KtFile,
  private val editor: Editor
) : EditorBoundHighlightingPass(editor, file, false) {

  private val hints = mutableListOf<InjectionCallHint>()

  override fun doCollectInformation(progress: ProgressIndicator) {
    if (!injectionHintsEnabled) return
    hints.clear()
    file.accept(callExpressionRecursiveVisitor { call ->
      val bindingContext = call.getResolutionFacade().analyze(call)
      val graph = bindingContext[InjektWritableSlices.INJECTION_GRAPH_FOR_CALL, call]
        ?: return@callExpressionRecursiveVisitor
      if (graph !is InjectionGraph.Success)
        return@callExpressionRecursiveVisitor
      hints += InjectionCallHint(call, graph.results.toList())
    })
  }

  override fun doApplyInformationToEditor() {
    editor.inlayModel.getInlineElementsInRange(0, Int.MAX_VALUE).forEach {
      it.dispose()
    }
    hints.forEach { hint ->
      hint.elements.forEach { element ->
        editor.inlayModel.addInlineElement(
          element.startOffset,
          element.renderer
        )
      }
    }
  }
}

class InjectionCallHint(
  val call: KtCallExpression,
  val results: List<Pair<InjectableRequest, ResolutionResult.Success>>
) {
  val elements: List<Element> = run {
    val elements = mutableListOf<Element>()
    var currentOffset = call.valueArgumentList?.rightParenthesis?.endOffset?.let { it - 1 }
      ?: call.lambdaArguments.firstOrNull()
        ?.startOffset
        ?.let { it - 1 }
      ?: return@run emptyList()
    results.forEachIndexed { index, (request, result) ->
      val text = buildString {
        if (index == 0 && call.valueArgumentList?.leftParenthesis == null) {
          append("(")
        }

        if (index == 0 && call.valueArgumentList?.arguments
            ?.isNotEmpty() == true) append(", ")

        append("${request.parameterName} = " +
            "${result.safeAs<ResolutionResult.Success.WithCandidate>()
              ?.candidate?.callableFqName?.shortName()}()")

        if (index != results.lastIndex) append(", ")

        if (index == results.lastIndex && call.valueArgumentList?.rightParenthesis == null) {
          append(")")
        }
      }
      elements += Element(currentOffset, TextPartsHintRenderer(text))
      currentOffset += text.length
    }
    elements
  }
  data class Element(
    val startOffset: Int,
    val renderer: TextPartsHintRenderer
  )
}

/*private fun ResolutionResult.Success.WithCandidate.Value.renderCall() = buildString {
  fun ResolutionResult.Success.WithCandidate.Value.render() {
    append(candidate.callableFqName.shortName())
    val candidate = candidate
    val isFunctionInvocation = candidate !is CallableInjectable ||
        candidate.callable.callable !is ValueDescriptor
    if (isFunctionInvocation) append("(")
    dependencyResults.forEach { request, success ->

    }
    if (isFunctionInvocation) append(")")
  }
}*/
