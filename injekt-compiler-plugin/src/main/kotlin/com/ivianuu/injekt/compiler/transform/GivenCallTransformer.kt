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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.forEachWith
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
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
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addDispatchReceiver
import org.jetbrains.kotlin.ir.builders.declarations.addExtensionReceiver
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class GivenCallTransformer(
    private val context: InjektContext,
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {
    private inner class GraphContext(val graph: GivenGraph.Success) {
        val statements = mutableListOf<IrStatement>()

        var variableIndex = 0

        private val graphContextParents = buildList<ResolutionScope> {
            var current: ResolutionScope? = graph.scope.parent
            while (current != null) {
                this += current
                current = current.parent
            }
        }

        private val isInBetweenCircularDependency = mutableMapOf<ResolutionResult.Success.WithCandidate.Value, Boolean>()
        fun isInBetweenCircularDependency(result: ResolutionResult.Success.WithCandidate.Value): Boolean = isInBetweenCircularDependency.getOrPut(result) {
            val allResults = mutableListOf<ResolutionResult.Success.WithCandidate.Value>()
            var isInBetweenCircularDependency = false
            fun ResolutionResult.Success.WithCandidate.visit() {
                if (this is ResolutionResult.Success.WithCandidate.CircularDependency &&
                        allResults.none { it.candidate == candidate }) {
                    isInBetweenCircularDependency = true
                    return
                }
                if (this is ResolutionResult.Success.WithCandidate.Value) {
                    allResults += this
                    dependencyResults.forEach { (_, dependencyResult) ->
                        if (!isInBetweenCircularDependency &&
                            dependencyResult is ResolutionResult.Success.WithCandidate)
                                dependencyResult.visit()
                    }
                }
            }
            result.dependencyResults.forEach {
                if (!isInBetweenCircularDependency)
                    it.value.safeAs<ResolutionResult.Success.WithCandidate>()?.visit()
            }
            isInBetweenCircularDependency
        }

        fun mapScopeIfNeeded(scope: ResolutionScope) =
            if (scope in graphContextParents) graph.scope else scope
    }

    private inner class ScopeContext(
        val parent: ScopeContext?,
        val graphContext: GraphContext,
        val scope: ResolutionScope,
        val irScope: Scope
    ) {
        val symbol = irScope.scopeOwnerSymbol
        val functionWrappedExpressions = mutableMapOf<TypeRef, ScopeContext.() -> IrExpression>()
        val cachedExpressions = mutableMapOf<TypeRef, ScopeContext.() -> IrExpression>()
        val statements = if (scope == graphContext.graph.scope) graphContext.statements else mutableListOf()
        val initializingExpressions: MutableMap<GivenNode, GivenExpression> =
            parent?.initializingExpressions ?: mutableMapOf()
        val parameterMap: MutableMap<ParameterDescriptor, IrValueParameter> =
            parent?.parameterMap ?: mutableMapOf()

        fun findScopeContext(scopeToFind: ResolutionScope): ScopeContext {
            val finalScope = graphContext.mapScopeIfNeeded(scopeToFind)
            if (finalScope == scope) return this@ScopeContext
            return parent?.findScopeContext(finalScope)
                ?: error("wtf")
        }

        fun expressionFor(result: ResolutionResult.Success.WithCandidate): IrExpression {
            val scopeContext = findScopeContext(result.scope)
            return scopeContext.expressionForImpl(result)
        }

        private fun expressionForImpl(result: ResolutionResult.Success.WithCandidate): IrExpression {
            initializingExpressions[result.candidate]?.run { return get() }
            val expression = GivenExpression(result)
            initializingExpressions[result.candidate] = expression
            val irExpression = expression.run { get() }
            initializingExpressions -= result.candidate
            return irExpression
        }
    }

    private fun IrFunctionAccessExpression.fillGivens(
        context: ScopeContext,
        results: Map<GivenRequest, ResolutionResult.Success>
    ) {
        results
            .forEach { (request, result) ->
                if (result !is ResolutionResult.Success.WithCandidate) return@forEach
                val expression = context.expressionFor(result)
                when(request.parameterName.asString()) {
                    "_dispatchReceiver" -> dispatchReceiver = expression
                    "_extensionReceiver" -> extensionReceiver = expression
                    else -> {
                        putValueArgument(
                            symbol.owner
                                .valueParameters
                                .first { it.name == request.parameterName }
                                .index,
                            expression
                        )
                    }
                }
            }
    }

    private inner class GivenExpression(private val result: ResolutionResult.Success.WithCandidate) {
        private var block: IrBlock? = null
        private var tmpVariable: IrVariable? = null
        private var finalExpression: IrExpression? = null

        private var initializing = false

        fun ScopeContext.get(): IrExpression {
            if (initializing) {
                if (block == null) {
                    val resultType = result.candidate.type.toIrType(pluginContext, localClasses, context)
                    block = DeclarationIrBuilder(pluginContext, symbol)
                        .irBlock(resultType = resultType) {
                            tmpVariable = irTemporary(
                                value = irNull(),
                                isMutable = true,
                                irType = resultType.makeNullable(),
                                nameHint = "${graphContext.variableIndex++}"
                            )
                        } as IrBlock
                }
                return DeclarationIrBuilder(pluginContext, symbol)
                    .irGet(tmpVariable!!)
            }

            result as ResolutionResult.Success.WithCandidate.Value

            finalExpression?.let { return it }

            initializing = true

            val rawExpression = cacheExpressionIfNeeded(result) {
                wrapExpressionInFunctionIfNeeded(result) {
                    when (result.candidate) {
                        is AbstractGivenNode -> abstractGivenExpression(result, result.candidate.cast())
                        is CallableGivenNode -> callableExpression(result, result.candidate.cast())
                        is ProviderGivenNode -> providerExpression(result, result.candidate.cast())
                        is SetGivenNode -> setExpression(result, result.candidate.cast())
                    }
                }
            }

            initializing = false

            finalExpression = if (block == null) rawExpression else {
                with(DeclarationIrBuilder(pluginContext, symbol)) {
                    block!!.statements += irSet(tmpVariable!!.symbol, rawExpression)
                    block!!.statements += irGet(tmpVariable!!)
                }
                block!!
            }

            return finalExpression!!
        }
    }

    private fun ResolutionResult.Success.WithCandidate.Value.shouldWrap(
        context: GraphContext
    ): Boolean = !candidate.cacheExpressionResultIfPossible &&
            dependencyResults.isNotEmpty() &&
            context.graph.usages[this.usageKey]!!.size > 1 &&
            !context.isInBetweenCircularDependency(this)

    private fun ScopeContext.wrapExpressionInFunctionIfNeeded(
        result: ResolutionResult.Success.WithCandidate.Value,
        rawExpressionProvider: () -> IrExpression
    ): IrExpression = if (!result.shouldWrap(graphContext)) rawExpressionProvider()
    else with(result.safeAs<ResolutionResult.Success.WithCandidate.Value>()
        ?.outerMostScope?.let { findScopeContext(it) } ?: this) {
        functionWrappedExpressions.getOrPut(result.candidate.type) {
            val function = IrFactoryImpl.buildFun {
                origin = IrDeclarationOrigin.DEFINED
                name = "local${graphContext.variableIndex++}".asNameId()
                returnType = result.candidate.type.toIrType(pluginContext, localClasses, context)
                visibility = DescriptorVisibilities.LOCAL
                isSuspend = scope.callContext == CallContext.SUSPEND
            }.apply {
                parent = irScope.getLocalDeclarationParent()
                if (result.candidate.callContext == CallContext.COMPOSABLE) {
                    annotations += DeclarationIrBuilder(pluginContext, symbol)
                        .irCallConstructor(
                            pluginContext.referenceConstructors(InjektFqNames.Composable)
                                .single(),
                            emptyList()
                        )
                }
                this.body = DeclarationIrBuilder(pluginContext, symbol).run {
                    irBlockBody {
                        +irReturn(rawExpressionProvider())
                    }
                }
                statements += this
            }

            val expression: ScopeContext.() -> IrExpression = {
                DeclarationIrBuilder(pluginContext, symbol)
                    .irCall(function)
            }
            expression
        }
    }.invoke(this)

    private fun ResolutionResult.Success.WithCandidate.Value.shouldCache(
        context: GraphContext
    ): Boolean = candidate.cacheExpressionResultIfPossible &&
            context.graph.usages[this.usageKey]!!.count { !it.isInline } > 1 &&
            !context.isInBetweenCircularDependency(this)

    private fun ScopeContext.cacheExpressionIfNeeded(
        result: ResolutionResult.Success.WithCandidate.Value,
        rawExpressionProvider: () -> IrExpression
    ): IrExpression {
        if (!result.shouldCache(graphContext)) return rawExpressionProvider()
        return with(findScopeContext(result.outerMostScope)) {
            cachedExpressions.getOrPut(result.candidate.type) {
                val variable = irScope.createTemporaryVariable(
                    rawExpressionProvider(),
                    nameHint = "${graphContext.variableIndex++}"
                )
                statements += variable
                val expression: ScopeContext.() -> IrExpression = {
                    DeclarationIrBuilder(pluginContext, symbol)
                        .irGet(variable)
                }
                expression
            }
        }.invoke(this)
    }

    private fun ScopeContext.objectExpression(type: TypeRef): IrExpression =
        DeclarationIrBuilder(pluginContext, symbol)
            .irGetObject(pluginContext.referenceClass(type.classifier.fqName)!!)

    private fun ScopeContext.abstractGivenExpression(
        result: ResolutionResult.Success.WithCandidate.Value,
        given: AbstractGivenNode
    ): IrExpression = DeclarationIrBuilder(pluginContext, symbol).irBlock {
        val clazz = IrFactoryImpl.buildClass {
            name = given.callableFqName.shortName()
            visibility = DescriptorVisibilities.LOCAL
        }.apply clazz@{
            parent = scope.getLocalDeclarationParent()
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes += given.type.toIrType(pluginContext, localClasses, this@GivenCallTransformer.context)

            addConstructor {
                returnType = defaultType
                isPrimary = true
                visibility = DescriptorVisibilities.PUBLIC
            }.apply {
                body = DeclarationIrBuilder(
                    pluginContext,
                    symbol
                ).irBlockBody {
                    if (given.type.classifier.descriptor!!.cast<ClassDescriptor>()
                            .kind == ClassKind.CLASS) {
                        +irDelegatingConstructorCall(
                            given.superConstructor.callable
                                .cast<ClassConstructorDescriptor>()
                                .irConstructor()
                        ).apply {
                            fillGivens(
                                this@abstractGivenExpression,
                                result.dependencyResults
                                    .filterKeys {
                                        it in given.constructorDependencies
                                    }
                            )
                        }
                    } else {
                        +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                    }
                    +IrInstanceInitializerCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        this@clazz.symbol,
                        context.irBuiltIns.unitType
                    )
                }
            }

            given.requestCallables.forEach { requestCallable ->
                fun IrSimpleFunction.setupFunction() {
                    if (requestCallable.callContext == CallContext.COMPOSABLE) {
                        annotations += DeclarationIrBuilder(pluginContext, symbol)
                            .irCallConstructor(
                                pluginContext.referenceConstructors(InjektFqNames.Composable)
                                    .single(),
                                emptyList()
                            )
                    }

                    addDispatchReceiver { type = defaultType }

                    if (requestCallable.callable.extensionReceiverParameter != null) {
                        addExtensionReceiver(
                            requestCallable.parameterTypes["_extensionReceiver"]!!
                                .toIrType(pluginContext, localClasses, this@GivenCallTransformer.context)
                        )
                    }

                    requestCallable.callable.valueParameters.forEach { parameter ->
                        addValueParameter(
                            parameter.name.asString(),
                            requestCallable.parameterTypes[parameter.injektName()]!!
                                .toIrType(pluginContext, localClasses, this@GivenCallTransformer.context)
                        )
                    }

                    body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                        val dependencyScopeContext = ScopeContext(
                            this@abstractGivenExpression,
                            graphContext, given.dependencyScopesByRequestCallable[requestCallable]!!, scope)
                        val expression = with(dependencyScopeContext) {
                            val request = given.requestsByRequestCallables[requestCallable]!!
                            val requestResult = result.dependencyResults[request]!!
                            if (requestResult is ResolutionResult.Success.WithCandidate.Value) {
                                val previousParametersMap = parameterMap.toMap()
                                requestCallable.callable.allParameters
                                    .filter { it != requestCallable.callable.dispatchReceiverParameter }
                                    .forEachWith(valueParameters) { a, b -> parameterMap[a] = b }
                                expressionFor(requestResult)
                                    .also {
                                        parameterMap.clear()
                                        parameterMap.putAll(previousParametersMap)
                                    }
                            } else {
                                irCall(
                                    overriddenSymbols.single().owner,
                                    null,
                                    superTypes.single().classOrNull!!
                                ).apply {
                                    dispatchReceiver = irGet(dispatchReceiverParameter!!)
                                    extensionReceiverParameter?.let {
                                        extensionReceiver = irGet(it)
                                    }
                                    valueParameters.forEach {
                                        putValueArgument(it.index, irGet(it))
                                    }
                                }
                            }
                        }
                        dependencyScopeContext.statements.forEach { +it }
                        +irReturn(expression)
                    }
                }

                if (requestCallable.callable is PropertyDescriptor) {
                    addProperty {
                        name = requestCallable.callable.name
                        visibility = requestCallable.callable.visibility
                    }.apply {
                        addGetter {
                            returnType = requestCallable.type.toIrType(
                                pluginContext, localClasses, this@GivenCallTransformer.context)
                            visibility = requestCallable.callable.getter!!.visibility
                        }.apply {
                            overriddenSymbols = overriddenSymbols + requestCallable.callable.cast<PropertyDescriptor>()
                                .getter!!
                                .irPropertyGetter()
                                .symbol
                                .cast<IrSimpleFunctionSymbol>()
                            setupFunction()
                        }
                    }
                } else {
                    addFunction {
                        returnType = requestCallable.type.toIrType(
                            pluginContext, localClasses, this@GivenCallTransformer.context)
                        name = requestCallable.callable.name
                        isSuspend = requestCallable.callContext == CallContext.SUSPEND
                        visibility = requestCallable.callable.visibility
                    }.apply {
                        overriddenSymbols = overriddenSymbols + requestCallable.callable.cast<FunctionDescriptor>()
                            .irFunction()
                            .symbol
                            .cast<IrSimpleFunctionSymbol>()
                        setupFunction()
                    }
                }
            }
        }

        +clazz
        +irCall(clazz.constructors.single())
    }

    private fun ScopeContext.providerExpression(
        result: ResolutionResult.Success.WithCandidate.Value,
        given: ProviderGivenNode
    ): IrExpression = DeclarationIrBuilder(pluginContext, symbol)
        .irLambda(
            given.type.toIrType(pluginContext, localClasses, context),
            parameterNameProvider = { "p${graphContext.variableIndex++}" }
        ) { function ->
            when (val dependencyResult = result.dependencyResults.values.single()) {
                is ResolutionResult.Success.DefaultValue -> return@irLambda irNull()
                is ResolutionResult.Success.WithCandidate -> {
                    val dependencyScopeContext = ScopeContext(
                        this@providerExpression, graphContext, given.dependencyScopes.values.single(), scope)
                    val expression = with(dependencyScopeContext) {
                        val previousParametersMap = parameterMap.toMap()
                        given.parameterDescriptors
                            .forEachWith(function.valueParameters) { a, b -> parameterMap[a] = b }
                        expressionFor(dependencyResult)
                            .also {
                                parameterMap.clear()
                                parameterMap.putAll(previousParametersMap)
                            }
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
        }

    private val mutableSetOf = pluginContext.referenceFunctions(
        FqName("kotlin.collections.mutableSetOf")
    ).single { it.owner.valueParameters.isEmpty() }
    private val setAdd = mutableSetOf.owner.returnType
        .classOrNull!!
        .owner
        .functions
        .single { it.name.asString() == "add" }
    private val setAddAll = mutableSetOf.owner.returnType
        .classOrNull!!
        .owner
        .functions
        .single { it.name.asString() == "addAll" }
    private val setOf = pluginContext.referenceFunctions(
        FqName("kotlin.collections.setOf")
    ).single { it.owner.valueParameters.singleOrNull()?.isVararg == false }
    private val iterableToSet = pluginContext.referenceFunctions(
        FqName("kotlin.collections.toSet")
    ).single {
        it.owner.extensionReceiverParameter?.type?.classifierOrNull?.descriptor ==
                pluginContext.builtIns.iterable
    }

    private fun ScopeContext.setExpression(
        result: ResolutionResult.Success.WithCandidate.Value,
        given: SetGivenNode
    ): IrExpression = when (given.dependencies.size) {
        1 -> {
            val singleDependency =
                result.dependencyResults.values.single()
                    .cast<ResolutionResult.Success.WithCandidate.Value>()
            when {
                singleDependency.candidate.type.isAssignableTo(context, given.type) ->
                    expressionFor(result.dependencyResults.values.single().cast())
                singleDependency.candidate.type.isAssignableTo(context, given.collectionElementType) -> {
                    DeclarationIrBuilder(pluginContext, symbol)
                        .irCall(iterableToSet)
                        .apply {
                            extensionReceiver =
                                expressionFor(result.dependencyResults.values.single().cast())
                            putTypeArgument(
                                0,
                                given.singleElementType.toIrType(
                                    pluginContext,
                                    localClasses,
                                    context
                                )
                            )
                        }
                }
                else -> DeclarationIrBuilder(pluginContext, symbol)
                    .irCall(setOf)
                    .apply {
                        putTypeArgument(
                            0,
                            given.singleElementType.toIrType(
                                pluginContext,
                                localClasses,
                                context
                            )
                        )
                        putValueArgument(
                            0,
                            expressionFor(result.dependencyResults.values.single().cast())
                        )
                    }
            }
        }
        else -> {
            DeclarationIrBuilder(pluginContext, symbol).irBlock {
                val tmpSet = irTemporary(
                    irCall(mutableSetOf)
                        .apply {
                            putTypeArgument(
                                0,
                                given.singleElementType.toIrType(
                                    pluginContext,
                                    localClasses,
                                    this@GivenCallTransformer.context
                                )
                            )
                        },
                    nameHint = "${graphContext.variableIndex++}"
                )

                result.dependencyResults
                    .forEach { (_, dependency) ->
                        if (dependency !is ResolutionResult.Success.WithCandidate.Value)
                            return@forEach
                        if (dependency.candidate.type.isAssignableTo(
                                this@GivenCallTransformer.context,
                                given.collectionElementType)) {
                            +irCall(setAddAll).apply {
                                dispatchReceiver = irGet(tmpSet)
                                putValueArgument(0, expressionFor(dependency))
                            }
                        } else {
                            +irCall(setAdd).apply {
                                dispatchReceiver = irGet(tmpSet)
                                putValueArgument(0, expressionFor(dependency))
                            }
                        }
                    }

                +irGet(tmpSet)
            }
        }
    }

    private fun ScopeContext.callableExpression(
        result: ResolutionResult.Success.WithCandidate.Value,
        given: CallableGivenNode
    ): IrExpression = when (given.callable.callable) {
        is ClassConstructorDescriptor -> classExpression(
            result,
            given,
            given.callable.callable
        )
        is PropertyDescriptor -> propertyExpression(
            result,
            given,
            given.callable.callable
        )
        is FunctionDescriptor -> functionExpression(
            result,
            given,
            given.callable.callable
        )
        is ReceiverParameterDescriptor -> if (given.type.classifier.isObject) objectExpression(given.type)
        else parameterExpression(given.callable.callable)
        is ValueParameterDescriptor -> parameterExpression(given.callable.callable)
        is VariableDescriptor -> variableExpression(given.callable.callable)
        else -> error("Unsupported callable $given")
    }

    private fun ScopeContext.classExpression(
        result: ResolutionResult.Success.WithCandidate.Value,
        given: CallableGivenNode,
        descriptor: ClassConstructorDescriptor
    ): IrExpression = if (descriptor.constructedClass.kind == ClassKind.OBJECT) {
        val clazz = pluginContext.referenceClass(descriptor.constructedClass.fqNameSafe)!!
        DeclarationIrBuilder(pluginContext, symbol)
            .irGetObject(clazz)
    } else {
        val constructor = descriptor.irConstructor()
        DeclarationIrBuilder(pluginContext, symbol)
            .irCall(constructor.symbol)
            .apply {
                fillTypeParameters(given.callable)
                fillGivens(this@classExpression, result.dependencyResults)
            }
    }

    private fun ScopeContext.propertyExpression(
        result: ResolutionResult.Success.WithCandidate.Value,
        given: CallableGivenNode,
        descriptor: PropertyDescriptor
    ): IrExpression {
        val property = pluginContext.referenceProperties(descriptor.fqNameSafe)
            .single { it.descriptor.uniqueKey(context) == descriptor.uniqueKey(context) }
        val getter = property.owner.getter!!
        return DeclarationIrBuilder(pluginContext, symbol)
            .irCall(getter.symbol)
            .apply {
                fillTypeParameters(given.callable)
                fillGivens(this@propertyExpression, result.dependencyResults)
            }
    }

    private fun ScopeContext.functionExpression(
        result: ResolutionResult.Success.WithCandidate.Value,
        given: CallableGivenNode,
        descriptor: FunctionDescriptor
    ): IrExpression {
        val function = descriptor.irFunction()
        return DeclarationIrBuilder(pluginContext, symbol)
            .irCall(function.symbol)
            .apply {
                fillTypeParameters(given.callable)
                fillGivens(this@functionExpression, result.dependencyResults)
            }
    }

    private fun ScopeContext.parameterExpression(descriptor: ParameterDescriptor): IrExpression =
        when (val containingDeclaration = descriptor.containingDeclaration) {
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
                    parameterMap[descriptor] ?: containingDeclaration.irFunction()
                        .let { function ->
                            function.allParameters
                                .filter { it != function.dispatchReceiverParameter }
                        }
                        .single { it.index == descriptor.index() }
                )
            else -> error("Unexpected parent $descriptor $containingDeclaration")
        }

    private fun IrFunctionAccessExpression.fillTypeParameters(callable: CallableRef) {
        callable
            .typeArguments
            .values
            .forEachIndexed { index, typeArgument ->
                putTypeArgument(index, typeArgument.toIrType(pluginContext, localClasses, context))
            }
    }

    private fun ScopeContext.variableExpression(descriptor: VariableDescriptor): IrExpression =
        DeclarationIrBuilder(pluginContext, symbol)
            .irGet(localVariables.single { it.descriptor == descriptor })

    private fun ClassConstructorDescriptor.irConstructor(): IrConstructor {
        if (constructedClass.visibility == DescriptorVisibilities.LOCAL) {
            return localClasses
                .single { it.descriptor.fqNameSafe == constructedClass.fqNameSafe }
                .constructors
                .single { it.descriptor.uniqueKey(context) == uniqueKey(context) }
        }
        return pluginContext.referenceConstructors(constructedClass.fqNameSafe)
            .single { it.descriptor.uniqueKey(context) == uniqueKey(context) }
            .owner
    }

    private fun PropertyGetterDescriptor.irPropertyGetter(): IrFunction {
        val property = propertyIfAccessor as PropertyDescriptor
        return pluginContext.referenceProperties(property.fqNameSafe)
            .single { it.descriptor.uniqueKey(context) == property.uniqueKey(context) }
            .owner
            .getter!!
    }

    private fun FunctionDescriptor.irFunction(): IrFunction {
        if (visibility == DescriptorVisibilities.LOCAL) {
            return localFunctions.single {
                it.descriptor.uniqueKey(context) == uniqueKey(context)
            }
        }
        return pluginContext.referenceFunctions(fqNameSafe)
            .single { it.descriptor.uniqueKey(context) == uniqueKey(context) }
            .owner
    }

    private val receiverAccessors = mutableListOf<Pair<IrClass, () -> IrExpression>>()

    private val localVariables = mutableListOf<IrVariable>()
    private val localFunctions = mutableListOf<IrFunction>()
    private val localClasses = mutableListOf<IrClass>()

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
        if (declaration.isLocal) localClasses += declaration
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
        if (declaration.isLocal) localFunctions += declaration
        val result = super.visitFunction(declaration)
        if (dispatchReceiver != null) receiverAccessors.pop()
        if (extensionReceiver != null) receiverAccessors.pop()
        return result
    }

    override fun visitVariable(declaration: IrVariable): IrStatement =
        super.visitVariable(declaration)
            .also { localVariables += declaration }

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
        val graphContext = GraphContext(graph)
        return DeclarationIrBuilder(pluginContext, result.symbol)
            .irBlock {
                try {
                    ScopeContext(
                        parent = null,
                        graphContext = graphContext,
                        scope = graph.scope,
                        irScope = scope
                    ).run { result.fillGivens(this, graph.results) }
                } catch (e: Throwable) {
                    throw RuntimeException("Wtf ${expression.dump()}", e)
                }
                graphContext.statements.forEach { +it }
                +result
            }
    }
}
