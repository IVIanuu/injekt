package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.SupportedMapKeyTypes
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class MapChecker : CallChecker {

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val descriptor = resolvedCall.resultingDescriptor
        if (descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.map") {
            val mapKeyType = resolvedCall.typeArguments.toList()
                .first()
                .second
                ?.constructor?.declarationDescriptor?.fqNameSafe

            if (mapKeyType !in SupportedMapKeyTypes) {
                context.trace.report(InjektErrors.UNSUPPORTED_MAP_KEY_TYPE.on(reportOn))
            }
        } else if (descriptor.dispatchReceiverParameter?.type?.constructor?.declarationDescriptor ==
            context.moduleDescriptor.findClassAcrossModuleDependencies(
                ClassId.topLevel(
                    InjektFqNames.MapDsl
                )
            )
            && descriptor.name.asString() == "put"
        ) {
            val keyArg = resolvedCall.call.valueArguments.single()
            val constant = ConstantExpressionEvaluator.getConstant(
                keyArg.getArgumentExpression()!!,
                context.trace.bindingContext
            )
            if (constant == null && keyArg.getArgumentExpression() !is KtClassLiteralExpression) {
                context.trace.report(InjektErrors.MAP_KEY_MUST_BE_CONSTANT.on(reportOn))
            }
        }
    }

}