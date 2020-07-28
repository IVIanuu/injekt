/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isMarkedAsImplicit
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.kotlin.utils.addToStdlib.cast

class ImplicitChecker : CallChecker, DeclarationChecker, AdditionalTypeChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        when (descriptor) {
            is ClassDescriptor -> checkClass(declaration, descriptor, context)
            is FunctionDescriptor -> checkFunction(declaration, descriptor, context)
            is PropertyDescriptor -> checkProperty(declaration, descriptor, context)
        }

        var implicitAnnotations = 0
        if (descriptor.hasAnnotation(InjektFqNames.Given)) implicitAnnotations += 1
        if (descriptor.hasAnnotation(InjektFqNames.Reader)) implicitAnnotations += 1
        if (descriptor.hasAnnotation(InjektFqNames.MapEntries)) implicitAnnotations += 1
        if (descriptor.hasAnnotation(InjektFqNames.SetElements)) implicitAnnotations += 1

        if (implicitAnnotations > 1) {
            context.trace.report(
                InjektErrors.MULTIPLE_IMPLICIT_ANNOTATIONS
                    .on(declaration)
            )
        }
    }

    private fun checkClass(
        declaration: KtDeclaration,
        descriptor: ClassDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (!descriptor.isMarkedAsImplicit() &&
            descriptor.constructors.none { it.isMarkedAsImplicit() }
        ) return

        if (descriptor.kind == ClassKind.INTERFACE) {
            context.trace.report(
                InjektErrors.READER_CLASS_CANNOT_BE_INTERFACE
                    .on(declaration)
            )
        }

        if (descriptor.kind == ClassKind.OBJECT) {
            context.trace.report(
                InjektErrors.READER_CLASS_CANNOT_BE_OBJECT
                    .on(declaration)
            )
        }
    }

    private fun checkFunction(
        declaration: KtDeclaration,
        descriptor: FunctionDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (!isImplicit(descriptor, context.trace) &&
            (descriptor !is ConstructorDescriptor ||
                    !descriptor.constructedClass.isMarkedAsImplicit())
        ) return
    }

    private fun checkProperty(
        declaration: KtDeclaration,
        descriptor: PropertyDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (declaration !is KtProperty) return
        if (!descriptor.hasAnnotation(InjektFqNames.Reader)) return

        if (declaration.initializer != null) {
            context.trace.report(
                InjektErrors.READER_PROPERTY_WITH_BACKING_FIELD
                    .on(declaration)
            )
        }

        if (descriptor.isVar) {
            context.trace.report(
                InjektErrors.READER_PROPERTY_VAR
                    .on(declaration)
            )
        }
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val resulting = resolvedCall.resultingDescriptor

        if (resulting.fqNameSafe.asString() == "com.ivianuu.injekt.runReader") {
            val receiver = resolvedCall.extensionReceiver!!.type
            if (receiver.constructor.declarationDescriptor?.annotations
                    ?.hasAnnotation(InjektFqNames.Component) != true &&
                !receiver.isTypeParameter()
            ) {
                context.trace.report(
                    InjektErrors.NOT_A_COMPONENT
                        .on(reportOn)
                )
            }
        }

        if (resulting !is FunctionDescriptor) return

        if (isImplicit(resulting, context.trace)) {
            checkInvocations(reportOn, context)
        }

        if (resulting is ConstructorDescriptor &&
            (resulting.constructedClass.isMarkedAsImplicit())
        ) {
            checkInvocations(reportOn, context)
        }

        if (isImplicit(resulting, context.trace)) {
            checkInvocations(reportOn, context)
        }
    }

    private fun checkInvocations(
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val enclosingReaderContext = findEnclosingContext(context) {
            isImplicit(it, context.trace)
        }

        if (enclosingReaderContext == null) {
            context.trace.report(
                InjektErrors.FORBIDDEN_READER_INVOCATION.on(reportOn)
            )
        }
    }

    private inline fun findEnclosingContext(
        context: CallCheckerContext,
        predicate: (DeclarationDescriptor) -> Boolean
    ): DeclarationDescriptor? = context.scope
        .parentsWithSelf.firstOrNull {
            it is LexicalScope && predicate(it.ownerDescriptor)
        }?.cast<LexicalScope>()?.ownerDescriptor

    fun isImplicit(
        descriptor: DeclarationDescriptor,
        trace: BindingTrace
    ): Boolean {
        val psi = descriptor.findPsi() as? KtElement

        psi?.let {
            trace.bindingContext.get(InjektWritableSlices.IS_IMPLICIT, it)?.let {
                return it
            }
        }

        var isImplicit = descriptor.isMarkedAsImplicit()

        if (!isImplicit && descriptor is PropertyGetterDescriptor) {
            isImplicit = descriptor.correspondingProperty.isMarkedAsImplicit()
        }

        if (!isImplicit && descriptor is ConstructorDescriptor) {
            isImplicit = descriptor.constructedClass.isMarkedAsImplicit() ||
                    descriptor.constructedClass.constructors.any {
                        it.isMarkedAsImplicit()
                    }
        }

        if (!isImplicit && descriptor is ClassDescriptor) {
            isImplicit = descriptor.constructors.any { it.isMarkedAsImplicit() }
        }

        if (!isImplicit) {
            isImplicit = trace.bindingContext.get(
                InjektWritableSlices.IS_IMPLICIT,
                descriptor
            ) ?: false
        }

        psi?.let { trace.record(InjektWritableSlices.IS_IMPLICIT, it, isImplicit) }

        return isImplicit
    }

    fun isImplicit(
        trace: BindingTrace,
        element: KtElement,
        type: KotlinType?
    ): Boolean {
        trace.bindingContext.get(InjektWritableSlices.IS_IMPLICIT, element)?.let {
            return it
        }

        var isReader = false

        if (element is KtParameter) {
            isReader = element
                .typeReference
                ?.annotationEntries
                ?.mapNotNull { trace.bindingContext.get(BindingContext.ANNOTATION, it) }
                ?.any { it.fqName == InjektFqNames.Reader } ?: false
        }

        if (!isReader) {
            isReader = type?.hasAnnotation(InjektFqNames.Reader) ?: false
        }

        if (!isReader) {
            val parent = element.parent
            val annotations = when {
                element is KtNamedFunction -> element.annotationEntries
                parent is KtAnnotatedExpression -> parent.annotationEntries
                element is KtProperty -> element.annotationEntries
                element is KtParameter -> element.typeReference?.annotationEntries ?: emptyList()
                else -> emptyList()
            }

            isReader = annotations.any {
                trace.bindingContext.get(BindingContext.ANNOTATION, it)
                    ?.fqName == InjektFqNames.Reader
            }
        }

        trace.record(InjektWritableSlices.IS_IMPLICIT, element, isReader)

        return isReader
    }

    override fun checkType(
        expression: KtExpression,
        expressionType: KotlinType,
        expressionTypeWithSmartCast: KotlinType,
        c: ResolutionContext<*>
    ) {
        return
        if (expression is KtLambdaExpression) {
            val expectedType = c.expectedType
            if (expectedType === TypeUtils.NO_EXPECTED_TYPE) return
            if (expectedType === TypeUtils.UNIT_EXPECTED_TYPE) return
            val expectedIsReader =
                expectedType.hasAnnotation(InjektFqNames.Reader)
            val isReader =
                isImplicit(c.trace, expression, c.expectedType)

            if (expectedIsReader != isReader) {
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

            val expectedIsReader = expectedType.hasAnnotation(InjektFqNames.Reader)
            val isReader = expressionType.hasAnnotation(InjektFqNames.Reader)

            if (expectedIsReader != isReader) {
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
