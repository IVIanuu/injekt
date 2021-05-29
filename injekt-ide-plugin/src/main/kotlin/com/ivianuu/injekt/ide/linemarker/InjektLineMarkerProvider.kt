package com.ivianuu.injekt.ide.linemarker

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjektLineMarkerProvider : LineMarkerProvider {
  private class InjectCallMarkerInfo(callElement: PsiElement, message: String) : LineMarkerInfo<PsiElement>(
    callElement,
    callElement.textRange,
    InjectionCallIcon,
    Pass.LINE_MARKERS,
    { message },
    null,
    GutterIconRenderer.Alignment.RIGHT
  ) {
    override fun createGutterRenderer(): GutterIconRenderer? {
      return object : LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement>(this) {
        override fun getClickAction(): AnAction? = null
      }
    }
  }

  override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

  override fun collectSlowLineMarkers(
    elements: MutableList<out PsiElement>,
    result: MutableCollection<in LineMarkerInfo<*>>
  ) {
    /*for (element in elements) {
      if (element !is KtCallExpression) continue
      if (element.performsInjections()) {
        result += InjectCallMarkerInfo(getElementForLineMark(element), "Performs injection")
      }
    }*/
  }

  private fun KtCallExpression.performsInjections(
    context: BindingContext = analyze(BodyResolveMode.PARTIAL)
  ): Boolean {
    val resolvedCall = getResolvedCall(context) ?: return false
    val module = resolvedCall.resultingDescriptor.module.injektContext
    val injektContext = module.injektContext
    return resolvedCall.valueArguments.any {
      it.key.isInject(injektContext, injektContext.trace) ||
          it.value.arguments.any {
            it.getArgumentExpression()
              ?.safeAs<KtCallExpression>()
              ?.performsInjections(context) == true
          }
    }
  }

  private fun getElementForLineMark(callElement: PsiElement): PsiElement =
    when (callElement) {
      is KtSimpleNameExpression -> callElement.getReferencedNameElement()
      else -> generateSequence(callElement, { it.firstChild }).last()
    }
}

private val InjectionCallIcon = IconLoader.getIcon("icons/needle.svg")

