package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.resolution.CallableGivenNode
import com.ivianuu.injekt.compiler.resolution.GivenNode
import com.ivianuu.injekt.compiler.resolution.GivenRequest
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.getSubstitutionMap
import com.ivianuu.injekt.compiler.resolution.isAssignableTo
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Binding class GivenCallChecker(
    private val bindingTrace: BindingTrace,
    private val declarationStore: DeclarationStore,
    private val lazyTopDownAnalyzer: LazyTopDownAnalyzer,
) : KtTreeVisitorVoid() {

    private val chain = mutableListOf<GivenRequest>()

    private abstract inner class Scope(
        private val declaration: KtDeclaration?,
        private val parent: Scope?,
    ) {

        private val checkedRequests = mutableSetOf<GivenRequest>()

        private var resolved = false

        fun resolve(call: KtElement) {
            if (!resolved) {
                resolved = true
                if (declaration != null) {
                    try {
                        lazyTopDownAnalyzer.analyzeDeclarations(
                            TopDownAnalysisMode.TopLevelDeclarations,
                            listOfNotNull(declaration)
                        )
                    } catch (e: Throwable) {
                    }
                    try {
                        lazyTopDownAnalyzer.analyzeDeclarations(
                            TopDownAnalysisMode.LocalDeclarations,
                            listOfNotNull(declaration)
                        )
                    } catch (e: Throwable) {
                    }
                }
                try {
                    lazyTopDownAnalyzer.analyzeDeclarations(
                        TopDownAnalysisMode.TopLevelDeclarations,
                        listOfNotNull(call)
                    )
                } catch (e: Throwable) {
                }
                try {
                    lazyTopDownAnalyzer.analyzeDeclarations(
                        TopDownAnalysisMode.LocalDeclarations,
                        listOfNotNull(call)
                    )
                } catch (e: Throwable) {
                }
            }
        }

        fun check(call: ResolvedCall<*>, reportOn: PsiElement) {
            val resultingDescriptor = call.resultingDescriptor
            if (resultingDescriptor !is FunctionDescriptor) return

            val givenInfo = declarationStore.givenInfoFor(resultingDescriptor)

            call
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
                .forEach { check(it, reportOn) }
        }

        private fun check(node: GivenNode, reportOn: PsiElement) {
            node.dependencies
                .forEach { check(it, reportOn) }
        }

        private fun check(request: GivenRequest, reportOn: PsiElement) {
            if (request in checkedRequests) return
            checkedRequests += request

            chain.push(request)
            val givens = givensFor(request.type)
            when {
                givens.size == 1 -> check(givens.single(), reportOn)
                givens.size > 1 -> {
                    bindingTrace.report(
                        InjektErrors.MULTIPLE_GIVENS
                            .on(reportOn, request.type, request.origin, givens)
                    )
                }
                givens.isEmpty() && request.required -> {
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

        private fun givensFor(type: TypeRef): List<GivenNode> {
            val givens = givensForInThisScope(type)
                .map { callable ->
                    val info = declarationStore.givenInfoFor(callable)
                    val substitutionMap = getSubstitutionMap(
                        listOf(type to callable.returnType!!.toTypeRef())
                    )
                    CallableGivenNode(
                        type,
                        callable.valueParameters
                            .filter { it.name in info.allGivens }
                            .map {
                                GivenRequest(
                                    it.type.toTypeRef()
                                        .substitute(substitutionMap),
                                    it.name in info.requiredGivens,
                                    it.fqNameSafe
                                )
                            },
                        callable
                    )
                }
            return when {
                givens.isNotEmpty() -> return givens
                parent != null -> parent.givensFor(type)
                else -> emptyList()
            }
        }

        protected abstract fun givensForInThisScope(type: TypeRef): List<CallableDescriptor>
    }

    private inner class ExternalScope : Scope(null, null) {
        override fun givensForInThisScope(type: TypeRef): List<CallableDescriptor> =
            declarationStore.givensForType(type)
                .filter { it.isExternalDeclaration() }
                .filter { it.visibility == DescriptorVisibilities.PUBLIC }
    }

    private inner class InternalScope(parent: Scope?) : Scope(null, parent) {
        override fun givensForInThisScope(type: TypeRef): List<CallableDescriptor> =
            declarationStore.givensForType(type)
                .filterNot { it.isExternalDeclaration() }
    }

    private inner class ClassScope(
        private val declaration: KtClassOrObject,
        parent: Scope?,
    ) : Scope(declaration, parent) {
        private val allGivens by unsafeLazy {
            declaration.descriptor<ClassDescriptor>(bindingTrace.bindingContext)
                ?.extractGivensOfDeclaration(bindingTrace.bindingContext, declarationStore)
                ?: emptyList()
        }

        override fun givensForInThisScope(type: TypeRef): List<CallableDescriptor> =
            allGivens
                .filter { it.first.isAssignableTo(type) }
                .map { it.second }
    }

    private inner class FunctionScope(
        private val declaration: KtFunction,
        parent: Scope?,
    ) : Scope(declaration, parent) {
        private val allGivens by unsafeLazy {
            declaration.descriptor<FunctionDescriptor>(bindingTrace.bindingContext)
                ?.extractGivensOfCallable(declarationStore) ?: emptyList()
        }

        override fun givensForInThisScope(type: TypeRef): List<CallableDescriptor> =
            allGivens.filter { it.returnType!!.toTypeRef().isAssignableTo(type) }
    }

    private var scope: Scope = InternalScope(ExternalScope())

    private inline fun <R> inScope(scope: Scope, block: () -> R): R {
        val prevScope = this.scope
        this.scope = scope
        val result = block()
        this.scope = prevScope
        return result
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        inScope(ClassScope(declaration, scope)) {
            super.visitObjectDeclaration(declaration)
        }
    }

    override fun visitClass(klass: KtClass) {
        val parentScope = klass.companionObjects.singleOrNull()
            ?.let { ClassScope(it, scope) }
            ?: scope
        inScope(ClassScope(klass, parentScope)) {
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

    private fun visitFunction(function: KtFunction, block: () -> Unit) {
        inScope(FunctionScope(function, scope)) { block() }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        scope.resolve(expression)
        scope.check(expression.getResolvedCall(bindingTrace.bindingContext) ?: return, expression)
    }

}
