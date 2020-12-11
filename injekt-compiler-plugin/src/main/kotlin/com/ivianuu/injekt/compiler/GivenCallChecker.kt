package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.CallableGivenNode
import com.ivianuu.injekt.compiler.resolution.GivenNode
import com.ivianuu.injekt.compiler.resolution.GivenRequest
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.isAssignableTo
import com.ivianuu.injekt.compiler.resolution.resolveGivens
import com.ivianuu.injekt.compiler.resolution.toGivenNode
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
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

    private abstract inner class Scope(private val parent: Scope?) {

        fun check(call: ResolvedCall<*>, reportOn: KtElement) {
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

        private fun check(node: GivenNode, reportOn: KtElement) {
            node.dependencies
                .forEach { check(it, reportOn) }
        }

        private fun check(request: GivenRequest, reportOn: KtElement) {
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
            val givens = resolveGivens(
                declarationStore,
                request,
                this,
                { givensForInThisScope(it) to parent },
                { givenCollectionElementsForInThisScope(it) to parent }
            )
            when {
                givens.size == 1 -> {
                    val given = givens.single()
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

        protected abstract fun givensForInThisScope(type: TypeRef): List<GivenNode>

        protected abstract fun givenCollectionElementsForInThisScope(type: TypeRef): List<CallableDescriptor>
    }

    private inner class ExternalScope : Scope(null) {
        override fun givensForInThisScope(type: TypeRef): List<GivenNode> =
            declarationStore.givensForType(type)
                .filter { it.isExternalDeclaration() }
                .filter { it.visibility == DescriptorVisibilities.PUBLIC }
                .map { it.toGivenNode(type, declarationStore) }

        override fun givenCollectionElementsForInThisScope(type: TypeRef): List<CallableDescriptor> =
            declarationStore.givenCollectionElementsFor(type)
                .filter { it.isExternalDeclaration() }
    }

    private inner class InternalScope(parent: Scope?) : Scope(parent) {
        override fun givensForInThisScope(type: TypeRef): List<GivenNode> =
            declarationStore.givensForType(type)
                .filterNot { it.isExternalDeclaration() }
                .map { it.toGivenNode(type, declarationStore) }

        override fun givenCollectionElementsForInThisScope(type: TypeRef): List<CallableDescriptor> =
            declarationStore.givenCollectionElementsFor(type)
                .filterNot { it.isExternalDeclaration() }
    }

    private inner class ClassScope(
        private val declaration: KtClassOrObject,
        parent: Scope?,
    ) : Scope(parent) {
        private val allGivens by unsafeLazy {
            declaration.descriptor<ClassDescriptor>(bindingTrace.bindingContext)
                ?.extractGivensOfDeclaration(bindingTrace.bindingContext, declarationStore)
                ?: emptyList()
        }
        private val allGivenCollectionElements by unsafeLazy {
            declaration.descriptor<ClassDescriptor>(bindingTrace.bindingContext)
                ?.extractGivenCollectionElementsOfDeclaration(bindingTrace.bindingContext)
                ?: emptyList()
        }

        override fun givensForInThisScope(type: TypeRef): List<GivenNode> =
            allGivens
                .filter { it.first.isAssignableTo(type) }
                .map { it.second }
                .map { it.toGivenNode(type, declarationStore) }

        override fun givenCollectionElementsForInThisScope(type: TypeRef): List<CallableDescriptor> =
            allGivenCollectionElements.filter { it.first.isAssignableTo(type) }
                .map { it.second }
    }

    private inner class FunctionScope(
        private val declaration: KtFunction,
        parent: Scope?,
    ) : Scope(parent) {
        private val allGivens by unsafeLazy {
            declaration.descriptor<FunctionDescriptor>(bindingTrace.bindingContext)
                ?.extractGivensOfCallable(declarationStore) ?: emptyList()
        }
        private val allGivenCollectionElements by unsafeLazy {
            declaration.descriptor<FunctionDescriptor>(bindingTrace.bindingContext)
                ?.extractGivenCollectionElementsOfCallable()
                ?: emptyList()
        }

        override fun givensForInThisScope(type: TypeRef): List<GivenNode> =
            allGivens.filter { it.returnType!!.toTypeRef().isAssignableTo(type) }
                .map { it.toGivenNode(type, declarationStore) }

        override fun givenCollectionElementsForInThisScope(type: TypeRef): List<CallableDescriptor> =
            allGivenCollectionElements.filter { it.returnType!!.toTypeRef().isAssignableTo(type) }
    }

    private inner class LambdaScope(
        private val descriptor: FunctionDescriptor,
        parent: Scope?,
    ) : Scope(parent) {
        private val allGivens by unsafeLazy {
            descriptor.extractGivensOfCallable(declarationStore)
        }
        private val allGivenCollectionElements by unsafeLazy {
            descriptor.extractGivenCollectionElementsOfCallable()
        }

        override fun givensForInThisScope(type: TypeRef): List<GivenNode> =
            allGivens.filter { it.returnType!!.toTypeRef().isAssignableTo(type) }
                .map { it.toGivenNode(type, declarationStore) }

        override fun givenCollectionElementsForInThisScope(type: TypeRef): List<CallableDescriptor> =
            allGivenCollectionElements.filter { it.returnType!!.toTypeRef().isAssignableTo(type) }
    }

    private inner class BlockScope(parent: Scope?) : Scope(parent) {
        private val givenVariables = mutableListOf<VariableDescriptor>()
        private val givenCollectionElementsVariables = mutableListOf<VariableDescriptor>()
        fun pushVariable(variable: VariableDescriptor) {
            if (variable.hasAnnotation(InjektFqNames.Given)) givenVariables += variable
            else if (variable.hasAnnotation(InjektFqNames.GivenMap) ||
                variable.hasAnnotation(InjektFqNames.GivenSet)
            ) givenVariables += variable
        }

        override fun givensForInThisScope(type: TypeRef): List<GivenNode> = givenVariables
            .filter { it.type.toTypeRef().isAssignableTo(type) }
            .map { it.toGivenNode(type, declarationStore) }

        override fun givenCollectionElementsForInThisScope(type: TypeRef): List<CallableDescriptor> =
            givenCollectionElementsVariables
                .filter { it.type.toTypeRef().isAssignableTo(type) }
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

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        val function = bindingTrace[BindingContext.FUNCTION, lambdaExpression.functionLiteral]
            ?: return
        inScope(LambdaScope(function, scope)) {
            super.visitLambdaExpression(lambdaExpression)
        }
    }

    private var blockScope: BlockScope? = null
    override fun visitBlockExpression(expression: KtBlockExpression) {
        val prevScope = blockScope
        val scope = BlockScope(scope)
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
        blockScope?.pushVariable(descriptor)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        scope.check(expression.getResolvedCall(bindingTrace.bindingContext) ?: return, expression)
    }

}
