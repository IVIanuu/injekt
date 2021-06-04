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

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.ui.popup.util.*
import com.intellij.psi.search.searches.*
import com.intellij.psi.util.*
import com.intellij.ui.popup.list.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.ide.*
import com.ivianuu.injekt.ide.refs.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*

class ShowInjectionUsagesAction : AnAction("Show injection usages") {
  override fun actionPerformed(event: AnActionEvent) {
    val context = event.dataContext
    val editor = CommonDataKeys.EDITOR.getData(context) ?: return
    val project = CommonDataKeys.PROJECT.getData(context) ?: return

    val file = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return

    val selectedDeclaration = file.findElementAt(editor.caretModel.offset)
      ?.getParentOfType<KtDeclaration>(false)
      ?.takeIf { it.isProvideOrInjectDeclaration() }
      ?: return

    val targetDescriptors = selectedDeclaration.resolveToDescriptorIfAny()
      ?.let { descriptor ->
        when (descriptor) {
          is ClassDescriptor ->
            descriptor.provideConstructors(descriptor.module.injektContext, null)
          is CallableDescriptor -> descriptor
          else -> null
        }
      }
      ?: return

    val referencingExpressions = ReferencesSearch.search(selectedDeclaration, selectedDeclaration.resolveScope)
      .findAll()
      .filterIsInstance<InjectReference>()
      .map { it.element }

    if (referencingExpressions.size == 1) {
      showInjectedArgumentsPopup(referencingExpressions.single(), project, editor)
    } else {
      ListPopupImpl(
        object : BaseListPopupStep<KtExpression>(
          "Pick call",
          referencingExpressions
        ) {
          override fun isAutoSelectionEnabled() = false

          override fun isSpeedSearchEnabled() = true

          override fun onChosen(
            selectedValue: KtExpression?,
            finalChoice: Boolean
          ): PopupStep<String>? {
            if (selectedValue == null || !finalChoice) return null
            showInjectedArgumentsPopup(selectedValue, project, editor)
            return null
          }

          override fun hasSubstep(selectedValue: KtExpression?) = false
          override fun getTextFor(value: KtExpression) = value.text
          override fun getIconFor(value: KtExpression) = null
        }
      ).showInBestPositionFor(editor)
    }
  }
}
