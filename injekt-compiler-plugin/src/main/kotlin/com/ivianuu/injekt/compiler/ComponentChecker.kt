package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentChecker : StorageComponentContainerContributor, CallChecker {

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        container.useInstance(this)
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val descriptor = resolvedCall.candidateDescriptor
        if (descriptor.fqNameSafe.asString() != "com.ivianuu.injekt.Component") return
        val keyArg = resolvedCall.call.valueArguments.first()
        val constant =
            ConstantExpressionEvaluator.getConstant(
                keyArg.getArgumentExpression()!!,
                context.trace.bindingContext
            )
        if (constant == null) {
            context.trace.report(InjektErrors.COMPONENT_KEY_MUST_BE_CONSTANT.on(reportOn))
        }
    }
}