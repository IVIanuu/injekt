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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Binding class GivenCallChecker(
    private val bindingContext: BindingContext,
    private val bindingTrace: BindingTrace,
    private val declarationStore: DeclarationStore,
) : Generator {

    private abstract inner class Scope(private val parent: Scope?) {

        private val checkedRequests = mutableSetOf<GivenRequest>()

        private val chain = mutableListOf<GivenRequest>()

        fun check(call: KtCallExpression) {
            val resolvedCall = call.getResolvedCall(bindingContext)
                ?: return

            val resultingDescriptor = resolvedCall.resultingDescriptor
            if (resultingDescriptor !is FunctionDescriptor) return

            val givenInfo = declarationStore.givenInfoForCallable(resultingDescriptor)

            resolvedCall
                .valueArguments
                .filterKeys { parameter ->
                    parameter.name in givenInfo.requiredGivens ||
                            parameter.name in givenInfo.givensWithDefault
                }
                .filter { it.value is DefaultValueArgument }
                .map {
                    GivenRequest(
                        it.key.type.toTypeRef(),
                        it.key.name in givenInfo.requiredGivens,
                        it.key.fqNameSafe
                    )
                }
                .forEach { check(it, call) }
        }

        private fun check(node: GivenNode, call: KtCallExpression) {
            node.dependencies
                .forEach { check(it, call) }
        }

        private fun check(request: GivenRequest, call: KtCallExpression) {
            if (request in checkedRequests) return
            checkedRequests += request

            chain.push(request)
            val givens = givensFor(request.type)
            when {
                givens.size == 1 -> check(givens.single(), call)
                givens.size > 1 -> {
                    bindingTrace.report(
                        InjektErrors.MULTIPLE_GIVENS
                            .on(call, request.type, givens)
                    )
                }
                givens.isEmpty() && request.required -> {
                    bindingTrace.report(
                        InjektErrors.UNRESOLVED_GIVEN
                            .on(
                                call,
                                request.type
                            )
                    )
                }
            }
            chain.pop()
        }

        private fun givensFor(type: TypeRef): List<GivenNode> {
            val givens = givensForInThisScope(type)
                .map { callable ->
                    val info = declarationStore.givenInfoForCallable(callable)
                    val substitutionMap = getSubstitutionMap(
                        listOf(type to callable.returnType!!.toTypeRef())
                    )
                    CallableGivenNode(
                        type,
                        callable.valueParameters
                            .filter { it.type.hasAnnotation(InjektFqNames.Given) }
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

    private inner class ExternalScope : Scope(null) {
        override fun givensForInThisScope(type: TypeRef): List<CallableDescriptor> =
            declarationStore.givensForType(type)
                .filter { it.isExternalDeclaration() }
                .filter { it.visibility == DescriptorVisibilities.PUBLIC }
    }

    private inner class InternalScope(parent: Scope?) : Scope(parent) {
        override fun givensForInThisScope(type: TypeRef): List<CallableDescriptor> =
            declarationStore.givensForType(type)
                .filterNot { it.isExternalDeclaration() }
    }

    private inner class ClassScope(
        private val descriptor: ClassDescriptor,
        parent: Scope?,
    ) : Scope(parent) {
        private val allGivens = descriptor.extractGivensOfDeclaration(bindingContext)
        override fun givensForInThisScope(type: TypeRef): List<CallableDescriptor> =
            allGivens
                .filter { it.first.isAssignableTo(type) }
                .map { it.second }
    }

    private inner class FunctionScope(
        private val descriptor: FunctionDescriptor,
        parent: Scope?,
    ) : Scope(parent) {
        private val allGivens = descriptor.extractGivensOfCallable()
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

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(object : KtTreeVisitorVoid() {
                override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
                    inScope(
                        ClassScope(
                            declaration.descriptor(bindingContext) ?: return, scope
                        )
                    ) {
                        super.visitObjectDeclaration(declaration)
                    }
                }

                override fun visitClass(klass: KtClass) {
                    val descriptor = klass.descriptor<ClassDescriptor>(bindingContext) ?: return
                    val parentScope = descriptor.companionObjectDescriptor
                        ?.let { ClassScope(it, scope) }
                        ?: scope
                    inScope(ClassScope(descriptor, parentScope)) {
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
                    val descriptor =
                        function.descriptor<FunctionDescriptor>(bindingContext) ?: return
                    inScope(FunctionScope(descriptor, scope)) {
                        block()
                    }
                }

                override fun visitCallExpression(expression: KtCallExpression) {
                    super.visitCallExpression(expression)
                    scope.check(expression)
                }
            })
        }
    }

}
