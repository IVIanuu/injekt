/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.ide

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.getTags
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.resolution.anyType
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class InjektInspectionSuppressor : InspectionSuppressor {
  override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
    emptyArray()

  override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
    if (!element.isInjektEnabled()) return false
    when (toolId) {
      "RedundantExplicitType" -> {
        if (element is KtTypeReference)
          element.getResolutionFacade().analyze(element, BodyResolveMode.FULL)
            .let { element.getAbbreviatedTypeOrType(it) }
            .let {
              return it?.getTags(element.injektFqNames())?.isNotEmpty() == true
            }
        else return false
      }
      "RedundantUnitReturnType" -> return element is KtUserType && element.text != "Unit"
      "RemoveExplicitTypeArguments" -> {
        val injektFqNames = element.injektFqNames()
        if (element !is KtTypeArgumentList) return false
        val call = element.parent as? KtCallExpression
        val bindingContext = call?.analyze() ?: return false
        val resolvedCall = call.getResolvedCall(bindingContext) ?: return false
        return resolvedCall.typeArguments
          .any { (typeParameter, typeArgument) ->
            val abbreviation = typeArgument.getAbbreviation()
            (abbreviation != null && typeParameter.defaultType.supertypes()
              .none { it.getAbbreviation() == typeArgument }) ||
                typeArgument.toTypeRef(ctx = Context(
                  resolvedCall.candidateDescriptor.module,
                  injektFqNames,
                  DelegatingBindingTrace(bindingContext, "injekt")
                ))
                  .anyType { it.classifier.isTag }
          }
      }
      "unused" -> {
        if (element !is LeafPsiElement) return false
        val typeParameter = element.parent.safeAs<KtTypeParameter>()
          ?: return false
        val injektFqNames = element.injektFqNames()
        return typeParameter.hasAnnotation(injektFqNames.spread) ||
            typeParameter.parent.parent is KtTypeAlias ||
            typeParameter.parent.parent.safeAs<KtClass>()
              ?.hasAnnotation(injektFqNames.tag) == true
      }
      else -> return false
    }
  }
}
