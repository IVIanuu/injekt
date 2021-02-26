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
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
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

class GivenCallTransformer(private val pluginContext: IrPluginContext) : IrElementTransformerVoid() {

    private class GraphContext(val graph: GivenGraph.Success) {
        val statements = mutableListOf<IrStatement>()
        private val scopeContexts = mutableMapOf<ResolutionScope, ScopeContext>()

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

    private class ScopeContext(
        val graphContext: GraphContext,
        val scope: ResolutionScope,
        val symbol: IrSymbol
    ) {
        val irScope = Scope(symbol)
        val givensByRequest: Map<GivenRequest, GivenNode> =
            (scope.parent
                ?.let { graphContext.existingScopeContextOrNull(it)?.givensByRequest }
                ?: emptyMap()) + (graphContext.graph.givensByScope[scope] ?: emptyMap())
        val statements =
            if (graphContext.graph.scope == scope) graphContext.statements else mutableListOf()
        val initializingExpressions = mutableMapOf<GivenNode, GivenExpression?>()
        val functionExpressions = mutableMapOf<GivenNode, () -> IrExpression>()
        val lambdasByProviderGiven: MutableMap<ProviderGivenNode, IrFunction> = scope.parent
            ?.let { graphContext.existingScopeContextOrNull(it)?.lambdasByProviderGiven }
            ?: mutableMapOf()
    }

    private fun ScopeContext.fillGivens(
        callable: CallableRef,
        call: IrFunctionAccessExpression,
    ) {
        val requests = callable.getGivenRequests(scope.declarationStore)
        if (callable.callable.dispatchReceiverParameter != null && call.dispatchReceiver == null) {
            call.dispatchReceiver = expressionFor(
                requests.singleOrNull { it.parameterName.asString() == "_dispatchReceiver" }
                    ?: error("Wtf ${requests.joinToString("\n")}")
            )
        }

        if (callable.callable.extensionReceiverParameter != null && call.extensionReceiver == null) {
            call.extensionReceiver = expressionFor(
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
                parameter to expressionFor(
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

            val rawExpression = when (given) {
                is CallableGivenNode -> callableExpression(given)
                is DefaultGivenNode -> null
                is ProviderGivenNode -> providerExpression(given)
                is SetGivenNode -> setExpression(given)
            }
                ?.let { intercepted(it, given) }
                ?.let { wrapInFunctionIfNeeded(it, given) }

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

    private fun ScopeContext.expressionFor(request: GivenRequest): IrExpression? {
        val given = givensByRequest[request] ?: error("Wtf $request")
        val scopeContext = graphContext.existingScopeContext(given.ownerScope)
        scopeContext.initializingExpressions[given]?.run { return get() }
        val expression = GivenExpression(given)
        scopeContext.initializingExpressions[given] = expression
        val irExpression = expression.run { get() }
        scopeContext.initializingExpressions -= given
        return irExpression
    }

    private fun ScopeContext.wrapInFunctionIfNeeded(
        expression: IrExpression,
        given: GivenNode
    ): IrExpression {
        return expression
        return if (given.dependencies.isEmpty()) expression
        else functionExpressions.getOrPut(given) {
            val function = IrFactoryImpl.buildFun {
                origin = IrDeclarationOrigin.DEFINED
                name = Name.special("<anonymous>")
                returnType = given.type.toIrType(pluginContext)
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
                statements += this
                this.body = DeclarationIrBuilder(pluginContext, symbol).run {
                    irBlockBody {
                        +irReturn(expression)
                    }
                }
            }

            val functionCallExpression: () -> IrExpression = {
                DeclarationIrBuilder(
                    pluginContext,
                    symbol
                ).irCall(function)
            }
            functionCallExpression
        }()
    }

    private fun ScopeContext.intercepted(
        unintercepted: IrExpression,
        given: GivenNode
    ): IrExpression {
        if (given.interceptors.isEmpty()) return unintercepted
        val providerType = given.callContext
            .providerType(graphContext.graph.scope.declarationStore)
            .typeWith(listOf(given.type))
        return given.interceptors
            .reversed()
            .fold(unintercepted) { acc: IrExpression, interceptor: InterceptorNode ->
                callableExpression(
                    interceptor.callable
                        .toGivenNode(interceptor.callable.type, given.ownerScope, given.ownerScope)
                ).apply {
                    this as IrFunctionAccessExpression
                    interceptor.callable.callable.valueParameters
                        .single { interceptor.callable.parameterTypes[it.injektName()] == providerType }
                        .index
                        .let { factoryIndex ->
                            putValueArgument(
                                factoryIndex,
                                DeclarationIrBuilder(
                                    pluginContext,
                                    symbol
                                ).irLambda(providerType.toIrType(pluginContext)) { acc }
                            )
                        }
                }
            }
    }

    private fun ScopeContext.objectExpression(type: TypeRef): IrExpression =
        DeclarationIrBuilder(pluginContext, symbol)
            .irGetObject(pluginContext.referenceClass(type.classifier.fqName)!!)

    private fun ScopeContext.providerExpression(given: ProviderGivenNode): IrExpression {
        return DeclarationIrBuilder(pluginContext, symbol)
            .irLambda(given.type.toIrType(pluginContext)) { function ->
                val dependencyScopeContext = graphContext.existingScopeContextOrNull(given.dependencyScope)
                    ?: graphContext.createScopeContext(given.dependencyScope, function.symbol)
                dependencyScopeContext.lambdasByProviderGiven[given] = function
                val expression = with(dependencyScopeContext) {
                    expressionFor(given.dependencies.single())
                        ?: DeclarationIrBuilder(pluginContext, symbol).irUnit()
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

    private fun ScopeContext.setExpression(given: SetGivenNode): IrExpression {
        val elementType = given.type.fullyExpandedType.arguments.single()

        if (given.elements.isEmpty()) {
            val emptySet = pluginContext.referenceFunctions(
                FqName("kotlin.collections.emptySet")
            ).single()
            return DeclarationIrBuilder(pluginContext, symbol)
                .irCall(emptySet)
                .apply { putTypeArgument(0, elementType.toIrType(pluginContext)) }
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
                    .apply { putTypeArgument(0, elementType.toIrType(pluginContext)) }
            )

            given.elements
                .forEach {
                    +irCall(setAddAll).apply {
                        dispatchReceiver = irGet(tmpSet)
                        putValueArgument(
                            0,
                            callableExpression(
                                it.toGivenNode(elementType, this@setExpression.scope, this@setExpression.scope)
                            )
                        )
                    }
                }

            +irGet(tmpSet)
        }
    }

    private fun ScopeContext.callableExpression(given: CallableGivenNode): IrExpression {
        return when (given.callable.callable) {
            is ClassConstructorDescriptor -> classExpression(
                given,
                given.type,
                given.callable,
                given.callable.callable
            )
            is PropertyDescriptor -> propertyExpression(
                given.type,
                given,
                given.callable,
                given.callable.callable
            )
            is FunctionDescriptor -> functionExpression(
                given.callable,
                given,
                given.callable.callable
            )
            is ReceiverParameterDescriptor -> if (given.type.classifier.isObject) objectExpression(given.type)
            else parameterExpression(given.callable.callable)
            is ValueParameterDescriptor -> parameterExpression(given.callable.callable)
            is VariableDescriptor -> variableExpression(given.callable.callable)
            else -> error("Unsupported callable $given")
        }
    }

    private fun ScopeContext.classExpression(
        given: GivenNode,
        type: TypeRef,
        callable: CallableRef,
        descriptor: ClassConstructorDescriptor
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
                    val substitutionMap = getSubstitutionMap(scope.declarationStore, listOf(type to callable.originalType))
                    callable.typeParameters
                        .map {
                            substitutionMap[it]
                                ?: error("No substitution found for $it")
                        }
                        .forEachIndexed { index, typeArgument ->
                            putTypeArgument(index, typeArgument.toIrType(pluginContext))
                        }

                    fillGivens(callable, this)
                }
        }
    }

    private fun ScopeContext.propertyExpression(
        type: TypeRef,
        given: GivenNode,
        callable: CallableRef,
        descriptor: PropertyDescriptor
    ): IrExpression {
        val property = pluginContext.symbolTable.referenceProperty(descriptor.original).bind()
        val getter = property.owner.getter!!
        return DeclarationIrBuilder(pluginContext, symbol)
            .irCall(getter.symbol)
            .apply {
                callable.typeParameters
                    .map {
                        callable.typeArguments[it]
                            ?: error("No substitution found for $it")
                    }
                    .forEachIndexed { index, typeArgument ->
                        putTypeArgument(index, typeArgument.toIrType(pluginContext))
                    }

                fillGivens(callable, this)
            }
    }

    private fun ScopeContext.functionExpression(
        callable: CallableRef,
        given: GivenNode,
        descriptor: FunctionDescriptor
    ): IrExpression {
        val function = descriptor.irFunction()
        return DeclarationIrBuilder(pluginContext, symbol)
            .irCall(function.symbol)
            .apply {
                callable.typeParameters
                    .map {
                        callable.typeArguments[it]
                            ?: error("No substitution found for $it")
                    }
                    .forEachIndexed { index, typeArgument ->
                        putTypeArgument(index, typeArgument.toIrType(pluginContext))
                    }

                fillGivens(callable, this)
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
                .fillGivens(
                    result.symbol.descriptor
                        .toCallableRef(graph.scope.declarationStore)
                        .substitute(substitutionMap),
                    result
                )
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
