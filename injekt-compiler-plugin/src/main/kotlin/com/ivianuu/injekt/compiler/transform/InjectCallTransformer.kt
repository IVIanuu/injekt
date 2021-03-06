/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.cfg.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class InjectCallTransformer(
  @Inject private val analysisContext: AnalysisContext,
  @Inject private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
  private inner class GraphContext(
    val graph: InjectionGraph.Success,
    val scope: ScopeWithIr,
    val startOffset: Int
  ) {
    val statements = mutableListOf<IrStatement>()

    var variableIndex = 0

    private val graphContextParents = buildList<InjectablesScope> {
      var current: InjectablesScope? = graph.scope.parent
      while (current != null) {
        this += current
        current = current.parent
      }
    }

    private val isInBetweenCircularDependency =
      mutableMapOf<ResolutionResult.Success.WithCandidate.Value, Boolean>()

    fun isInBetweenCircularDependency(result: ResolutionResult.Success.WithCandidate.Value): Boolean =
      isInBetweenCircularDependency.getOrPut(result) {
        val allResults = mutableListOf<ResolutionResult.Success.WithCandidate.Value>()
        var isInBetweenCircularDependency = false
        fun ResolutionResult.Success.WithCandidate.visit() {
          if (this is ResolutionResult.Success.WithCandidate.CircularDependency &&
            allResults.none { it.candidate == candidate }
          ) {
            isInBetweenCircularDependency = true
            return
          }
          if (this is ResolutionResult.Success.WithCandidate.Value) {
            allResults += this
            dependencyResults.forEach { (_, dependencyResult) ->
              if (!isInBetweenCircularDependency &&
                dependencyResult is ResolutionResult.Success.WithCandidate
              )
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

    fun mapScopeIfNeeded(scope: InjectablesScope) =
      if (scope in graphContextParents) graph.scope else scope
        .allScopes.last { !it.isTypeScope }
  }

  private inner class ScopeContext(
    val parent: ScopeContext?,
    val graphContext: GraphContext,
    val scope: InjectablesScope,
    val irScope: Scope
  ) {
    val symbol = irScope.scopeOwnerSymbol
    val functionWrappedExpressions = mutableMapOf<TypeRef, ScopeContext.() -> IrExpression>()
    val cachedExpressions = mutableMapOf<TypeRef, ScopeContext.() -> IrExpression>()
    val statements =
      if (scope == graphContext.graph.scope) graphContext.statements else mutableListOf()
    val initializingExpressions: MutableMap<Injectable, InjectableExpression> =
      parent?.initializingExpressions ?: mutableMapOf()
    val parameterMap: MutableMap<ParameterDescriptor, IrValueParameter> =
      parent?.parameterMap ?: mutableMapOf()

    fun findScopeContext(scopeToFind: InjectablesScope): ScopeContext {
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
      val expression = InjectableExpression(result)
      initializingExpressions[result.candidate] = expression
      val irExpression = expression.run { get() }
      initializingExpressions -= result.candidate
      return irExpression
    }
  }

  private fun IrFunctionAccessExpression.inject(
    context: ScopeContext,
    results: Map<InjectableRequest, ResolutionResult.Success>
  ) {
    results
      .forEach { (request, result) ->
        if (result !is ResolutionResult.Success.WithCandidate) return@forEach
        val expression = context.expressionFor(result)
        when (request.parameterIndex) {
          DISPATCH_RECEIVER_INDEX -> dispatchReceiver = expression
          EXTENSION_RECEIVER_INDEX -> extensionReceiver = expression
          else -> {
            putValueArgument(
              symbol.owner
                .valueParameters
                .first { it.descriptor.injektIndex() == request.parameterIndex }
                .index,
              expression
            )
          }
        }
      }
  }

  private inner class InjectableExpression(private val result: ResolutionResult.Success.WithCandidate) {
    private var block: IrBlock? = null
    private var tmpVariable: IrVariable? = null
    private var finalExpression: IrExpression? = null

    private var initializing = false

    fun ScopeContext.get(): IrExpression {
      if (initializing) {
        if (block == null) {
          val resultType = result.candidate.type.toIrType()
            .typeOrNull!!
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
            is CallableInjectable -> callableExpression(result, result.candidate.cast())
            is ProviderInjectable -> providerExpression(result, result.candidate.cast())
            is SetInjectable -> setExpression(result, result.candidate.cast())
            is SourceKeyInjectable -> sourceKeyExpression()
            is TypeKeyInjectable -> typeKeyExpression(result, result.candidate.cast())
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
        returnType = result.candidate.type.toIrType()
          .typeOrNull!!
        visibility = DescriptorVisibilities.LOCAL
        isSuspend = scope.callContext == CallContext.SUSPEND
      }.apply {
        parent = irScope.getLocalDeclarationParent()
        if (result.candidate.callContext == CallContext.COMPOSABLE) {
          annotations = annotations + DeclarationIrBuilder(pluginContext, symbol)
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
          .irCall(
            function.symbol,
            result.candidate.type.toIrType().typeOrNull!!
          )
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

  private fun ScopeContext.providerExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: ProviderInjectable
  ): IrExpression = DeclarationIrBuilder(pluginContext, symbol)
    .irLambda(
      injectable.type.toIrType().typeOrNull!!,
      parameterNameProvider = { "p${graphContext.variableIndex++}" }
    ) { function ->
      when (val dependencyResult = result.dependencyResults.values.single()) {
        is ResolutionResult.Success.DefaultValue -> return@irLambda irNull()
        is ResolutionResult.Success.WithCandidate -> {
          val dependencyScopeContext = ScopeContext(
            this@providerExpression, graphContext, injectable.dependencyScope, scope
          )
          val expression = with(dependencyScopeContext) {
            val previousParametersMap = parameterMap.toMap()
            injectable.parameterDescriptors
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
    injectable: SetInjectable
  ): IrExpression = when (injectable.dependencies.size) {
    1 -> {
      val singleDependency = result.dependencyResults.values.single()
        .cast<ResolutionResult.Success.WithCandidate.Value>()
      when {
        singleDependency.candidate.type.isSubTypeOf(injectable.type) ->
          expressionFor(result.dependencyResults.values.single().cast())
        singleDependency.candidate.type.isSubTypeOf(injectable.collectionElementType) -> {
          DeclarationIrBuilder(pluginContext, symbol)
            .irCall(iterableToSet)
            .apply {
              extensionReceiver =
                expressionFor(result.dependencyResults.values.single().cast())
              putTypeArgument(
                0,
                injectable.singleElementType.toIrType().typeOrNull
              )
            }
        }
        else -> DeclarationIrBuilder(pluginContext, symbol)
          .irCall(setOf)
          .apply {
            putTypeArgument(
              0,
              injectable.singleElementType.toIrType().typeOrNull
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
                injectable.singleElementType.toIrType().typeOrNull
              )
            },
          nameHint = "${graphContext.variableIndex++}"
        )

        result.dependencyResults
          .forEach { (_, dependency) ->
            if (dependency !is ResolutionResult.Success.WithCandidate.Value)
              return@forEach
            if (dependency.candidate.type.isSubTypeOf(injectable.collectionElementType)) {
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

  private val sourceKeyConstructor = pluginContext.referenceClass(InjektFqNames.SourceKey)
    ?.constructors?.single()

  private fun ScopeContext.sourceKeyExpression(): IrExpression =
    DeclarationIrBuilder(pluginContext, symbol).run {
      irCall(sourceKeyConstructor!!).apply {
        putValueArgument(
          0,
          irString(
            buildString {
              append(currentFile.name)
              append(":")
              val path = graphContext.scope.irElement.cast<IrDeclaration>()
                .descriptor
                .fqNameSafe
                .pathSegments()
                .filter {
                  it.asString() != "<anonymous>" &&
                      it.asString() != "<init>"
                }
                .map {
                  it.asString()
                    .removePrefix("<get-")
                    .removePrefix("<set-")
                    .removeSuffix(">")
                }

              path.forEachIndexed { index, name ->
                append(name)
                if (index != path.lastIndex) append(".")
              }
              append(":")

              append(currentFile.fileEntry.getLineNumber(graphContext.startOffset) + 1)
              append(":")
              append(currentFile.fileEntry.getColumnNumber(graphContext.startOffset))
            }
          )
        )
      }
    }

  private val typeKey = pluginContext.referenceClass(InjektFqNames.TypeKey)
  private val typeKeyValue = typeKey?.owner?.properties
    ?.single { it.name.asString() == "value" }
  private val typeKeyConstructor = typeKey?.constructors?.single()

  private fun ScopeContext.typeKeyExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: TypeKeyInjectable
  ): IrExpression = DeclarationIrBuilder(pluginContext, symbol).run {
    val expressions = mutableListOf<IrExpression>()
    var currentString = ""
    fun commitCurrentString() {
      if (currentString.isNotEmpty()) {
        expressions += irString(currentString)
        currentString = ""
      }
    }

    fun appendToCurrentString(value: String) {
      currentString += value
    }

    fun appendTypeParameterExpression(expression: IrExpression) {
      commitCurrentString()
      expressions += expression
    }

    injectable.type.arguments.single().render(
      renderType = { typeToRender ->
        if (!typeToRender.classifier.isTypeParameter) true else {
          appendTypeParameterExpression(
            irCall(typeKeyValue!!.getter!!).apply {
              dispatchReceiver = expressionFor(
                result.dependencyResults.values.single {
                  it is ResolutionResult.Success.WithCandidate &&
                      it.candidate.type.arguments.single().classifier == typeToRender.classifier
                }.cast()
              )
            }
          )
          false
        }
      },
      append = { appendToCurrentString(it) }
    )

    commitCurrentString()

    val stringExpression = if (expressions.size == 1) {
      expressions.single()
    } else {
      val stringPlus = pluginContext.irBuiltIns.stringClass
        .functions
        .map { it.owner }
        .first { it.name.asString() == "plus" }
      expressions.reduce { acc, expression ->
        irCall(stringPlus).apply {
          dispatchReceiver = acc
          putValueArgument(0, expression)
        }
      }
    }

    irCall(typeKeyConstructor!!).apply {
      putTypeArgument(0, injectable.type.arguments.single().toIrType().cast())
      putValueArgument(0, stringExpression)
    }
  }

  private fun ScopeContext.callableExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: CallableInjectable
  ): IrExpression = when (injectable.callable.callable) {
    is ClassConstructorDescriptor -> classExpression(
      result,
      injectable,
      injectable.callable.callable
    )
    is PropertyDescriptor -> propertyExpression(
      result,
      injectable,
      injectable.callable.callable
    )
    is FunctionDescriptor -> functionExpression(
      result,
      injectable,
      injectable.callable.callable
    )
    is ReceiverParameterDescriptor -> if (injectable.callable.type.unwrapTags().classifier.isObject)
      objectExpression(injectable.type)
    else parameterExpression(injectable.callable.callable, injectable)
    is ValueParameterDescriptor -> parameterExpression(injectable.callable.callable, injectable)
    is VariableDescriptor -> variableExpression(injectable.callable.callable, injectable)
    else -> error("Unsupported callable $injectable")
  }

  private fun ScopeContext.classExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: CallableInjectable,
    descriptor: ClassConstructorDescriptor
  ): IrExpression = if (descriptor.constructedClass.kind == ClassKind.OBJECT) {
    val clazz = pluginContext.referenceClass(descriptor.constructedClass.fqNameSafe)!!
    DeclarationIrBuilder(pluginContext, symbol)
      .irGetObject(clazz)
  } else {
    val constructor = descriptor.irConstructor()
    DeclarationIrBuilder(pluginContext, symbol)
      .irCall(
        constructor.symbol,
        injectable.type.toIrType().typeOrNull!!
      )
      .apply {
        fillTypeParameters(injectable.callable)
        inject(this@classExpression, result.dependencyResults)
      }
  }

  private fun ScopeContext.propertyExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: CallableInjectable,
    descriptor: PropertyDescriptor
  ): IrExpression {
    val property = pluginContext.referenceProperties(descriptor.fqNameSafe)
      .single { it.descriptor.uniqueKey() == descriptor.uniqueKey() }
    val getter = property.owner.getter!!
    return DeclarationIrBuilder(pluginContext, symbol)
      .irCall(
        getter.symbol,
        injectable.type.toIrType().typeOrNull!!
      )
      .apply {
        fillTypeParameters(injectable.callable)
        inject(this@propertyExpression, result.dependencyResults)
      }
  }

  private fun ScopeContext.functionExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: CallableInjectable,
    descriptor: FunctionDescriptor
  ): IrExpression {
    val function = descriptor.irFunction()
    return DeclarationIrBuilder(pluginContext, symbol)
      .irCall(
        function.symbol,
        injectable.type.toIrType().typeOrNull!!
      )
      .apply {
        fillTypeParameters(injectable.callable)
        inject(this@functionExpression, result.dependencyResults)
      }
  }

  private fun ScopeContext.parameterExpression(
    descriptor: ParameterDescriptor,
    injectable: CallableInjectable
  ): IrExpression =
    when (val containingDeclaration = descriptor.containingDeclaration) {
      is ClassDescriptor -> receiverAccessors.last {
        descriptor.type.constructor.declarationDescriptor == it.first.descriptor
      }.second()
      is ClassConstructorDescriptor -> DeclarationIrBuilder(pluginContext, symbol)
        .irGet(
          injectable.type.toIrType().typeOrNull!!,
          containingDeclaration.irConstructor()
            .allParameters
            .single { it.name == descriptor.name }
            .symbol
        )
      is FunctionDescriptor -> DeclarationIrBuilder(pluginContext, symbol)
        .irGet(
          injectable.type.toIrType().typeOrNull!!,
          (parameterMap[descriptor] ?: containingDeclaration.irFunction()
            .let { function ->
              function.allParameters
                .filter { it != function.dispatchReceiverParameter }
            }
            .single { it.index == descriptor.index() })
            .symbol
        )
      else -> error("Unexpected parent $descriptor $containingDeclaration")
    }

  private fun IrFunctionAccessExpression.fillTypeParameters(callable: CallableRef) {
    callable
      .typeArguments
      .values
      .forEachIndexed { index, typeArgument ->
        putTypeArgument(index, typeArgument.toIrType().typeOrNull)
      }
  }

  private fun ScopeContext.variableExpression(
    descriptor: VariableDescriptor,
    injectable: CallableInjectable
  ): IrExpression {
    return if (descriptor is LocalVariableDescriptor && descriptor.isDelegated) {
      val localFunction = localFunctions.single { candidateFunction ->
        candidateFunction.descriptor
          .safeAs<LocalVariableAccessorDescriptor.Getter>()
          ?.correspondingVariable == descriptor
      }
      DeclarationIrBuilder(pluginContext, symbol)
        .irCall(
          localFunction.symbol,
          injectable.type.toIrType().typeOrNull!!
        )
    } else {
      DeclarationIrBuilder(pluginContext, symbol)
        .irGet(
          injectable.type.toIrType().typeOrNull!!,
          localVariables.single { it.descriptor == descriptor }.symbol
        )
    }
  }

  private fun ClassConstructorDescriptor.irConstructor(): IrConstructor {
    if (constructedClass.visibility == DescriptorVisibilities.LOCAL) {
      return localClasses
        .single { it.descriptor.fqNameSafe == constructedClass.fqNameSafe }
        .constructors
        .single { it.descriptor.uniqueKey() == uniqueKey() }
    }
    return pluginContext.referenceConstructors(constructedClass.fqNameSafe)
      .single { it.descriptor.uniqueKey() == uniqueKey() }
      .owner
  }

  private fun FunctionDescriptor.irFunction(): IrFunction {
    if (visibility == DescriptorVisibilities.LOCAL) {
      return localFunctions.single {
        it.descriptor.uniqueKey() == uniqueKey()
      }
    }
    return pluginContext.referenceFunctions(fqNameSafe)
      .single { it.descriptor.uniqueKey() == uniqueKey() }
      .owner
  }

  private val receiverAccessors = mutableListOf<Pair<IrClass, () -> IrExpression>>()

  private val localVariables = mutableListOf<IrVariable>()
  private val localFunctions = mutableListOf<IrFunction>()
  @Provide private val localClasses = mutableListOf<IrClass>()

  override fun visitClassNew(declaration: IrClass): IrStatement {
    receiverAccessors.push(
      declaration to {
        DeclarationIrBuilder(pluginContext, declaration.symbol)
          .irGet(declaration.thisReceiver!!)
      }
    )
    if (declaration.isLocal) localClasses += declaration
    val result = super.visitClassNew(declaration)
    receiverAccessors.pop()
    return result
  }

  override fun visitFunctionNew(declaration: IrFunction): IrStatement {
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
    val result = super.visitFunctionNew(declaration)
    if (dispatchReceiver != null) receiverAccessors.pop()
    if (extensionReceiver != null) receiverAccessors.pop()
    return result
  }

  override fun visitVariable(declaration: IrVariable): IrStatement =
    super.visitVariable(declaration)
      .also { localVariables += declaration }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
    // ignore safe calls
    if (result.origin == IrStatementOrigin.EQEQ)
      return result

    val graph = pluginContext.bindingContext[
        InjektWritableSlices.INJECTION_GRAPH_FOR_POSITION,
        SourcePosition(currentFile.fileEntry.name, result.startOffset, result.endOffset)
    ] ?: return result

    return DeclarationIrBuilder(pluginContext, result.symbol)
      .irBlock {
        val graphContext = GraphContext(graph, currentScope!!, result.startOffset)
        try {
          ScopeContext(
            parent = null,
            graphContext = graphContext,
            scope = graph.scope,
            irScope = scope
          ).run { result.inject(this, graph.results) }
        } catch (e: Throwable) {
          throw RuntimeException("Wtf ${expression.dump()}", e)
        }
        graphContext.statements.forEach { +it }
        +result
      }
  }
}
