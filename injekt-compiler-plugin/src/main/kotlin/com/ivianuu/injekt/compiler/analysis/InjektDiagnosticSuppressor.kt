package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.descriptor
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.utils.addToStdlib.cast

class InjektDiagnosticSuppressor : DiagnosticSuppressor {

    override fun isSuppressed(diagnostic: Diagnostic): Boolean =
        isSuppressed(diagnostic, null)

    override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
        if (bindingContext == null) return false

        if (diagnostic.factory == Errors.WRONG_ANNOTATION_TARGET) {
            val annotationDescriptor = bindingContext[BindingContext.ANNOTATION, diagnostic.psiElement.cast()]
            if (annotationDescriptor?.type?.constructor?.declarationDescriptor
                    ?.hasAnnotation(InjektFqNames.Qualifier) == true)
                        return true
        }

        if (diagnostic.factory == Errors.UNDERSCORE_IS_RESERVED) {
            val descriptor = try {
                (diagnostic.psiElement.parent as? KtDeclaration)
                    ?.descriptor<ParameterDescriptor>(bindingContext)
            } catch (e: Throwable) {
                null
            } ?: return false
            if (descriptor.hasAnnotation(InjektFqNames.Given)) return true
        }

        if (diagnostic.factory == Errors.UNUSED_PARAMETER) {
            val descriptor =
                (diagnostic.psiElement as KtDeclaration).descriptor<ParameterDescriptor>(
                    bindingContext)
                    ?: return false
            try {
                if (bindingContext[InjektWritableSlices.USED_GIVEN, descriptor] != null) return true
            } catch (e: Throwable) {
            }
        }

        return false
    }
}
