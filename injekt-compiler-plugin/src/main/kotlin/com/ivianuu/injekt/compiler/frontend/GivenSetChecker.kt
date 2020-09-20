package com.ivianuu.injekt.compiler.frontend

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

@Given
class GivenSetChecker : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor is FunctionDescriptor &&
            descriptor.hasAnnotation(InjektFqNames.GivenSet) &&
            descriptor.returnType?.constructor?.declarationDescriptor?.hasAnnotation(InjektFqNames.GivenSet) != true
        ) {
            context.trace.report(
                InjektErrors.NOT_A_GIVEN_SET
                    .on(declaration)
            )
        }

        if (descriptor is PropertyDescriptor &&
            descriptor.hasAnnotation(InjektFqNames.GivenSet) &&
            descriptor.returnType?.constructor?.declarationDescriptor?.hasAnnotation(InjektFqNames.GivenSet) != true
        ) {
            context.trace.report(
                InjektErrors.NOT_A_GIVEN_SET
                    .on(declaration)
            )
        }
    }

}