package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

class GivenFunChecker : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        if (descriptor !is SimpleFunctionDescriptor) return
        if (!descriptor.hasAnnotation(InjektFqNames.GivenFun)) return

        declaration as KtNamedFunction
        if (!declaration.hasDeclaredReturnType() && !declaration.hasBlockBody()) {
            context.trace.report(
                InjektErrors.GIVEN_FUN_WITHOUT_EXPLICIT_RETURN_TYPE
                    .on(declaration)
            )
        }

        if (descriptor.containingDeclaration is ClassDescriptor) {
            context.trace.report(
                InjektErrors.GIVEN_FUN_AS_MEMBER
                    .on(declaration)
            )
        } else {
            val functionsWithSameName = descriptor.findPackage().getMemberScope()
                .getContributedFunctions(descriptor.name, NoLookupLocation.FROM_BACKEND)
                .filter { it.hasAnnotation(InjektFqNames.GivenFun) }
            if (functionsWithSameName.size > 1) {
                context.trace.report(
                    InjektErrors.GIVEN_FUN_MUST_HAVE_UNIQUE_NAME
                        .on(declaration)
                )
            }
        }
    }
}
