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
import com.ivianuu.injekt.compiler.InjektWritableSlices
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class InjektInspectionSuppressor : InspectionSuppressor {
    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        emptyArray()

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != "UnusedImport") return false
        if (element !is KtImportDirective) return false
        val context = latestBindingContext
            ?: return false
        val usedGivensByFile = context[InjektWritableSlices.USED_GIVENS_FOR_FILE,
                element.containingKtFile]
            ?: return false
        return usedGivensByFile
            .any {
                if (element.isAllUnder) it.findPackage().fqName == element.importedFqName
                else it.fqNameSafe == element.importedFqName
            }
    }
}

var latestBindingContext: BindingContext? = null
