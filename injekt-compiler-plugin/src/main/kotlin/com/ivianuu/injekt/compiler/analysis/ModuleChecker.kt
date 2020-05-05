package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.hasModuleAnnotation
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.isCallableReference
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.upperIfFlexible

class ModuleChecker : CallChecker, DeclarationChecker,
    AdditionalTypeChecker, StorageComponentContainerContributor {

    companion object {
        fun get(project: Project): ModuleChecker {
            return StorageComponentContainerContributor.getInstances(project).single {
                it is ModuleChecker
            } as ModuleChecker
        }
    }

    fun analyze(trace: BindingTrace, descriptor: FunctionDescriptor): Boolean {
        val psi = descriptor.findPsi() as? KtElement

        psi?.let {
            trace.bindingContext.get(InjektWritableSlices.IS_MODULE, it)?.let { return it }
        }

        var isModule = false
        if (descriptor.hasModuleAnnotation() ||
            (descriptor as? PropertyGetterDescriptor)?.correspondingProperty?.hasModuleAnnotation() == true
        ) isModule = true
        if (trace.bindingContext.get(InjektWritableSlices.IS_MODULE, descriptor) == true) isModule =
            true

        psi?.let {
            if (isModule) {
                if (descriptor.returnType != null && descriptor.returnType != descriptor.builtIns.unitType) {
                    trace.report(InjektErrors.RETURN_TYPE_NOT_ALLOWED_FOR_MODULE.on(psi))
                }
            }
        }

        psi?.let { trace.record(InjektWritableSlices.IS_MODULE, it, isModule) }
        return isModule
    }

    fun analyze(trace: BindingTrace, element: KtElement, type: KotlinType?): Boolean {
        trace.bindingContext.get(InjektWritableSlices.IS_MODULE, element)?.let { return it }

        var isModule = false

        if (element is KtParameter) {
            val moduleAnnotation = element
                .typeReference
                ?.annotationEntries
                ?.mapNotNull { trace.bindingContext.get(BindingContext.ANNOTATION, it) }
                ?.singleOrNull { it.fqName == InjektFqNames.Module }

            if (moduleAnnotation != null) {
                isModule = true
            }
        }

        if (
            type != null &&
            type !== TypeUtils.NO_EXPECTED_TYPE &&
            type.hasModuleAnnotation()
        ) {
            isModule = true
        }
        val parent = element.parent
        val annotations = when {
            element is KtNamedFunction -> element.annotationEntries
            parent is KtAnnotatedExpression -> parent.annotationEntries
            element is KtProperty -> element.annotationEntries
            element is KtParameter -> element.typeReference?.annotationEntries ?: emptyList()
            else -> emptyList()
        }

        for (entry in annotations) {
            val descriptor = trace.bindingContext.get(BindingContext.ANNOTATION, entry) ?: continue
            if (descriptor.fqName == InjektFqNames.Module) {
                isModule = true
            }
        }

        trace.record(InjektWritableSlices.IS_MODULE, element, isModule)
        return isModule
    }

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        container.useInstance(this)
    }

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor is FunctionDescriptor) {
            analyze(context.trace, descriptor)
        }
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
            val expectedIsModule = expectedType.hasModuleAnnotation()
            val isModule = analyze(c.trace, expression, c.expectedType)
            if (expectedIsModule != isModule) {
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

            val expectedIsModule = expectedType.hasModuleAnnotation()
            val isModule = expressionType.hasModuleAnnotation()

            if (expectedIsModule != isModule) {
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

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val descriptor = resolvedCall.candidateDescriptor
        if (descriptor !is FunctionDescriptor) return

        if (analyze(context.trace, descriptor)) {
            checkModuleInvocations(resolvedCall, reportOn, context)
        }
    }

    private fun checkModuleInvocations(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val enclosingModuleFunction = findEnclosingModuleFunctionContext(context) {
            analyze(context.trace, it) || it.annotations.hasAnnotation(InjektFqNames.Factory) ||
                    it.annotations.hasAnnotation(InjektFqNames.ChildFactory)
        }

        when {
            enclosingModuleFunction != null -> {
                var isConditional = false

                var walker: PsiElement? = resolvedCall.call.callElement
                while (walker != null) {
                    val parent = walker.parent
                    if (parent is KtIfExpression ||
                        parent is KtForExpression ||
                        parent is KtWhenExpression ||
                        parent is KtTryExpression ||
                        parent is KtCatchClause ||
                        parent is KtWhileExpression
                    ) {
                        isConditional = true
                    }
                    walker = try {
                        walker.parent
                    } catch (e: Throwable) {
                        null
                    }
                }

                if (isConditional) {
                    context.trace.report(
                        InjektErrors.CONDITIONAL_NOT_ALLOWED_IN_MODULE.on(reportOn)
                    )
                }


                if (context.scope.parentsWithSelf.any {
                        it.isScopeForDefaultParameterValuesOf(
                            enclosingModuleFunction
                        )
                    }) {
                    context.trace.report(
                        Errors.UNSUPPORTED.on(
                            reportOn,
                            "@Module function calls in a context of default parameter value"
                        )
                    )
                }
            }
            resolvedCall.call.isCallableReference() -> {
                // do nothing: we can get callable reference to suspend function outside suspend context
            }
            else -> {
                context.trace.report(
                    InjektErrors.FORBIDDEN_MODULE_INVOCATION.on(reportOn)
                )
            }
        }
    }
}
