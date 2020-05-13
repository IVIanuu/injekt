package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class AnnotatedBindingChecker : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is ClassDescriptor) return
        if (!descriptor.annotations.hasAnnotation(InjektFqNames.Transient) &&
            !descriptor.hasAnnotatedAnnotations(InjektFqNames.Scope, descriptor.module)
        ) return
        if ((descriptor.kind != ClassKind.CLASS && descriptor.kind != ClassKind.OBJECT) || descriptor.modality == Modality.ABSTRACT) {
            context.trace.report(InjektErrors.ANNOTATED_BINDING_CANNOT_BE_ABSTRACT.on(declaration))
        }
    }
}