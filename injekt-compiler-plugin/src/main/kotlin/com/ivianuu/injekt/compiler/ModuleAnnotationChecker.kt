package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.upperIfFlexible

open class ModuleAnnotationChecker : DeclarationChecker,
    AdditionalTypeChecker, StorageComponentContainerContributor {

    companion object {
        fun get(project: Project): ModuleAnnotationChecker {
            return StorageComponentContainerContributor.getInstances(project).single {
                it is ModuleAnnotationChecker
            } as ModuleAnnotationChecker
        }
    }

    enum class ModuleFunctionState { NO_MODULE, INFERRED, MARKED }

    fun analyze(trace: BindingTrace, descriptor: FunctionDescriptor): ModuleFunctionState {
        val psi = descriptor.findPsi() as? KtElement
        psi?.let {
            trace.bindingContext.get(InjektWritableSlices.MODULE_STATE, it)?.let { return it }
        }

        var moduleState = ModuleFunctionState.NO_MODULE
        if (trace.bindingContext.get(
                InjektWritableSlices.INFERRED_MODULE_DESCRIPTOR,
                descriptor
            ) == true
        ) {
            moduleState = ModuleFunctionState.MARKED
        } else {
            when (descriptor) {
                is AnonymousFunctionDescriptor -> {
                    if (descriptor.hasModuleAnnotation()) moduleState =
                        ModuleFunctionState.MARKED
                }
                else -> if (descriptor.hasModuleAnnotation()) moduleState =
                    ModuleFunctionState.MARKED
            }
        }
        (descriptor.findPsi() as? KtElement)?.let { element ->
            moduleState = analyzeFunctionContents(trace, element, moduleState)
        }
        psi?.let { trace.record(InjektWritableSlices.MODULE_STATE, it, moduleState) }
        return moduleState
    }

    private fun analyzeFunctionContents(
        trace: BindingTrace,
        element: KtElement,
        signatureModuleFunctionState: ModuleFunctionState
    ): ModuleFunctionState {
        var moduleState = signatureModuleFunctionState
        var localFcs = false
        var isInlineLambda = false
        element.accept(object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                if (function == element) {
                    super.visitNamedFunction(function)
                }
            }

            override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                val isInlineable = InlineUtil.isInlinedArgument(
                    lambdaExpression.functionLiteral,
                    trace.bindingContext,
                    true
                )
                if (isInlineable && lambdaExpression == element) isInlineLambda = true
                if (isInlineable || lambdaExpression == element)
                    super.visitLambdaExpression(lambdaExpression)
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                checkResolvedCall(expression.calleeExpression ?: expression)
                super.visitCallExpression(expression)
            }

            private fun checkResolvedCall(reportElement: KtExpression) {
                // Can be null in cases where the call isn't resolvable (eg. due to a bug/typo in the user's code)
                localFcs = true
                if (!isInlineLambda && moduleState != ModuleFunctionState.MARKED) {
                    /*// Report error on composable element to make it obvious which invocation is offensive
                    trace.report(
                        InjektErrors.MODULE_INVOCATION_IN_NON_MODULE
                            .on(reportElement)
                    )*/
                }
            }
        }, null)
        if (
            localFcs &&
            !isInlineLambda && moduleState != ModuleFunctionState.MARKED
        ) {
            val reportElement = when (element) {
                is KtNamedFunction -> element.nameIdentifier ?: element
                else -> element
            }
            if (localFcs) {
                /*trace.report(
                    InjektErrors.MODULE_INVOCATION_IN_NON_MODULE.on(reportElement)
                )*/
            }
        }
        if (localFcs && moduleState == ModuleFunctionState.NO_MODULE)
            moduleState =
                ModuleFunctionState.INFERRED
        return moduleState
    }

    /**
     * Analyze a KtElement
     *  - Determine if it is @Module (eg. the element or inferred type has an @Module annotation)
     *  - Update the binding context to cache analysis results
     *  - Report errors (eg. invocations of an @Module, etc)
     *  - Return true if element is @Module, else false
     */
    fun analyze(trace: BindingTrace, element: KtElement, type: KotlinType?): ModuleFunctionState {
        trace.bindingContext.get(InjektWritableSlices.MODULE_STATE, element)?.let { return it }

        var moduleState = ModuleFunctionState.NO_MODULE

        if (element is KtParameter) {
            val moduleAnnotation = element
                .typeReference
                ?.annotationEntries
                ?.mapNotNull { trace.bindingContext.get(BindingContext.ANNOTATION, it) }
                ?.singleOrNull { it.isModuleAnnotation }

            if (moduleAnnotation != null) {
                moduleState += ModuleFunctionState.MARKED
            }
        }

        if (
            type != null &&
            type !== TypeUtils.NO_EXPECTED_TYPE &&
            type.hasModuleAnnotation()
        ) {
            moduleState += ModuleFunctionState.MARKED
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
            if (descriptor.isModuleAnnotation) {
                moduleState += ModuleFunctionState.MARKED
            }
        }

        if (element is KtLambdaExpression || element is KtFunction) {
            moduleState = analyzeFunctionContents(trace, element, moduleState)
        }

        trace.record(InjektWritableSlices.MODULE_STATE, element, moduleState)
        return moduleState
    }

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (!platform.isJvm()) return
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
            val expectedModule = expectedType.hasModuleAnnotation()
            val moduleState = analyze(c.trace, expression, c.expectedType)
            if ((expectedModule && moduleState == ModuleFunctionState.NO_MODULE) ||
                (!expectedModule && moduleState == ModuleFunctionState.MARKED)
            ) {
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

            val expectedModule = expectedType.hasModuleAnnotation()
            val isModule = expressionType.hasModuleAnnotation()

            if (expectedModule != isModule) {
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

    operator fun ModuleFunctionState.plus(rhs: ModuleFunctionState): ModuleFunctionState =
        if (this > rhs) this else rhs
}