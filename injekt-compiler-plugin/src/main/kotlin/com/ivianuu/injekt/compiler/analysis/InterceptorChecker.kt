package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.resolution.callContext
import com.ivianuu.injekt.compiler.resolution.providerType
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.resolution.typeWith
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class InterceptorChecker : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (!descriptor.hasAnnotation(InjektFqNames.Interceptor) &&
            (descriptor !is ValueDescriptor ||
                    !descriptor.type.hasAnnotation(InjektFqNames.Interceptor)))
                        return
        if (descriptor is FunctionDescriptor) {
            val providerType = descriptor.callContext.providerType(descriptor.module)
                .typeWith(listOf(descriptor.returnType!!.toTypeRef()))
            val factoryParameter = descriptor
                .valueParameters
                .singleOrNull { it.type.toTypeRef() == providerType }
            if (factoryParameter == null) {
                context.trace.report(
                    InjektErrors.INTERCEPTOR_WITHOUT_FACTORY_PARAMETER
                        .on(declaration)
                )
            }
        } else if (descriptor is ValueDescriptor) {
            val providerType = descriptor.type.toTypeRef()
                .callContext
                .providerType(descriptor.module)
                .typeWith(listOf(descriptor.type.arguments.last().type.toTypeRef()))
            val factoryParameter = descriptor
                .type
                .toTypeRef()
                .typeArguments
                .singleOrNull { it == providerType }
            if (factoryParameter == null) {
                context.trace.report(
                    InjektErrors.INTERCEPTOR_WITHOUT_FACTORY_PARAMETER
                        .on(declaration)
                )
            }
        }
    }
}
