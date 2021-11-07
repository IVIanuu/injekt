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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.addInjectNInfo
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.descriptor
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.isIde
import com.ivianuu.injekt.compiler.resolution.anyType
import com.ivianuu.injekt.compiler.resolution.isInject
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.lambdaExpressionRecursiveVisitor
import org.jetbrains.kotlin.psi.propertyRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class InjektDiagnosticSuppressor : DiagnosticSuppressor {
  override fun isSuppressed(diagnostic: Diagnostic): Boolean =
    isSuppressed(diagnostic, null)

  override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
    if (bindingContext == null)
      return false

    @Provide val ctx = bindingContext[InjektWritableSlices.INJEKT_CONTEXT, Unit]
      ?: return false

    if (diagnostic.factory == InjektErrors.FILE_DECOY) {
      @Provide val innerCtx = bindingContext[InjektWritableSlices.INJEKT_CONTEXT, Unit]
        ?.withTrace(DelegatingBindingTrace(bindingContext, "dummy"))
        ?: return false
      diagnostic.psiElement.cast<KtFile>()
        .accept(
          lambdaExpressionRecursiveVisitor { lambdaExpression ->
            lambdaExpression.functionLiteral.descriptor<AnonymousFunctionDescriptor>()
              ?.addInjectNInfo()
            lambdaExpression.getType(bindingContext)?.addInjectNInfo()
          }
        )
      diagnostic.psiElement.cast<KtFile>()
        .accept(
          propertyRecursiveVisitor { property ->
            if (property.isLocal)
              property.descriptor<CallableDescriptor>()?.addInjectNInfo()
          }
        )
      return true
    }

    if (diagnostic.factory == Errors.UNRESOLVED_REFERENCE)
      return bindingContext[InjektWritableSlices.FIXED_TYPE, diagnostic.psiElement.text] != null

    if (diagnostic.factory == Errors.INAPPLICABLE_INFIX_MODIFIER ||
      diagnostic.factory == Errors.INAPPLICABLE_OPERATOR_MODIFIER
    )
      return diagnostic.psiElement.parent.parent.safeAs<KtNamedFunction>()
        ?.descriptor<CallableDescriptor>()
        ?.valueParameters
        ?.filterNot { it.isInject() }
        ?.size
        ?.let { it <= 1 } == true

    if (diagnostic.factory == Errors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT)
      return true

    if (diagnostic.factory == Errors.UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE)
      return true

    if (diagnostic.factory == Errors.UNSUPPORTED) {
      val typeParameter = diagnostic.psiElement.parent?.parent as? KtTypeParameter
      if (typeParameter?.hasAnnotation(injektFqNames().spread) == true) return true
    }

    if (diagnostic.factory == Errors.WRONG_ANNOTATION_TARGET) {
      val annotationDescriptor =
        bindingContext[BindingContext.ANNOTATION, diagnostic.psiElement.cast()]
      if (annotationDescriptor?.type?.constructor?.declarationDescriptor
          ?.hasAnnotation(injektFqNames().tag) == true
      )
        return true
    }

    if (diagnostic.factory == InjektErrors.UNUSED_INJECTABLE_IMPORT) {
      val filePath = diagnostic.psiElement.containingFile.safeAs<KtFile>()?.virtualFilePath
      if (filePath != null) {
        return bindingContext[InjektWritableSlices.USED_IMPORT,
            SourcePosition(
              filePath,
              diagnostic.psiElement.startOffset,
              diagnostic.psiElement.endOffset
            )] != null
      }
    }

    if (diagnostic.factory == Errors.NOTHING_TO_INLINE) {
      val function = diagnostic.psiElement.getParentOfType<KtNamedFunction>(false)
      if (function?.hasAnnotation(injektFqNames().provide) == true ||
          function?.valueParameters?.any {
            it.hasAnnotation(injektFqNames().inject) ||
                it.hasAnnotation(injektFqNames().provide)
          } == true)
            return true
    }

    if (diagnostic.factory == Errors.UNUSED_TYPEALIAS_PARAMETER) {
      val typeParameter = diagnostic.psiElement
        .cast<KtTypeParameter>().descriptor<TypeParameterDescriptor>()
      return diagnostic.psiElement.getParentOfType<KtTypeAlias>(false)
        ?.descriptor<TypeAliasDescriptor>()
        ?.expandedType
        ?.toTypeRef()
        ?.anyType { it.classifier.descriptor == typeParameter } == true
    }

    return false
  }
}
