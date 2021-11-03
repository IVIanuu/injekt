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

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import com.intellij.util.ThreeState
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.injektContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.completion.KotlinCompletionExtension
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class ImportReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement(KtStringTemplateExpression::class.java),
      object : PsiReferenceProvider() {
        override fun getReferencesByElement(
          element: PsiElement,
          context: ProcessingContext
        ): Array<PsiReference> {
          if (!element.isInjektEnabled()) return emptyArray()

          element as KtStringTemplateExpression
          if (!element.isProviderImport()) return emptyArray()

          val importedFqName = element.text
            .removeSurrounding("\"")
            .removeSuffix("IntellijIdeaRulezzz")

          val refs = mutableListOf<PsiReference>()

          val module = element.getResolutionFacade().moduleDescriptor
          val injektContext = module.injektContext
          val psiFacade = KotlinJavaPsiFacade.getInstance(element.project)

          fun resolveFqName(fqName: String, endOffset: Int) {
            if (fqName.isEmpty()) return
            if (fqName.endsWith(".*")) {
              return resolveFqName(fqName.removeSuffix(".*"), endOffset - 2)
            }
            val startOffset = fqName.lastIndexOf(".") + 1

            val range = TextRange.create(
              startOffset + 1, endOffset
            )

            val finalFqName = FqName(fqName)

            refs += ImportElementReference(
              element,
              range,
              lazy@ {
                val packageDescriptor = module.getPackage(finalFqName)
                if (packageDescriptor.fragments.isNotEmpty()) {
                  return@lazy psiFacade.findPackage(finalFqName.asString(), element.resolveScope)
                }

                injektContext.memberScopeForFqName(finalFqName.parent(), NoLookupLocation.FROM_IDE)
                  ?.getContributedDescriptors { it == finalFqName.shortName() }
                  ?.firstOrNull()
                  ?.findPsiDeclarations(element.project, element.resolveScope)
                  ?.first()
              },
              finalFqName
            )

            resolveFqName(
              finalFqName.parent().asString(),
              endOffset - range.length - 1
            )
          }

          resolveFqName(importedFqName, element.textLength - 1)

          return refs.toTypedArray()
        }
      }
    )
  }
}

fun KtStringTemplateExpression.isProviderImport() = getParentOfType<KtAnnotationEntry>(false)
  ?.takeIf { it.typeReference?.text == InjektFqNames.Providers.shortName().asString() } != null

class ImportCompletionExtension : KotlinCompletionExtension() {
  override fun perform(parameters: CompletionParameters, result: CompletionResultSet): Boolean {
    val template = parameters.position.getStrictParentOfType<KtStringTemplateExpression>()
      ?: return false
    if (!template.isPlain()) return false

    val references = template.references
    val importReference = references.firstIsInstanceOrNull<ImportElementReference>()
      ?: return false

    val startOffset = parameters.offset
    val offsetInElement = startOffset - template.startOffset
    val range = importReference.rangeInElement
    if (offsetInElement < range.startOffset) return false

    val prefix = template.text.substring(range.startOffset, offsetInElement)

    val module = template.getResolutionFacade().moduleDescriptor
    val injektContext = module.injektContext

    injektContext.memberScopeForFqName(
      importReference.fqName.parent(),
      NoLookupLocation.FROM_IDE
    )
      ?.getContributedDescriptors()
      ?.filter { declaration ->
        declaration.name.asString().startsWith(prefix) &&
            !declaration.name.isSpecial && (declaration !is FunctionDescriptor ||
            // ignore our incremental fix functions
            !declaration.name.asString().endsWith("injectables"))
      }
      ?.mapTo(mutableListOf()) { declaration ->
        LookupElementBuilder.create(declaration.name.asString())
          .withIcon(KotlinDescriptorIconProvider.getIcon(declaration, null, 0))
      }
      ?.distinct()
      ?.let { result.addAllElements(it) }

    return true
  }
}

class ImportTypedHandlerDelegate : TypedHandlerDelegate() {
  override fun beforeCharTyped(
    c: Char,
    project: Project,
    editor: Editor,
    file: PsiFile,
    fileType: FileType
  ): Result {
    val caretOffset = editor.caretModel.offset

    file.findElementAt(caretOffset)
      ?.getParentOfType<KtStringTemplateExpression>(false)
      ?.getParentOfType<KtAnnotationEntry>(false)
      ?.takeIf {
        it.typeReference?.text == InjektFqNames.Providers.shortName().asString()
      }
      ?: return super.beforeCharTyped(c, project, editor, file, fileType)

    AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, null)

    return super.beforeCharTyped(c, project, editor, file, fileType)
  }

  override fun checkAutoPopup(
    charTyped: Char,
    project: Project,
    editor: Editor,
    file: PsiFile
  ): Result {
    val caretOffset = editor.caretModel.offset

    file.findElementAt(caretOffset)
      ?.getParentOfType<KtStringTemplateExpression>(false)
      ?.getParentOfType<KtAnnotationEntry>(false)
      ?.takeIf {
        it.typeReference?.text == InjektFqNames.Providers.shortName().asString()
      }
      ?: return super.checkAutoPopup(charTyped, project, editor, file)

    AutoPopupController.getInstance(file.project).scheduleAutoPopup(editor)

    return Result.STOP
  }
}

class ImportCompletionConfidence : CompletionConfidence() {
  override fun shouldSkipAutopopup(
    contextElement: PsiElement,
    psiFile: PsiFile,
    offset: Int
  ): ThreeState = contextElement
    .getParentOfType<KtStringTemplateExpression>(false)
    ?.getParentOfType<KtAnnotationEntry>(false)
    ?.takeIf {
      it.typeReference?.text == InjektFqNames.Providers.shortName().asString()
    }
    ?.let { ThreeState.NO } ?: ThreeState.UNSURE
}

class ImportElementReference(
  element: KtStringTemplateExpression,
  rangeInElement: TextRange,
  computeTarget: () -> PsiElement?,
  val fqName: FqName
) : PsiReferenceBase<KtStringTemplateExpression>(
  element,
  rangeInElement
) {
  private val target by lazy(computeTarget)
  override fun resolve(): PsiElement? = target
}
