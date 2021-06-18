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

package com.ivianuu.injekt.ide.suppression

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.ide.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

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
        return element.parent.safeAs<KtTypeParameter>()
          ?.hasAnnotation(InjektFqNames.Spread) == true
      }
      else -> return false
    }
  }
}
