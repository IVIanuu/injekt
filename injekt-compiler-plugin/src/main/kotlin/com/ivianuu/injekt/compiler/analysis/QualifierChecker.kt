package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention

class QualifierChecker : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is ClassDescriptor) return
        if (!descriptor.annotations.hasAnnotation(InjektFqNames.Qualifier)) return
        val retention = descriptor.getAnnotationRetention() ?: KotlinRetention.RUNTIME
        val targets = AnnotationChecker.applicableTargetSet(descriptor) ?: return

        if (retention != KotlinRetention.RUNTIME) {
            context.trace.report(InjektErrors.MUST_HAVE_RUNTIME_RETENTION.on(declaration))
        }

        if (KotlinTarget.EXPRESSION !in targets || KotlinTarget.TYPE !in targets) {
            context.trace.report(InjektErrors.MISSING_QUALIFIER_TARGETS.on(declaration))
        }

    }
}