package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.hasModuleAnnotation
import com.ivianuu.injekt.compiler.makeModule
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

@OptIn(InternalNonStableExtensionPoints::class)
@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED")
open class InjektTypeResolutionInterceptorExtension(
    private val moduleChecker: ModuleChecker
) : TypeResolutionInterceptorExtension {

    override fun interceptFunctionLiteralDescriptor(
        expression: KtLambdaExpression,
        context: ExpressionTypingContext,
        descriptor: AnonymousFunctionDescriptor
    ): AnonymousFunctionDescriptor {
        if (context.expectedType.hasModuleAnnotation()) {
            // If the expected type has an @Module annotation then the literal function
            // expression should infer a an @Module annotation
            context.trace.record(InjektWritableSlices.IS_MODULE, descriptor, true)
        }
        return descriptor
    }

    override fun interceptType(
        element: KtElement,
        context: ExpressionTypingContext,
        resultType: KotlinType
    ): KotlinType {
        if (resultType === TypeUtils.NO_EXPECTED_TYPE) return resultType
        if (element !is KtLambdaExpression) return resultType
        val module = context.scope.ownerDescriptor.module
        if (context.expectedType.hasModuleAnnotation() || moduleChecker.analyze(
                context.trace,
                element,
                resultType
            )
        ) {
            return resultType.makeModule(module)
        }
        return resultType
    }
}
