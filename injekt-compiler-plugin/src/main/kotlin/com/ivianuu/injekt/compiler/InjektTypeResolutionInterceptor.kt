package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.generator.hasAnnotation
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED", "IllegalExperimentalApiUsage")
@OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
class InjektTypeResolutionInterceptor : TypeResolutionInterceptorExtension {

    override fun interceptType(
        element: KtElement,
        context: ExpressionTypingContext,
        resultType: KotlinType
    ): KotlinType {
        if (resultType === TypeUtils.NO_EXPECTED_TYPE) return resultType
        if (element !is KtLambdaExpression) return resultType
        return ANNOTATIONS.fold(resultType) { current, annotation ->
            if ((element is KtAnnotated && element.hasAnnotation(annotation)) ||
                (element.parent?.safeAs<KtAnnotated>()?.hasAnnotation(annotation) == true) ||
                (context.expectedType !== TypeUtils.NO_EXPECTED_TYPE &&
                        context.expectedType.hasAnnotation(annotation))) {
                            current.withAnnotation(annotation, context.scope.ownerDescriptor.module)
            } else current
        }
    }

    private fun KotlinType.withAnnotation(fqName: FqName, module: ModuleDescriptor): KotlinType {
        if (hasAnnotation(fqName)) return this
        val annotation = makeAnnotation(fqName, module)
        return replaceAnnotations(Annotations.create(annotations + annotation))
    }

    private fun makeAnnotation(fqName: FqName, module: ModuleDescriptor): AnnotationDescriptor =
        object : AnnotationDescriptor {
            override val type: KotlinType
                get() = module.findClassAcrossModuleDependencies(
                    ClassId.topLevel(fqName)
                )!!.defaultType
            override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
            override val source: SourceElement get() = SourceElement.NO_SOURCE
        }

    private companion object {
        private val ANNOTATIONS = listOf(
            InjektFqNames.Binding,
            InjektFqNames.Interceptor,
            InjektFqNames.Module,
            InjektFqNames.MapEntries,
            InjektFqNames.SetElements
        )
    }
}
