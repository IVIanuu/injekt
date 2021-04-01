/*
 * Copyright 2020 Manuel Wrage
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
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.typeUtil.supertypes

class InjektInspectionSuppressor : InspectionSuppressor {
    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        emptyArray()

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        when (toolId) {
            "RedundantUnitReturnType" -> return element is KtUserType && element.text != "Unit"
            "UnusedImport" -> {
                if (element !is KtImportDirective) return false
                val context = latestBindingContext
                    ?: return false
                val usedGivensByFile = context[InjektWritableSlices.USED_GIVENS_FOR_FILE,
                        element.containingKtFile.virtualFilePath]
                    ?: return false
                return usedGivensByFile
                    .any {
                        if (element.isAllUnder) it.findPackage().fqName == element.importedFqName
                        else it.fqNameSafe == element.importedFqName
                    }
            }
            "RemoveExplicitTypeArguments" -> {
                if (element !is KtTypeArgumentList) return false
                val call = element.parent as? KtCallExpression
                val resolvedCall = call?.resolveToCall(BodyResolveMode.FULL)
                    ?: return false
                return resolvedCall.typeArguments
                    .any { (typeParameter, typeArgument) ->
                        val abbreviation = typeArgument.getAbbreviation()
                        abbreviation != null && typeParameter.defaultType.supertypes()
                            .none { it.getAbbreviation() == typeArgument }
                    }
            }
            "unused" -> {
                if (element !is LeafPsiElement) return false
                val typeParameter = element.parent as? KtTypeParameter
                    ?: return false
                return typeParameter.hasAnnotation(InjektFqNames.Given)
            }
            else -> return false
        }
    }
}

var latestBindingContext: BindingContext? = null
