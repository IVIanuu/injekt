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
    file.accept(expressionRecursiveVisitor { call ->
      val bindingContext = call.getResolutionFacade().analyze(call)
      val graph = bindingContext[InjektWritableSlices.INJECTION_GRAPH_FOR_CALL, call]
        ?: return@expressionRecursiveVisitor
      if (graph !is InjectionGraph.Success)
        return@expressionRecursiveVisitor
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
  val expression: KtExpression,
  val results: List<Pair<InjectableRequest, ResolutionResult.Success>>
) {
  val elements: List<Element> = run {
    val elements = mutableListOf<Element>()
    var currentOffset = expression.safeAs<KtCallExpression>()
      ?.valueArgumentList?.rightParenthesis?.endOffset?.let { it - 1 }
      ?: expression.safeAs<KtCallExpression>()?.lambdaArguments?.firstOrNull()
        ?.startOffset
        ?.let { it - 1 }
      ?: return@run emptyList()
    results.forEachIndexed { index, (request, result) ->
      val text = buildString {
        if (index == 0 &&
          expression.safeAs<KtCallExpression>()?.valueArgumentList?.leftParenthesis == null) {
          append("(")
        }

        if (index == 0 && expression.safeAs<KtCallExpression>()?.valueArgumentList?.arguments
            ?.isNotEmpty() == true) append(", ")

        append("${request.parameterName} = " +
            "${result.safeAs<ResolutionResult.Success.WithCandidate>()
              ?.candidate?.callableFqName?.shortName()}()")

        if (index != results.lastIndex) append(", ")

        if (index == results.lastIndex &&
          expression.safeAs<KtCallExpression>()?.valueArgumentList?.rightParenthesis == null) {
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
