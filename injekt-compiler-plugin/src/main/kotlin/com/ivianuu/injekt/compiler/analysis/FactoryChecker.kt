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
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isCallableReference
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.typeUtil.supertypes

class FactoryChecker : CallChecker, DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is FunctionDescriptor ||
            (!descriptor.hasAnnotation(InjektFqNames.Factory) &&
                    !descriptor.hasAnnotation(InjektFqNames.ChildFactory) &&
                    !descriptor.hasAnnotation(InjektFqNames.CompositionFactory) &&
                    !descriptor.hasAnnotation(InjektFqNames.InstanceFactory))
        ) return

        val parentMemberScope = (descriptor.containingDeclaration as? ClassDescriptor)
            ?.unsubstitutedMemberScope
            ?: (descriptor.containingDeclaration as? PackageFragmentDescriptor)
                ?.getMemberScope()

        if ((parentMemberScope?.getContributedDescriptors()
                ?.filterIsInstance<FunctionDescriptor>()
                ?.filter { it.name == descriptor.name }
                ?.size ?: 0) > 1
        ) {
            context.trace.report(
                InjektErrors.MULTIPLE_DECLARATIONS_WITH_SAME_NAME
                    .on(declaration)
            )
        }

        checkFactoriesLastStatementIsCreate(
            declaration as KtFunction,
            descriptor,
            context
        )

        val annotationsSize = descriptor.annotations
            .filter {
                it.fqName == InjektFqNames.ChildFactory ||
                        it.fqName == InjektFqNames.Factory ||
                        it.fqName == InjektFqNames.CompositionFactory ||
                        it.fqName == InjektFqNames.InstanceFactory ||
                        it.fqName == InjektFqNames.Module
            }
            .size

        if (annotationsSize > 1) {
            context.trace.report(InjektErrors.EITHER_MODULE_OR_FACTORY.on(declaration))
        }

        if (descriptor.isSuspend) {
            context.trace.report(
                InjektErrors.CANNOT_BE_SUSPEND
                    .on(declaration)
            )
        }

        if (descriptor.isTailrec) {
            context.trace.report(
                InjektErrors.CANNOT_HAVE_TAILREC_MODIFIER
                    .on(declaration)
            )
        }

        if (descriptor.isInline) {
            context.trace.report(
                InjektErrors.FACTORY_CANNOT_BE_INLINE
                    .on(descriptor.findPsi() ?: declaration)
            )
        }
        if (descriptor.typeParameters.isNotEmpty()) {
            context.trace.report(
                InjektErrors.FACTORY_WITH_TYPE_PARAMETERS
                    .on(descriptor.findPsi() ?: declaration)
            )
        }

        if (descriptor.hasAnnotation(InjektFqNames.CompositionFactory) &&
            descriptor.returnType?.constructor?.declarationDescriptor?.annotations
                ?.hasAnnotation(InjektFqNames.CompositionComponent) != true
        ) {
            context.trace.report(
                InjektErrors.NOT_A_COMPOSITION_COMPONENT
                    .on(declaration)
            )
        }
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val resultingDescriptor = resolvedCall.resultingDescriptor

        if (resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.composition.parent") {
            if (resolvedCall.typeArguments.values.single()
                    .constructor.declarationDescriptor?.annotations
                    ?.hasAnnotation(InjektFqNames.CompositionComponent) != true
            ) {
                context.trace.report(
                    InjektErrors.NOT_A_COMPOSITION_COMPONENT
                        .on(reportOn)
                )
            }

            val enclosingCompositionFactory = findDirectEnclosingFunctionContext(context) {
                it.hasAnnotation(InjektFqNames.CompositionFactory)
            }

            if (enclosingCompositionFactory == null) {
                context.trace.report(
                    InjektErrors.PARENT_CALL_WITHOUT_COMPOSITION_FACTORY
                        .on(reportOn)
                )
            }
        }

        when (resultingDescriptor.fqNameSafe.asString()) {
            "com.ivianuu.injekt.childFactory" -> {
                val referencedFunction = resolvedCall
                    .valueArgumentsByIndex!!.singleOrNull()
                    ?.arguments
                    ?.single()
                    ?.getArgumentExpression()
                    ?.let { it as? KtCallableReferenceExpression }
                    ?.callableReference
                    ?.getResolvedCall(context.trace.bindingContext)
                    ?.resultingDescriptor

                if (referencedFunction?.annotations?.hasAnnotation(InjektFqNames.ChildFactory) != true) {
                    context.trace.report(InjektErrors.NOT_A_CHILD_FACTORY.on(reportOn))
                }
            }
            "com.ivianuu.injekt.create" -> {
                checkCreateInvocation(resolvedCall, reportOn, context)
                val enclosingFactory = findDirectEnclosingFunctionContext(context) {
                    it.hasAnnotation(InjektFqNames.Factory)
                }
                if (enclosingFactory != null) {
                    val type =
                        resolvedCall.typeArguments.values.singleOrNull()

                    type?.constructor?.declarationDescriptor?.let {
                        if (it is ClassDescriptor) checkImplType(it, context, reportOn)
                    }
                }
            }
        }

        if (resultingDescriptor is FunctionDescriptor &&
            resultingDescriptor.hasAnnotation(InjektFqNames.Factory) &&
            resultingDescriptor.isInline
        ) {
            resolvedCall.getReturnType().constructor.declarationDescriptor?.let {
                if (it is ClassDescriptor) checkImplType(it, context, reportOn)
            }
        }

        if ((resultingDescriptor.hasAnnotation(InjektFqNames.ChildFactory) ||
                    resultingDescriptor.hasAnnotation(InjektFqNames.CompositionFactory)) &&
            !resolvedCall.call.isCallableReference()
        ) {
            context.trace.report(
                InjektErrors.CANNOT_INVOKE_CHILD_OR_COMPOSITION_FACTORIES.on(
                    reportOn
                )
            )
        }
    }

    private fun checkFactoriesLastStatementIsCreate(
        element: KtFunction,
        descriptor: FunctionDescriptor,
        context: DeclarationCheckerContext
    ) {
        fun reportLastStatementMustBeCreate() {
            context.trace.report(InjektErrors.LAST_STATEMENT_MUST_BE_CREATE.on(element))
        }

        val statements = element.bodyBlockExpression?.statements
            ?: listOfNotNull(element.bodyExpression)

        val returnedExpression = when (val lastStatement = statements.lastOrNull()) {
            is KtReturnExpression -> lastStatement.returnedExpression
            is KtCallExpression -> lastStatement
            else -> null
        }

        if (returnedExpression !is KtCallExpression) {
            reportLastStatementMustBeCreate()
            return
        }

        val resolvedCall = returnedExpression.getResolvedCall(context.trace.bindingContext)
        if (resolvedCall == null) {
            reportLastStatementMustBeCreate()
            return
        }

        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() != "com.ivianuu.injekt.create") {
            reportLastStatementMustBeCreate()
        }
    }

    private fun checkCreateInvocation(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val enclosingModuleFunction = findDirectEnclosingFunctionContext(context) {
            it.hasAnnotation(InjektFqNames.Factory) ||
                    it.hasAnnotation(InjektFqNames.ChildFactory) ||
                    it.hasAnnotation(InjektFqNames.CompositionFactory) ||
                    it.hasAnnotation(InjektFqNames.InstanceFactory)
        }

        when {
            enclosingModuleFunction != null -> {
                if (context.scope.parentsWithSelf.any {
                        it.isScopeForDefaultParameterValuesOf(
                            enclosingModuleFunction
                        )
                    }) {
                    context.trace.report(
                        Errors.UNSUPPORTED.on(
                            reportOn,
                            "createImpl function calls in a context of default parameter value"
                        )
                    )
                }
            }
            resolvedCall.call.isCallableReference() -> {
                // do nothing: we can get callable reference to suspend function outside suspend context
            }
            else -> {
                context.trace.report(
                    InjektErrors.CREATE_WITHOUT_FACTORY.on(reportOn)
                )
            }
        }
    }

    private fun checkImplType(
        clazz: ClassDescriptor,
        context: CallCheckerContext,
        reportOn: PsiElement
    ) {
        if (clazz.modality != Modality.ABSTRACT) {
            context.trace.report(
                InjektErrors.IMPL_FACTORY_RETURN_TYPE_MUST_BE_ABSTRACT
                    .on(reportOn)
            )
        }

        if (clazz.kind == ClassKind.CLASS && clazz.constructors.none { it.valueParameters.isEmpty() }) {
            context.trace.report(
                InjektErrors.IMPL_SUPER_TYPE_MUST_HAVE_EMPTY_CONSTRUCTOR
                    .on(reportOn)
            )
        }

        clazz.forEachDeclarationInThisAndSuperTypes { declaration ->
            when (declaration) {
                is FunctionDescriptor -> {
                    if (declaration.typeParameters.isNotEmpty()) {
                        context.trace.report(
                            InjektErrors.PROVISION_FUNCTION_CANNOT_HAVE_TYPE_PARAMETERS
                                .on(reportOn)
                        )
                    }
                    if (declaration.valueParameters.isNotEmpty()) {
                        context.trace.report(
                            InjektErrors.PROVISION_FUNCTION_CANNOT_HAVE_VALUE_PARAMETERS
                                .on(reportOn)
                        )
                    }
                    if (declaration.isSuspend) {
                        context.trace.report(
                            InjektErrors.PROVISION_FUNCTION_CANNOT_BE_SUSPEND
                                .on(reportOn)
                        )
                    }
                }
                is PropertyDescriptor -> {
                    if (declaration.isVar) {
                        context.trace.report(
                            InjektErrors.PROVISION_PROPERTY_CANNOT_BE_VAR
                                .on(reportOn)
                        )
                    }
                }
            }
        }
    }

    private fun ClassDescriptor.forEachDeclarationInThisAndSuperTypes(block: (DeclarationDescriptor) -> Unit) {
        unsubstitutedMemberScope
            .getContributedDescriptors()
            .filter {
                it !is CallableMemberDescriptor ||
                        it.dispatchReceiverParameter?.type != builtIns.anyType
            }
            .forEach(block)
        defaultType.supertypes()
            .mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }
            .forEach { it.forEachDeclarationInThisAndSuperTypes(block) }
    }
}
