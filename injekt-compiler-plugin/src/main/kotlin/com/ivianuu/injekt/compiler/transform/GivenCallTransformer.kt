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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.cfg.index
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class GivenCallTransformer(
    private val declarationStore: DeclarationStore,
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    private inner class GraphContext(val graph: GivenGraph.Success) {
        val statements = mutableListOf<IrStatement>()
        val functionExpressions = mutableMapOf<GivenKey, WrappedExpression>()
        private val scopeContexts = mutableMapOf<ResolutionScope, ScopeContext>()

        var parameterIndex = 0

        val givensByRequestsByScope = mutableMapOf<ResolutionScope, Map<GivenRequest, GivenNode>>()

        private val graphContextParents = buildList<ResolutionScope> {
            var current: ResolutionScope? = graph.scope.parent
            while (current != null) {
                this += current
                current = current.parent
            }
        }

        private fun ResolutionScope.mapScopeIfNeeded() =
            if (this in graphContextParents) graph.scope else this

        fun existingScopeContext(scope: ResolutionScope): ScopeContext =
            scopeContexts[scope.mapScopeIfNeeded()] ?: error("No existing scope context found for $scope")

        fun existingScopeContextOrNull(scope: ResolutionScope): ScopeContext? =
            scopeContexts[scope.mapScopeIfNeeded()]

        fun createScopeContext(scope: ResolutionScope, symbol: IrSymbol): ScopeContext {
            check(scopeContexts[scope.mapScopeIfNeeded()] == null) {
                "Cannot create scope context twice"
            }
            return scopeContexts.getOrPut(scope.mapScopeIfNeeded()) {
                ScopeContext(this, scope.mapScopeIfNeeded(), symbol)
            }.also {
                check(it.symbol === symbol)
            }
        }
    }

    private fun ResolutionScope.getGivensByRequest(
        graphContext: GraphContext
    ): Map<GivenRequest, GivenNode> = graphContext.givensByRequestsByScope.getOrPut(this) {
        (parent?.getGivensByRequest(graphContext) ?: emptyMap()) +
                (graphContext.graph.givensByScope[this] ?: emptyMap())
    }

    private inner class ScopeContext(
        val graphContext: GraphContext,
        val scope: ResolutionScope,
        val symbol: IrSymbol
    ) {
        val irScope = Scope(symbol)
        val givensByRequest: Map<GivenRequest, GivenNode> = scope.getGivensByRequest(graphContext)
        val statements =
            if (graphContext.graph.scope == scope) graphContext.statements else mutableListOf()
        val initializingExpressions = mutableMapOf<GivenNode, GivenExpression?>()
        val lambdasByProviderGiven: MutableMap<ProviderGivenNode, IrFunction> = scope.parent
            ?.let { graphContext.existingScopeContextOrNull(it)?.lambdasByProviderGiven }
            ?: mutableMapOf()
        val scopeExpressionProvider: (GivenRequest) -> IrExpression? = provider@ { request ->
            val given = givensByRequest[request]
                ?: error("Wtf $request")
            val scopeContext = graphContext.existingScopeContext(given.ownerScope)
            scopeContext.initializingExpressions[given]?.run { return@provider get() }
            val expression = GivenExpression(given)
            scopeContext.initializingExpressions[given] = expression
            val irExpression = expression.run { get() }
            scopeContext.initializingExpressions -= given
            irExpression
        }
    }

    private fun ScopeContext.fillGivens(
        callable: CallableRef,
        requests: List<GivenRequest>,
        call: IrFunctionAccessExpression,
        expressionProvider: (GivenRequest) -> IrExpression?
    ) {
        if (callable.callable.dispatchReceiverParameter != null && call.dispatchReceiver == null) {
            call.dispatchReceiver = expressionProvider(
                requests.singleOrNull { it.parameterName.asString() == "_dispatchReceiver" }
                    ?: error("Wtf ${requests.joinToString("\n")}")
            )
        }

        if (callable.callable.extensionReceiverParameter != null && call.extensionReceiver == null) {
            call.extensionReceiver = expressionProvider(
                requests.singleOrNull { it.parameterName.asString() == "_extensionReceiver" }
                    ?: error("Wtf ${requests.joinToString("\n")}")
            )
        }
        callable.callable
            .valueParameters
            .filter { call.getValueArgument(it.index) == null }
            .filter {
                it.contributionKind(scope.declarationStore) == ContributionKind.VALUE ||
                        callable.parameterTypes[it.injektName()]!!.contributionKind == ContributionKind.VALUE
            }
            .map { parameter ->
                val parameterName = parameter.injektName()
                parameter to expressionProvider(
                    requests.singleOrNull { it.parameterName.asString() == parameterName }
                        ?: error("Wtf $parameterName -> ${requests.joinToString("\n")}")
                )
            }
            .forEach { call.putValueArgument(it.first.index, it.second) }
    }

    private inner class GivenExpression(private val given: GivenNode) {
        private var block: IrBlock? = null
        private var tmpVariable: IrVariable? = null
        private var finalExpression: IrExpression? = null

        private var initializing = false

        fun ScopeContext.get(): IrExpression? {
            if (initializing) {
                if (block == null) {
                    block = DeclarationIrBuilder(pluginContext, symbol)
                        .irBlock { tmpVariable = irTemporary(irNull(), isMutable = true) } as IrBlock
                }
                return DeclarationIrBuilder(pluginContext, symbol)
                    .irGet(tmpVariable!!)
            }

            finalExpression?.let { return it }

            initializing = true

            val rawExpression = wrapInFunctionIfNeeded(given) { expressionProvider ->
                when (given) {
                    is CallableGivenNode -> callableExpression(given, expressionProvider)
                    is DefaultGivenNode -> null
                    is ProviderGivenNode -> providerExpression(given, expressionProvider)
                    is SetGivenNode -> setExpression(given, expressionProvider)
                }
            }

            initializing = false

            finalExpression = if (block == null) rawExpression else {
                with(DeclarationIrBuilder(pluginContext, symbol)) {
                    block!!.statements += irSet(tmpVariable!!.symbol, rawExpression!!)
                    block!!.statements += irGet(tmpVariable!!)
                }
                block!!
            }

            return finalExpression
        }
    }

    private data class GivenKey(
        val type: TypeRef,
        val uniqueKey: Any,
        val dependencies: List<GivenRequest>
    ) {
        constructor(given: GivenNode) : this(given.type, given.uniqueKey, given.dependencies)
    }

    private class WrappedExpression(
        val unstableDependencies: List<GivenRequest>,
        val function: IrFunction
    )

    private fun GivenNode.canFunctionWrap(): Boolean = this !is ProviderGivenNode &&
            dependencies.isNotEmpty() &&
            !hasCircularDependency &&
            dependencies.all { it.required }

    private fun ScopeContext.wrapInFunctionIfNeeded(
        given: GivenNode,
        expression: ((GivenRequest) -> IrExpression?) -> IrExpression?
    ): IrExpression? {
        if (!given.canFunctionWrap()) return expression(scopeExpressionProvider)

        val key = GivenKey(given)

        val wrappedExpression = graphContext.functionExpressions.getOrPut(key) {
            val usages = graphContext.graph.givensByScope
                .values
                .flatMap { it.values }
                .filter { it.canFunctionWrap() }
                .filter { GivenKey(it) == key }

            if (usages.size == 1) return expression(scopeExpressionProvider)

            fun ResolutionScope.allScopes(): List<ResolutionScope> =
                (parent?.allScopes() ?: emptyList()) + this

            val hostingScope = given.requestingScope.allScopes()
                .last { candidateScope ->
                    usages
                        .all { usage ->
                            candidateScope in usage.requestingScope.allScopes()
                        }
                }
            val hostingScopeContext = graphContext.existingScopeContext(hostingScope)

            val mergedDependencies = given.dependencies
                .indices
                .map { parameterIndex ->
                    usages
                        .map { usage ->
                            val usageParameterRequest = usage.dependencies[parameterIndex]
                            val givensByRequest = usage.requestingScope.getGivensByRequest(graphContext)
                            usage.dependencies[parameterIndex] to (givensByRequest[usageParameterRequest]
                                ?: error("Wtf"))
                        }
                        .distinctBy { GivenKey(it.second) }
                }

            fun GivenNode.ensureAllInScope(scope: ResolutionScope): Boolean {
                val allGivensByRequest = scope.getGivensByRequest(graphContext)
                return dependencies
                    .map { allGivensByRequest[it] }
                    .all {
                        it != null && it.ensureAllInScope(scope)
                    }
            }

            val (stableDependencies, unstableDependencies) = mergedDependencies
                .partition {
                    val singleDependencyNode = it.singleOrNull() ?: return@partition false
                    singleDependencyNode.second.ensureAllInScope(hostingScope)
                }
                .let {
                    it.first
                        .map { it.first().first } to it.second
                        .map { it.first().first }
                }

            val function = IrFactoryImpl.buildFun {
                origin = IrDeclarationOrigin.DEFINED
                name = Name.special("<anonymous>")
                returnType = given.type.toIrType(pluginContext, declarationStore)
                visibility = DescriptorVisibilities.LOCAL
                isSuspend = given.callContext == CallContext.SUSPEND
            }.apply {
                parent = irScope.getLocalDeclarationParent()
                if (given.callContext == CallContext.COMPOSABLE) {
                    annotations += DeclarationIrBuilder(pluginContext, symbol)
                        .irCallConstructor(
                            pluginContext.referenceConstructors(InjektFqNames.Composable)
                                .single(),
                            emptyList()
                        )
                }
                val valueParametersByDependency = unstableDependencies
                    .associateWith { request ->
                        addValueParameter(
                            request.parameterName.asString(),
                            request.type.toIrType(pluginContext, declarationStore)
                        )
                    }

                this.body = DeclarationIrBuilder(pluginContext, symbol).run {
                    irBlockBody {
                        +irReturn(
                            expression { request ->
                                when (request) {
                                    in stableDependencies -> {
                                        scopeExpressionProvider(request)
                                    }
                                    in unstableDependencies -> {
                                        valueParametersByDependency[request]
                                            ?.let { irGet(it) }
                                            ?: error("Wtf $request")
                                    }
                                    else -> {
                                        error("Unexpected request $request")
                                    }
                                }
                            } ?: error("Wtf")
                        )
                    }
                }

                hostingScopeContext.statements += this
            }

            WrappedExpression(unstableDependencies, function)
        }

        return DeclarationIrBuilder(
            pluginContext,
            symbol
        ).irCall(wrappedExpression.function).apply {
            wrappedExpression.unstableDependencies
                .forEachIndexed { index, dependency ->
                    putValueArgument(index, scopeExpressionProvider(dependency))
                }
        }
    }

    private fun ScopeContext.objectExpression(type: TypeRef): IrExpression =
        DeclarationIrBuilder(pluginContext, symbol)
            .irGetObject(pluginContext.referenceClass(type.classifier.fqName)!!)

    private fun ScopeContext.providerExpression(
        given: ProviderGivenNode,
        expressionProvider: (GivenRequest) -> IrExpression?
    ): IrExpression {
        return DeclarationIrBuilder(pluginContext, symbol)
            .irLambda(
                given.type.toIrType(pluginContext, declarationStore),
                parameterNameProvider = {
                    "p${graphContext.parameterIndex++}"
                }
            ) { function ->
                val dependencyScopeContext = graphContext.existingScopeContextOrNull(given.dependencyScope)
                    ?: graphContext.createScopeContext(given.dependencyScope, function.symbol)
                dependencyScopeContext.lambdasByProviderGiven[given] = function
                val expression = with(dependencyScopeContext) {
                    scopeExpressionProvider(given.dependencies.single())
                        ?: error("Wtf")
                }
                if (dependencyScopeContext.statements.isEmpty()) expression
                else {
                    irBlock {
                        dependencyScopeContext.statements.forEach { +it }
                        +expression
                    }
                }
            }
    }

    private fun ScopeContext.setExpression(
        given: SetGivenNode,
        expressionProvider: (GivenRequest) -> IrExpression?
    ): IrExpression {
        val elementType = given.type.fullyExpandedType.arguments.single()

        if (given.dependencies.isEmpty()) {
            val emptySet = pluginContext.referenceFunctions(
                FqName("kotlin.collections.emptySet")
            ).single()
            return DeclarationIrBuilder(pluginContext, symbol)
                .irCall(emptySet)
                .apply { putTypeArgument(0, elementType.toIrType(pluginContext, declarationStore)) }
        }

        return DeclarationIrBuilder(pluginContext, symbol).irBlock {
            val mutableSetOf = pluginContext.referenceFunctions(
                FqName("kotlin.collections.mutableSetOf")
            ).single { it.owner.valueParameters.isEmpty() }

            val setAddAll = mutableSetOf.owner.returnType
                .classOrNull!!
                .owner
                .functions
                .single { it.name.asString() == "add" }

            val tmpSet = irTemporary(
                irCall(mutableSetOf)
                    .apply { putTypeArgument(0, elementType.toIrType(pluginContext, declarationStore)) }
            )

            given.dependencies
                .forEach {
                    +irCall(setAddAll).apply {
                        dispatchReceiver = irGet(tmpSet)
                        putValueArgument(0, expressionProvider(it))
                    }
                }

            +irGet(tmpSet)
        }
    }

    private fun ScopeContext.callableExpression(
        given: CallableGivenNode,
        expressionProvider: (GivenRequest) -> IrExpression?
    ): IrExpression {
        return when (given.callable.callable) {
            is ClassConstructorDescriptor -> classExpression(
                given,
                given.callable.callable,
                expressionProvider
            )
            is PropertyDescriptor -> propertyExpression(
                given,
                given.callable.callable,
                expressionProvider
            )
            is FunctionDescriptor -> functionExpression(
                given,
                given.callable.callable,
                expressionProvider
            )
            is ReceiverParameterDescriptor -> if (given.type.classifier.isObject) objectExpression(given.type)
            else parameterExpression(given.callable.callable)
            is ValueParameterDescriptor -> parameterExpression(given.callable.callable)
            is VariableDescriptor -> variableExpression(given.callable.callable)
            else -> error("Unsupported callable $given")
        }
    }

    private fun ScopeContext.classExpression(
        given: CallableGivenNode,
        descriptor: ClassConstructorDescriptor,
        expressionProvider: (GivenRequest) -> IrExpression?
    ): IrExpression {
        return if (descriptor.constructedClass.kind == ClassKind.OBJECT) {
            val clazz =
                pluginContext.symbolTable.referenceClass(descriptor.constructedClass.original)
                    .bind()
            DeclarationIrBuilder(pluginContext, symbol)
                .irGetObject(clazz)
        } else {
            val constructor =
                pluginContext.symbolTable.referenceConstructor(descriptor.original).bind()
                    .owner
            DeclarationIrBuilder(pluginContext, symbol)
                .irCall(constructor.symbol)
                .apply {
                    fillTypeParameters(given.callable)
                    fillGivens(given.callable, given.dependencies, this, expressionProvider)
                }
        }
    }

    private fun ScopeContext.propertyExpression(
        given: CallableGivenNode,
        descriptor: PropertyDescriptor,
        expressionProvider: (GivenRequest) -> IrExpression?
    ): IrExpression {
        val property = pluginContext.symbolTable.referenceProperty(descriptor.original).bind()
        val getter = property.owner.getter!!
        return DeclarationIrBuilder(pluginContext, symbol)
            .irCall(getter.symbol)
            .apply {
                fillTypeParameters(given.callable)
                fillGivens(given.callable, given.dependencies, this, expressionProvider)
            }
    }

    private fun ScopeContext.functionExpression(
        given: CallableGivenNode,
        descriptor: FunctionDescriptor,
        expressionProvider: (GivenRequest) -> IrExpression?
    ): IrExpression {
        val function = descriptor.irFunction()
        return DeclarationIrBuilder(pluginContext, symbol)
            .irCall(function.symbol)
            .apply {
                fillTypeParameters(given.callable)
                fillGivens(given.callable, given.dependencies, this, expressionProvider)
            }
    }

    private fun ScopeContext.parameterExpression(descriptor: ParameterDescriptor): IrExpression {
        if (descriptor is ProviderGivenNode.ProviderParameterDescriptor) {
            val parameter = lambdasByProviderGiven[descriptor.given]?.valueParameters?.get(descriptor.index)
                ?: error("Wtf ${descriptor.given} -> $lambdasByProviderGiven")
            return DeclarationIrBuilder(pluginContext, symbol).irGet(parameter)
        }

        return when (val containingDeclaration = descriptor.containingDeclaration) {
            is ClassDescriptor -> receiverAccessors.last {
                descriptor.type.constructor.declarationDescriptor == it.first.descriptor
            }.second()
            is ClassConstructorDescriptor -> DeclarationIrBuilder(pluginContext, symbol)
                .irGet(
                    containingDeclaration.irConstructor()
                        .allParameters
                        .single { it.name == descriptor.name }
                )
            is FunctionDescriptor -> DeclarationIrBuilder(pluginContext, symbol)
                .irGet(
                    containingDeclaration.irFunction()
                        .let { function ->
                            function.allParameters
                                .filter { it != function.dispatchReceiverParameter }
                        }
                        .single { it.index == descriptor.index() }
                )
            else -> error("Unexpected parent $descriptor $containingDeclaration")
        }
    }

    private fun IrFunctionAccessExpression.fillTypeParameters(callable: CallableRef) {
        callable
            .typeArguments
            .values
            .forEachIndexed { index, typeArgument ->
                putTypeArgument(index, typeArgument.toIrType(pluginContext, declarationStore))
            }
    }

    private fun ScopeContext.variableExpression(descriptor: VariableDescriptor): IrExpression {
        return DeclarationIrBuilder(pluginContext, symbol)
            .irGet(variables.single { it.descriptor == descriptor })
    }

    private fun ClassConstructorDescriptor.irConstructor() =
        pluginContext.symbolTable.referenceConstructor(original).bind().owner

    private fun FunctionDescriptor.irFunction() =
        pluginContext.symbolTable.referenceSimpleFunction(original)
            .bind()
            .owner

    private fun <T : IrBindableSymbol<*, *>> T.bind(): T {
        (pluginContext as IrPluginContextImpl).linker.run {
            getDeclaration(this@bind)
            postProcess()
        }
        return this
    }

    private val receiverAccessors = mutableListOf<Pair<IrClass, () -> IrExpression>>()

    private val variables = mutableListOf<IrVariable>()

    private val fileStack = mutableListOf<IrFile>()
    override fun visitFile(declaration: IrFile): IrFile {
        fileStack.push(declaration)
        return super.visitFile(declaration)
            .also { fileStack.pop() }
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        receiverAccessors.push(
            declaration to {
                DeclarationIrBuilder(pluginContext, declaration.symbol)
                    .irGet(declaration.thisReceiver!!)
            }
        )
        val result = super.visitClass(declaration)
        receiverAccessors.pop()
        return result
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val dispatchReceiver = declaration.dispatchReceiverParameter?.type?.classOrNull?.owner
        if (dispatchReceiver != null) {
            receiverAccessors.push(
                dispatchReceiver to {
                    DeclarationIrBuilder(pluginContext, declaration.symbol)
                        .irGet(declaration.dispatchReceiverParameter!!)
                }
            )
        }
        val extensionReceiver = declaration.extensionReceiverParameter?.type?.classOrNull?.owner
        if (extensionReceiver != null) {
            receiverAccessors.push(
                extensionReceiver to {
                    DeclarationIrBuilder(pluginContext, declaration.symbol)
                        .irGet(declaration.extensionReceiverParameter!!)
                }
            )
        }
        val result = super.visitFunction(declaration)
        if (dispatchReceiver != null) receiverAccessors.pop()
        if (extensionReceiver != null) receiverAccessors.pop()
        return result
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return super.visitVariable(declaration)
            .also { variables += declaration }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
        val graph = pluginContext.bindingContext[
                InjektWritableSlices.GIVEN_GRAPH,
                SourcePosition(
                    fileStack.last().fileEntry.name,
                    result.startOffset,
                    result.endOffset
                )
        ] ?: return result
        val substitutionMap = getSubstitutionMap(
            graph.scope.declarationStore,
            (0 until expression.typeArgumentsCount)
                .map { result.getTypeArgument(it)!!.toKotlinType().toTypeRef(graph.scope.declarationStore) }
                .zip(
                    expression.symbol.descriptor.typeParameters
                        .map { it.toClassifierRef(graph.scope.declarationStore).defaultType }
                )
        ) + getSubstitutionMap(
            graph.scope.declarationStore,
            ((result.dispatchReceiver?.type as? IrSimpleType)?.arguments
                ?.map { it.typeOrNull!!.toKotlinType().toTypeRef(graph.scope.declarationStore) }
                ?: emptyList())
                .zip(
                    result.dispatchReceiver?.type?.classOrNull?.owner?.let {
                        it.typeParameters
                            .map { it.defaultType.toKotlinType().toTypeRef(graph.scope.declarationStore) }
                    } ?: emptyList()
                )
        )

        val graphContext = GraphContext(graph)
        try {
            graphContext
                .createScopeContext(graph.scope, result.symbol)
                .run {
                    fillGivens(
                        result.symbol.descriptor
                            .toCallableRef(graph.scope.declarationStore)
                            .substitute(substitutionMap),
                        graph.requests,
                        result,
                        scopeExpressionProvider
                    )
                }
        } catch (e: Throwable) {
            throw RuntimeException("Wtf ${expression.dump()}", e)
        }

        return if (graphContext.statements.isEmpty()) {
            result
        } else {
            DeclarationIrBuilder(pluginContext, result.symbol)
                .irBlock {
                    graphContext.statements.forEach { +it }
                    +result
                }
        }
    }

}
