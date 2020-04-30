package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.SupportedMapKeyTypes
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
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
        }
    }

}