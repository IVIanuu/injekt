package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.BlockResolutionScope
import com.ivianuu.injekt.compiler.resolution.CallableGivenNode
import com.ivianuu.injekt.compiler.resolution.ClassResolutionScope
import com.ivianuu.injekt.compiler.resolution.ExternalResolutionScope
import com.ivianuu.injekt.compiler.resolution.FunctionResolutionScope
import com.ivianuu.injekt.compiler.resolution.GivenGraph
import com.ivianuu.injekt.compiler.resolution.GivenNode
import com.ivianuu.injekt.compiler.resolution.GivenRequest
import com.ivianuu.injekt.compiler.resolution.InternalResolutionScope
import com.ivianuu.injekt.compiler.resolution.ResolutionScope
import com.ivianuu.injekt.compiler.resolution.resolveGivenCandidates
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
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
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class GivenCallChecker(
    private val bindingTrace: BindingTrace,
    private val declarationStore: DeclarationStore,
) : KtTreeVisitorVoid() {

    private val chain = mutableListOf<GivenRequest>()

    private fun ResolutionScope.check(call: ResolvedCall<*>, reportOn: KtElement) {
        val resultingDescriptor = call.resultingDescriptor
        if (resultingDescriptor !is FunctionDescriptor) return

        val givenInfo = declarationStore.givenInfoFor(resultingDescriptor)

        val givenRequests = call
            .valueArguments
            .filterKeys { it.name in givenInfo.allGivens }
            .filter { it.value is DefaultValueArgument }
            .map {
                GivenRequest(
                    it.key.type.toTypeRef(),
                    it.key.name in givenInfo.requiredGivens,
                    it.key.fqNameSafe
                )
            }
            .onEach { check(it, reportOn) }

        if (givenRequests.isEmpty()) return

        if (givenRequests.all { givensByRequest[it] != null }) {
            bindingTrace.record(
                InjektWritableSlices.GIVEN_GRAPH,
                SourcePosition(
                    reportOn.containingKtFile.virtualFilePath,
                    reportOn.startOffset,
                    reportOn.endOffset
                ),
                GivenGraph(givensByRequest)
            )
        }

        if (call.resultingDescriptor.fqNameSafe == InjektFqNames.debugGiven) {
            val type = call.typeArguments.values.single().toTypeRef()
            val given = givensByRequest.toList().firstOrNull { it.first.type == type }
                ?.second
            if (given != null) {
                bindingTrace.report(
                    InjektErrors.DEBUG_GIVEN
                        .on(reportOn, given to givensByRequest)
                )
            }
        }
    }

    private fun ResolutionScope.check(node: GivenNode, reportOn: KtElement) {
        node.dependencies
            .forEach { check(it, reportOn) }
    }

    private fun ResolutionScope.check(request: GivenRequest, reportOn: KtElement) {
        if (request in chain) {
            val cycleChain = chain.subList(
                chain.indexOf(request), chain.size
            )

            val cycleOriginRequest = chain[chain.indexOf(request) - 1]

            bindingTrace.report(
                InjektErrors.CIRCULAR_DEPENDENCY
                    .on(reportOn, cycleChain.reversed() + cycleOriginRequest),
            )
            return
        }

        chain.push(request)
        val candidates = resolveGivenCandidates(declarationStore, request)

        when {
            candidates.size == 1 -> {
                val given = candidates.single()
                givensByRequest[request] = given
                if (given is CallableGivenNode) {
                    val lookedUpDeclaration = when (val callable = given.callable) {
                        is ClassConstructorDescriptor -> callable.constructedClass
                        else -> callable
                    } as DeclarationDescriptor
                    when (val parent = lookedUpDeclaration.containingDeclaration) {
                        is ClassDescriptor -> parent.unsubstitutedMemberScope
                        is PackageFragmentDescriptor -> parent.getMemberScope()
                        else -> null
                    }?.recordLookup(given.callable.name, KotlinLookupLocation(reportOn))
                }
                check(given, reportOn)
            }
            candidates.size > 1 -> {
                bindingTrace.report(
                    InjektErrors.MULTIPLE_GIVENS
                        .on(reportOn, request.type, request.origin, candidates)
                )
            }
            candidates.isEmpty() && request.required -> {
                bindingTrace.report(
                    InjektErrors.UNRESOLVED_GIVEN
                        .on(
                            reportOn,
                            request.type,
                            request.origin,
                            chain
                        )
                )
            }
        }
        chain.pop()
    }

    private var scope = InternalResolutionScope(
        ExternalResolutionScope(declarationStore),
        declarationStore
    )

    private inline fun <R> inScope(scope: ResolutionScope, block: () -> R): R {
        val prevScope = this.scope
        this.scope = scope
        val result = block()
        this.scope = prevScope
        return result
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        inScope(ClassResolutionScope(
            bindingTrace.bindingContext,
            declarationStore,
            declaration.descriptor(bindingTrace.bindingContext) ?: return,
            scope
        )) {
            super.visitObjectDeclaration(declaration)
        }
    }

    override fun visitClass(klass: KtClass) {
        val parentScope = klass.companionObjects.singleOrNull()
            ?.let {
                ClassResolutionScope(
                    bindingTrace.bindingContext,
                    declarationStore,
                    it.descriptor(bindingTrace.bindingContext) ?: return,
                    scope
                )
            }
            ?: scope
        inScope(ClassResolutionScope(
            bindingTrace.bindingContext,
            declarationStore,
            klass.descriptor(bindingTrace.bindingContext) ?: return,
            parentScope
        )) {
            super.visitClass(klass)
        }
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        visitFunction(constructor) { super.visitPrimaryConstructor(constructor) }
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        visitFunction(constructor) { super.visitSecondaryConstructor(constructor) }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        visitFunction(function) { super.visitNamedFunction(function) }
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        visitFunction(lambdaExpression.functionLiteral) {
            super.visitLambdaExpression(lambdaExpression)
        }
    }

    private fun visitFunction(function: KtFunction, block: () -> Unit) {
        inScope(FunctionResolutionScope(
            declarationStore,
            scope,
            function.descriptor(bindingTrace.bindingContext) ?: return
        )) { block() }
    }

    private var blockScope: ResolutionScope? = null
    override fun visitBlockExpression(expression: KtBlockExpression) {
        val prevScope = blockScope
        val scope = BlockResolutionScope(declarationStore, scope)
        this.blockScope = scope
        inScope(scope) {
            super.visitBlockExpression(expression)
        }
        blockScope = prevScope
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        val descriptor = property.descriptor<VariableDescriptor>(bindingTrace.bindingContext)
            ?: return
        blockScope?.addIfNeeded(descriptor)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        scope.check(expression.getResolvedCall(bindingTrace.bindingContext) ?: return, expression)
    }

}
