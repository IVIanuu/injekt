package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.descriptor
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.resolution.CallableGivenNode
import com.ivianuu.injekt.compiler.resolution.ClassResolutionScope
import com.ivianuu.injekt.compiler.resolution.ExternalResolutionScope
import com.ivianuu.injekt.compiler.resolution.FunctionResolutionScope
import com.ivianuu.injekt.compiler.resolution.GivenGraph
import com.ivianuu.injekt.compiler.resolution.GivenRequest
import com.ivianuu.injekt.compiler.resolution.InternalResolutionScope
import com.ivianuu.injekt.compiler.resolution.LocalDeclarationResolutionScope
import com.ivianuu.injekt.compiler.resolution.ResolutionScope
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.resolveGiven
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class GivenCallChecker(
    private val bindingTrace: BindingTrace,
    module: ModuleDescriptor,
) : KtTreeVisitorVoid() {

    private val declarationStore = DeclarationStore(module)

    private fun ResolutionScope.check(call: ResolvedCall<*>, reportOn: KtElement) {
        val resultingDescriptor = call.resultingDescriptor
        if (resultingDescriptor !is FunctionDescriptor) return

        val requests = call
            .valueArguments
            .filterKeys {
                it.hasAnnotation(InjektFqNames.Given) ||
                        it.type.hasAnnotation(InjektFqNames.Given)
            }
            .filter { it.value is DefaultValueArgument }
            .map {
                GivenRequest(
                    type = it.key.type.toTypeRef(),
                    required = !it.key.hasDefaultValueIgnoringGiven,
                    callableFqName = resultingDescriptor.fqNameSafe,
                    parameterName = it.key.name,
                    callContext = null
                )
            }

        if (requests.isEmpty()) return

        val graph = resolveGiven(requests)

        when (graph) {
            is GivenGraph.Success -> {
                graph.givens.values
                    .filterIsInstance<CallableGivenNode>()
                    .forEach { given ->
                        val lookedUpDeclaration = when (val callable = given.callable.callable) {
                            is ClassConstructorDescriptor -> callable.constructedClass
                            else -> callable
                        } as DeclarationDescriptor
                        when (val parent = lookedUpDeclaration.containingDeclaration) {
                            is ClassDescriptor -> parent.unsubstitutedMemberScope
                            is PackageFragmentDescriptor -> parent.getMemberScope()
                            else -> null
                        }?.recordLookup(given.callable.callable.name,
                            KotlinLookupLocation(reportOn))
                        bindingTrace.record(
                            InjektWritableSlices.USED_GIVEN,
                            given.callable.callable,
                            Unit
                        )
                    }
                bindingTrace.record(
                    InjektWritableSlices.GIVEN_GRAPH,
                    SourcePosition(
                        reportOn.containingKtFile.virtualFilePath,
                        reportOn.startOffset,
                        reportOn.endOffset
                    ),
                    graph
                )
            }
            is GivenGraph.Error -> bindingTrace.report(
                InjektErrors.UNRESOLVED_GIVEN
                    .on(reportOn, graph)
            )
        }
    }

    private var scope = ExternalResolutionScope(declarationStore)

    override fun visitFile(file: PsiFile) {
        inScope(
            {
                InternalResolutionScope(
                    scope,
                    declarationStore
                )
            }
        ) {
            super.visitFile(file)
        }
    }

    private inline fun inScope(scope: () -> ResolutionScope, block: () -> Unit) {
        val prevScope = this.scope
        try {
            this.scope = scope()
            block()
        } catch (e: Throwable) {
        } finally {
            this.scope = prevScope
        }
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        inScope({
            ClassResolutionScope(
                declarationStore,
                declaration.descriptor(bindingTrace.bindingContext) ?: return,
                scope
            )
        }) {
            super.visitObjectDeclaration(declaration)
        }
    }

    override fun visitClass(klass: KtClass) {
        val descriptor = klass.descriptor<ClassDescriptor>(bindingTrace.bindingContext) ?: return
        val parentScope = klass.companionObjects.singleOrNull()
            ?.let {
                ClassResolutionScope(
                    declarationStore,
                    it.descriptor(bindingTrace.bindingContext) ?: return,
                    scope
                )
            }
            ?: scope
        inScope({
            ClassResolutionScope(
                declarationStore,
                descriptor,
                parentScope
            )
        }) {
            super.visitClass(klass)
        }
        if (klass.isLocal) scope = LocalDeclarationResolutionScope(
            declarationStore,
            scope,
            descriptor
        )
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        visitFunction(constructor) { super.visitPrimaryConstructor(constructor) }
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        visitFunction(constructor) { super.visitSecondaryConstructor(constructor) }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        visitFunction(function) { super.visitNamedFunction(function) }
        if (function.isLocal) {
            val descriptor =
                function.descriptor<FunctionDescriptor>(bindingTrace.bindingContext) ?: return
            scope = LocalDeclarationResolutionScope(
                declarationStore,
                scope,
                descriptor
            )
        }
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        val type = lambdaExpression.getType(bindingTrace.bindingContext)
            ?.toTypeRef() ?: return
        visitFunction(lambdaExpression.functionLiteral, type) {
            super.visitLambdaExpression(lambdaExpression)
        }
    }

    private fun visitFunction(
        function: KtFunction,
        lambdaType: TypeRef? = null,
        block: () -> Unit,
    ) {
        inScope({
            FunctionResolutionScope(
                declarationStore,
                scope,
                function.descriptor(bindingTrace.bindingContext) ?: return,
                lambdaType
            )
        }) { block() }
    }

    override fun visitBlockExpression(expression: KtBlockExpression) {
        // capture the current scope here because
        // the scope might chage because of local declarations
        val current = scope
        inScope({ current }) { super.visitBlockExpression(expression) }
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (property.isLocal) {
            val descriptor = try {
                property.descriptor<VariableDescriptor>(bindingTrace.bindingContext)
            } catch (e: Throwable) {
                null
            } ?: return
            scope = LocalDeclarationResolutionScope(
                declarationStore,
                scope,
                descriptor
            )
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        try {
            scope.check(expression.getResolvedCall(bindingTrace.bindingContext) ?: return,
                expression)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

}
