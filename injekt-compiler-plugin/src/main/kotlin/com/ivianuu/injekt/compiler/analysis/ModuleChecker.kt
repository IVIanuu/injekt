package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.typeUtil.isNothing

class ModuleChecker : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is ClassDescriptor) return
        val moduleAnnotation = descriptor.annotations.findAnnotation(InjektFqNames.Module)
            ?: return
        val component = (moduleAnnotation.argumentValue("context") as? KClassValue)
            ?.getArgumentType(descriptor.module)
            ?.constructor
            ?.declarationDescriptor
            ?.let { it as ClassDescriptor }
        if (component != null && !component.defaultType.isNothing()) {
            if (descriptor.kind != ClassKind.OBJECT &&
                descriptor.constructors.none { it.valueParameters.isEmpty() }
            ) {
                context.trace.report(
                    InjektErrors.INVALID_MODULE_WITH_TARGET
                        .on(declaration)
                )
            }

            if (!component.hasAnnotation(InjektFqNames.Context)) {
                context.trace.report(
                    InjektErrors.NOT_A_CONTEXT
                        .on(declaration)
                )
            }
        }
    }

}
