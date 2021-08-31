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

package com.ivianuu.injekt.ide

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.analysis.AnalysisContext
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektContext
import com.ivianuu.injekt.compiler.resolution.anyType
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
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
              return it?.getAnnotatedAnnotations(InjektFqNames.Tag)
                ?.isNotEmpty() == true
            }
        else return false
      }
      "RedundantUnitReturnType" -> return element is KtUserType && element.text != "Unit"
      "RemoveExplicitTypeArguments" -> {
        if (element !is KtTypeArgumentList) return false
        val call = element.parent as? KtCallExpression
        val resolvedCall = call?.resolveToCall(BodyResolveMode.FULL)
          ?: return false
        return resolvedCall.typeArguments
          .any { (typeParameter, typeArgument) ->
            val abbreviation = typeArgument.getAbbreviation()
            (abbreviation != null && typeParameter.defaultType.supertypes()
              .none { it.getAbbreviation() == typeArgument }) ||
                typeArgument.toTypeRef(context = AnalysisContext(
                  resolvedCall.candidateDescriptor.module
                    .injektContext
                )).anyType { it.classifier.isTag }
          }
      }
      "unused" -> {
        if (element !is LeafPsiElement) return false
        val typeParameter = element.parent.safeAs<KtTypeParameter>()
          ?: return false
        return typeParameter.hasAnnotation(InjektFqNames.Spread) ||
            typeParameter.parent.parent is KtTypeAlias ||
            typeParameter.parent.parent.safeAs<KtClass>()
              ?.hasAnnotation(InjektFqNames.Tag) == true
      }
      else -> return false
    }
  }
}
