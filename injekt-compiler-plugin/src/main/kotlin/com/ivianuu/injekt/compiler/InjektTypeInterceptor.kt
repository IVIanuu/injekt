package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

@OptIn(InternalNonStableExtensionPoints::class)
@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED")
open class InjektTypeResolutionInterceptorExtension : TypeResolutionInterceptorExtension {

    override fun interceptFunctionLiteralDescriptor(
        expression: KtLambdaExpression,
        context: ExpressionTypingContext,
        descriptor: AnonymousFunctionDescriptor
    ): AnonymousFunctionDescriptor {
        if (context.expectedType.hasModuleAnnotation()) {
            /*// If the expected type has an @Composable annotation then the literal function
            // expression should infer a an @Composable annotation
            context.trace.record(INFERRED_COMPOSABLE_DESCRIPTOR, descriptor, true)*/
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
        if ((context.expectedType.hasModuleAnnotation())) {
            return resultType.makeModule(module)
        }
        return resultType
    }
}

fun KotlinType.hasModuleAnnotation(): Boolean =
    !isSpecialType && annotations.findAnnotation(InjektClassNames.Module) != null

internal val KotlinType.isSpecialType: Boolean
    get() =
        this === TypeUtils.NO_EXPECTED_TYPE || this === TypeUtils.UNIT_EXPECTED_TYPE

fun KotlinType.makeModule(module: ModuleDescriptor): KotlinType {
    if (hasModuleAnnotation()) return this
    val annotation = makeModuleAnnotation(module)
    return replaceAnnotations(Annotations.create(annotations + annotation))
}

private fun makeModuleAnnotation(module: ModuleDescriptor): AnnotationDescriptor =
    object : AnnotationDescriptor {
        override val type: KotlinType
            get() = module.findClassAcrossModuleDependencies(
                ClassId.topLevel(InjektClassNames.Module)
            )!!.defaultType
        override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
        override val source: SourceElement get() = SourceElement.NO_SOURCE
        override fun toString() = "[@Module]"
    }
