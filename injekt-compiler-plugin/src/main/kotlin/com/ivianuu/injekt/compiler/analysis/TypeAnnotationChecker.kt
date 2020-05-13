package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getOrPut
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.upperIfFlexible

class TypeAnnotationChecker : AdditionalTypeChecker {

    fun hasTypeAnnotation(
        trace: BindingTrace,
        descriptor: FunctionDescriptor,
        fqName: FqName
    ): Boolean = fqName in getTypeAnnotations(trace, descriptor)

    fun getTypeAnnotations(
        trace: BindingTrace,
        descriptor: FunctionDescriptor
    ): Set<FqName> {
        val psi = descriptor.findPsi() as? KtElement

        psi?.let {
            trace.bindingContext.get(InjektWritableSlices.TYPE_ANNOTATIONS, it)?.let {
                return it
            }
        }

        val typeAnnotations = mutableSetOf<FqName>()

        typeAnnotations +=
            descriptor.getAnnotatedAnnotations(InjektFqNames.TypeAnnotation, descriptor.module)
                .map { it.fqName!! }

        typeAnnotations += (descriptor as? PropertyGetterDescriptor)?.correspondingProperty
            ?.getAnnotatedAnnotations(InjektFqNames.TypeAnnotation, descriptor.module)
            ?.map { it.fqName!! } ?: emptyList()
        typeAnnotations += trace.bindingContext.get(
            InjektWritableSlices.TYPE_ANNOTATIONS,
            descriptor
        ) ?: emptySet()

        psi?.let {
            trace.getOrPut(
                InjektWritableSlices.TYPE_ANNOTATIONS,
                it
            ) { mutableSetOf() } += typeAnnotations
        }

        return typeAnnotations
    }

    fun getTypeAnnotations(
        trace: BindingTrace,
        element: KtElement,
        module: ModuleDescriptor,
        type: KotlinType?
    ): Set<FqName> {
        trace.bindingContext.get(InjektWritableSlices.TYPE_ANNOTATIONS, element)?.let {
            return it
        }

        val typeAnnotations = mutableSetOf<FqName>()

        if (element is KtParameter) {
            typeAnnotations += element
                .typeReference
                ?.annotationEntries
                ?.mapNotNull { trace.bindingContext.get(BindingContext.ANNOTATION, it) }
                ?.mapNotNull {
                    if (it.type.constructor.declarationDescriptor
                            ?.annotations?.hasAnnotation(InjektFqNames.TypeAnnotation) == true
                    )
                        it.fqName else null
                } ?: emptyList()
        }

        typeAnnotations +=
            type?.getAnnotatedAnnotations(InjektFqNames.TypeAnnotation, module)
                ?.map { it.fqName!! } ?: emptyList()

        val parent = element.parent
        val annotations = when {
            element is KtNamedFunction -> element.annotationEntries
            parent is KtAnnotatedExpression -> parent.annotationEntries
            element is KtProperty -> element.annotationEntries
            element is KtParameter -> element.typeReference?.annotationEntries ?: emptyList()
            else -> emptyList()
        }

        typeAnnotations += annotations
            .mapNotNull { trace.bindingContext.get(BindingContext.ANNOTATION, it) }
            .map { it.fqName!! }

        if (typeAnnotations.isNotEmpty()) {
            trace.getOrPut(
                InjektWritableSlices.TYPE_ANNOTATIONS,
                element
            ) { mutableSetOf() } += typeAnnotations
        }

        return typeAnnotations
    }

    override fun checkType(
        expression: KtExpression,
        expressionType: KotlinType,
        expressionTypeWithSmartCast: KotlinType,
        c: ResolutionContext<*>
    ) {
        if (expression is KtLambdaExpression) {
            val expectedType = c.expectedType
            if (expectedType === TypeUtils.NO_EXPECTED_TYPE) return
            val expectedTypeAnnotations =
                expectedType.getAnnotatedAnnotations(
                        InjektFqNames.TypeAnnotation,
                        c.scope.ownerDescriptor.module
                    )
                    .map { it.fqName!! }
                    .toSet()
                    .sortedBy { it.asString() }
            val annotations = getTypeAnnotations(
                c.trace,
                expression,
                c.scope.ownerDescriptor.module,
                c.expectedType
            )
                .sortedBy { it.asString() }
            if (expectedTypeAnnotations != annotations) {
                val isInlineable =
                    InlineUtil.isInlinedArgument(
                        expression.functionLiteral,
                        c.trace.bindingContext,
                        true
                    )
                if (isInlineable) return

                val reportOn =
                    if (expression.parent is KtAnnotatedExpression)
                        expression.parent as KtExpression
                    else expression
                c.trace.report(
                    Errors.TYPE_MISMATCH.on(
                        reportOn,
                        expectedType,
                        expressionTypeWithSmartCast
                    )
                )
            }
            return
        } else {
            val expectedType = c.expectedType

            if (expectedType === TypeUtils.NO_EXPECTED_TYPE) return
            if (expectedType === TypeUtils.UNIT_EXPECTED_TYPE) return

            val nullableAnyType = expectedType.builtIns.nullableAnyType
            val anyType = expectedType.builtIns.anyType

            if (anyType == expectedType.lowerIfFlexible() &&
                nullableAnyType == expectedType.upperIfFlexible()
            ) return

            val nullableNothingType = expectedType.builtIns.nullableNothingType

            if (expectedType.isMarkedNullable &&
                expressionTypeWithSmartCast == nullableNothingType
            ) return

            val expectedTypeAnnotations =
                expectedType.getAnnotatedAnnotations(
                        InjektFqNames.TypeAnnotation,
                        c.scope.ownerDescriptor.module
                    )
                    .map { it.fqName!! }
                    .toSet()
                    .sortedBy { it.asString() }
            val typeAnnotations = expressionType.getAnnotatedAnnotations(
                    InjektFqNames.TypeAnnotation,
                    c.scope.ownerDescriptor.module
                )
                .map { it.fqName!! }
                .toSet()
                .sortedBy { it.asString() }

            if (expectedTypeAnnotations != typeAnnotations) {
                val reportOn =
                    if (expression.parent is KtAnnotatedExpression)
                        expression.parent as KtExpression
                    else expression
                c.trace.report(
                    Errors.TYPE_MISMATCH.on(
                        reportOn,
                        expectedType,
                        expressionTypeWithSmartCast
                    )
                )
            }
            return
        }
    }

}