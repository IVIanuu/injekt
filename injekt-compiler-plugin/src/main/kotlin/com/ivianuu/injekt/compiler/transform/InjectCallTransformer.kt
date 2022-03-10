/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@OptIn(ObsoleteDescriptorBasedAPI::class)
class InjectCallTransformer(
  private val localDeclarations: LocalDeclarations,
  private val irCtx: IrPluginContext,
  private val ctx: Context
) : IrElementTransformerVoidWithContext() {
  private inner class GraphContext(
    val graph: InjectionGraph.Success,
    val startOffset: Int
  ) {
    val statements = mutableListOf<IrStatement>()

    var variableIndex = 0

    @OptIn(ExperimentalStdlibApi::class)
    private val graphContextParents = buildList<InjectablesScope> {
      val seenScopes = mutableSetOf<InjectablesScope>()
      fun InjectablesScope.add() {
        if (!seenScopes.add(this)) return
        add(this)
        parent?.add()
        typeScopes.forEach { it.value.add() }
      }

      graph.scope.add()
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
        .allScopes.last { it.isDeclarationContainer }
  }

  private inner class ScopeContext(
    val parent: ScopeContext?,
    val graphContext: GraphContext,
    val scope: InjectablesScope,
    val irScope: Scope
  ) {
    val symbol = irScope.scopeOwnerSymbol
    val functionWrappedExpressions = mutableMapOf<TypeRef, ScopeContext.() -> IrExpression>()
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
    ctx: ScopeContext,
    results: Map<InjectableRequest, ResolutionResult.Success>
  ) {
    results
      .forEach { (request, result) ->
        if (result !is ResolutionResult.Success.WithCandidate) return@forEach
        val expression = ctx.expressionFor(result)
        when (request.parameterIndex) {
          DISPATCH_RECEIVER_INDEX -> dispatchReceiver = expression
          EXTENSION_RECEIVER_INDEX -> extensionReceiver = expression
          else -> putValueArgument(
            symbol.owner
              .valueParameters
              .first { it.descriptor.injektIndex() == request.parameterIndex }
              .index,
            expression
          )
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
          val resultType = result.candidate.type.toIrType(irCtx, localDeclarations, ctx)
            .typeOrNull!!
          block = DeclarationIrBuilder(irCtx, symbol)
            .irBlock(resultType = resultType) {
              tmpVariable = irTemporary(
                value = irNull(),
                isMutable = true,
                irType = resultType.makeNullable(),
                nameHint = "${graphContext.variableIndex++}"
              )
            } as IrBlock
        }
        return DeclarationIrBuilder(irCtx, symbol)
          .irGet(tmpVariable!!)
      }

      result as ResolutionResult.Success.WithCandidate.Value

      finalExpression?.let { return it }

      initializing = true

      val rawExpression = wrapExpressionInFunctionIfNeeded(result) {
        when (result.candidate) {
          is CallableInjectable -> callableExpression(result, result.candidate.cast())
          is ProviderInjectable -> providerExpression(result, result.candidate.cast())
          is ListInjectable -> listExpression(result, result.candidate.cast())
          is SourceKeyInjectable -> sourceKeyExpression()
          is TypeKeyInjectable -> typeKeyExpression(result, result.candidate.cast())
        }
      }

      initializing = false

      finalExpression = if (block == null) rawExpression else {
        with(DeclarationIrBuilder(irCtx, symbol)) {
          block!!.statements += irSet(tmpVariable!!.symbol, rawExpression)
          block!!.statements += irGet(tmpVariable!!)
        }
        block!!
      }

      return finalExpression!!
    }
  }

  private fun ResolutionResult.Success.WithCandidate.Value.shouldWrap(
    ctx: GraphContext
  ): Boolean = (candidate !is ProviderInjectable || !candidate.isInline) &&
      (dependencyResults.isNotEmpty() && ctx.graph.usages[this.usageKey]!!.size > 1) &&
      !ctx.isInBetweenCircularDependency(this)

  private fun ScopeContext.wrapExpressionInFunctionIfNeeded(
    result: ResolutionResult.Success.WithCandidate.Value,
    rawExpressionProvider: () -> IrExpression
  ): IrExpression = if (!result.shouldWrap(graphContext)) rawExpressionProvider()
  else with(result.safeAs<ResolutionResult.Success.WithCandidate.Value>()
    ?.highestScope?.let { findScopeContext(it) } ?: this) {
    functionWrappedExpressions.getOrPut(result.candidate.type) {
      val function = IrFactoryImpl.buildFun {
        origin = IrDeclarationOrigin.DEFINED
        name = "function${graphContext.variableIndex++}".asNameId()
        returnType = result.candidate.type.toIrType(irCtx, localDeclarations, ctx)
          .typeOrNull!!
        visibility = DescriptorVisibilities.LOCAL
      }.apply {
        parent = irScope.getLocalDeclarationParent()

        this.body = DeclarationIrBuilder(irCtx, symbol).run {
          irBlockBody {
            +irReturn(rawExpressionProvider())
          }
        }

        statements += this
      }

      val expression: ScopeContext.() -> IrExpression = {
        DeclarationIrBuilder(irCtx, symbol)
          .irCall(
            function.symbol,
            result.candidate.type.toIrType(irCtx, localDeclarations, ctx).typeOrNull!!
          )
      }
      expression
    }
  }.invoke(this)

  private fun ScopeContext.objectExpression(type: TypeRef): IrExpression =
    DeclarationIrBuilder(irCtx, symbol)
      .irGetObject(irCtx.referenceClass(type.classifier.fqName)!!)

  private fun ScopeContext.providerExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: ProviderInjectable
  ): IrExpression = DeclarationIrBuilder(irCtx, symbol)
    .irLambda(
      injectable.type.toIrType(irCtx, localDeclarations, ctx).typeOrNull!!,
      parameterNameProvider = { "p${graphContext.variableIndex++}" }
    ) { function ->
      val dependencyResult = result.dependencyResults.values.single()
      val dependencyScope = injectable.dependencyScopes.values.single()
      val dependencyScopeContext = if (dependencyScope == this@providerExpression.scope) null
      else ScopeContext(
        this@providerExpression, graphContext,
        dependencyScope, scope
      )

      fun ScopeContext.createExpression(): IrExpression {
        for ((index, a) in injectable.parameterDescriptors.withIndex())
          parameterMap[a] = function.valueParameters[index]
        return expressionFor(dependencyResult.cast())
          .also {
            injectable.parameterDescriptors.forEach {
              parameterMap -= it
            }
          }
      }

      val expression = dependencyScopeContext?.run { createExpression() } ?: createExpression()

      if (dependencyScopeContext == null || dependencyScopeContext.statements.isEmpty()) expression
      else irBlock {
        dependencyScopeContext.statements.forEach { +it }
        +expression
      }
    }

  private val mutableListOf = irCtx.referenceFunctions(
    FqName("kotlin.collections.mutableListOf")
  ).single { it.owner.valueParameters.isEmpty() }
  private val listAdd = mutableListOf.owner.returnType
    .classOrNull!!
    .owner
    .functions
    .single { it.name.asString() == "add" && it.valueParameters.size == 1 }
  private val listAddAll = mutableListOf.owner.returnType
    .classOrNull!!
    .owner
    .functions
    .single { it.name.asString() == "addAll" && it.valueParameters.size == 1 }

  private fun ScopeContext.listExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: ListInjectable
  ): IrExpression = DeclarationIrBuilder(irCtx, symbol).irBlock {
    val tmpSet = irTemporary(
      irCall(mutableListOf)
        .apply {
          putTypeArgument(
            0,
            injectable.singleElementType.toIrType(irCtx, localDeclarations, ctx).typeOrNull
          )
        },
      nameHint = "${graphContext.variableIndex++}"
    )

    result.dependencyResults
      .forEach { (_, dependency) ->
        if (dependency !is ResolutionResult.Success.WithCandidate.Value)
          return@forEach
        if (dependency.candidate.type.isSubTypeOf(injectable.collectionElementType, ctx)) {
          +irCall(listAddAll).apply {
            dispatchReceiver = irGet(tmpSet)
            putValueArgument(0, expressionFor(dependency))
          }
        } else {
          +irCall(listAdd).apply {
            dispatchReceiver = irGet(tmpSet)
            putValueArgument(0, expressionFor(dependency))
          }
        }
      }

    +irGet(tmpSet)
  }

  private val sourceKeyConstructor = irCtx.referenceClass(InjektFqNames.SourceKey)
    ?.constructors?.single()

  private fun ScopeContext.sourceKeyExpression(): IrExpression =
    DeclarationIrBuilder(irCtx, symbol).run {
      irCall(sourceKeyConstructor!!).apply {
        putValueArgument(
          0,
          irString(
            buildString {
              append(currentFile.name)
              append(":")
              append(currentFile.fileEntry.getLineNumber(graphContext.startOffset) + 1)
              append(":")
              append(currentFile.fileEntry.getColumnNumber(graphContext.startOffset))
            }
          )
        )
      }
    }

  private val typeKey = irCtx.referenceClass(InjektFqNames.TypeKey)
  private val typeKeyValue = typeKey?.owner?.properties
    ?.single { it.name.asString() == "value" }
  private val typeKeyConstructor = typeKey?.constructors?.single()
  private val stringPlus = irCtx.irBuiltIns.stringClass
    .functions
    .map { it.owner }
    .first { it.name.asString() == "plus" }

  private fun ScopeContext.typeKeyExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: TypeKeyInjectable
  ): IrExpression = DeclarationIrBuilder(irCtx, symbol).run {
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
      expressions.reduce { acc, expression ->
        irCall(stringPlus).apply {
          dispatchReceiver = acc
          putValueArgument(0, expression)
        }
      }
    }

    irCall(typeKeyConstructor!!).apply {
      putTypeArgument(0, injectable.type.arguments.single().toIrType(irCtx, localDeclarations, ctx).cast())
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
      objectExpression(injectable.callable.type.unwrapTags())
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
    val clazz = descriptor.constructedClass.irClass(ctx, irCtx, localDeclarations)
    DeclarationIrBuilder(irCtx, symbol)
      .irGetObject(clazz.symbol)
  } else {
    val constructor = descriptor.irConstructor(ctx, irCtx, localDeclarations)
    DeclarationIrBuilder(irCtx, symbol)
      .irCall(
        constructor.symbol,
        injectable.type.toIrType(irCtx, localDeclarations, ctx).typeOrNull!!
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
    val property = descriptor.irProperty(ctx, irCtx, localDeclarations)
    val getter = property.getter!!
    return DeclarationIrBuilder(irCtx, symbol)
      .irCall(
        getter.symbol,
        injectable.type.toIrType(irCtx, localDeclarations, ctx).typeOrNull!!
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
    val function = descriptor.irFunction(ctx, irCtx, localDeclarations)
    return DeclarationIrBuilder(irCtx, symbol)
      .irCall(function.symbol, injectable.type.toIrType(irCtx, localDeclarations, ctx).typeOrNull!!)
      .apply {
        fillTypeParameters(injectable.callable)
        inject(this@functionExpression, result.dependencyResults)
      }
  }

  private fun receiverExpression(
    descriptor: ParameterDescriptor
  ) = receiverAccessors.last {
    descriptor.type.constructor.declarationDescriptor == it.first.descriptor
  }.second()

  private fun ScopeContext.parameterExpression(
    descriptor: ParameterDescriptor,
    injectable: CallableInjectable
  ): IrExpression =
    when (val containingDeclaration = descriptor.containingDeclaration) {
      is ClassDescriptor -> receiverExpression(descriptor)
      is ClassConstructorDescriptor -> DeclarationIrBuilder(irCtx, symbol)
        .irGet(
          injectable.type.toIrType(irCtx, localDeclarations, ctx).typeOrNull!!,
          containingDeclaration.irConstructor(ctx, irCtx, localDeclarations)
            .allParameters
            .single { it.name == descriptor.name }
            .symbol
        )
      is FunctionDescriptor -> DeclarationIrBuilder(irCtx, symbol)
        .irGet(
          injectable.type.toIrType(irCtx, localDeclarations, ctx).typeOrNull!!,
          (parameterMap[descriptor] ?: containingDeclaration.irFunction(ctx, irCtx, localDeclarations)
            .let { function ->
              function.allParameters
                .filter { it != function.dispatchReceiverParameter }
            }
            .singleOrNull { it.descriptor.injektIndex() == descriptor.injektIndex() })
            ?.symbol
            ?: error("")
        )
      is PropertyDescriptor -> DeclarationIrBuilder(irCtx, symbol)
        .irGet(
          injectable.type.toIrType(irCtx, localDeclarations, ctx).typeOrNull!!,
          parameterMap[descriptor]?.symbol ?:
          if (descriptor.injektIndex() == EXTENSION_RECEIVER_INDEX)
            containingDeclaration.irProperty(ctx, irCtx, localDeclarations)
              .getter!!.extensionReceiverParameter!!.symbol
          else
            containingDeclaration.irProperty(ctx, irCtx, localDeclarations)
              .getter!!.valueParameters[descriptor.injektIndex()].symbol
        )
      else -> error("Unexpected parent $descriptor $containingDeclaration")
    }

  private fun IrFunctionAccessExpression.fillTypeParameters(callable: CallableRef) {
    callable
      .typeArguments
      .values
      .forEachIndexed { index, typeArgument ->
        putTypeArgument(index, typeArgument.toIrType(irCtx, localDeclarations, ctx).typeOrNull)
      }
  }

  private fun ScopeContext.variableExpression(
    descriptor: VariableDescriptor,
    injectable: CallableInjectable
  ): IrExpression = if (descriptor is LocalVariableDescriptor && descriptor.isDelegated) {
    val localFunction = localDeclarations.localFunctions.single { candidateFunction ->
      candidateFunction.descriptor
        .safeAs<LocalVariableAccessorDescriptor.Getter>()
        ?.correspondingVariable == descriptor
    }
    DeclarationIrBuilder(irCtx, symbol)
      .irCall(
        localFunction.symbol,
        injectable.type.toIrType(irCtx, localDeclarations, ctx).typeOrNull!!
      )
  } else {
    DeclarationIrBuilder(irCtx, symbol)
      .irGet(
        injectable.type.toIrType(irCtx, localDeclarations, ctx).typeOrNull!!,
        localDeclarations.localVariables.single { it.descriptor == descriptor }.symbol
      )
  }

  private val receiverAccessors = mutableListOf<Pair<IrClass, () -> IrExpression>>()

  override fun visitClassNew(declaration: IrClass): IrStatement {
    receiverAccessors.push(
      declaration to {
        DeclarationIrBuilder(irCtx, declaration.symbol)
          .irGet(declaration.thisReceiver!!)
      }
    )
    val result = super.visitClassNew(declaration)
    receiverAccessors.pop()
    return result
  }

  override fun visitFunctionNew(declaration: IrFunction): IrStatement {
    val dispatchReceiver = declaration.dispatchReceiverParameter?.type?.classOrNull?.owner
    if (dispatchReceiver != null) {
      receiverAccessors.push(
        dispatchReceiver to {
          DeclarationIrBuilder(irCtx, declaration.symbol)
            .irGet(declaration.dispatchReceiverParameter!!)
        }
      )
    }
    val extensionReceiver = declaration.extensionReceiverParameter?.type?.classOrNull?.owner
    if (extensionReceiver != null) {
      receiverAccessors.push(
        extensionReceiver to {
          DeclarationIrBuilder(irCtx, declaration.symbol)
            .irGet(declaration.extensionReceiverParameter!!)
        }
      )
    }
    val result = super.visitFunctionNew(declaration)
    if (dispatchReceiver != null) receiverAccessors.pop()
    if (extensionReceiver != null) receiverAccessors.pop()
    return result
  }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression

    val graph = irCtx.bindingContext[
        InjektWritableSlices.INJECTION_GRAPH,
        SourcePosition(currentFile.fileEntry.name, result.startOffset, result.endOffset)
    ] ?: return result

    // some ir transformations reuse the start and end offsets
    // we ensure that were not transforming wrong calls
    if (!expression.symbol.owner.isPropertyAccessor &&
      expression.symbol.owner.descriptor.containingDeclaration
        .safeAs<ClassDescriptor>()
        ?.defaultType
        ?.isFunctionOrSuspendFunctionType != true &&
      graph.callee.callable.fqNameSafe != result.symbol.owner.descriptor.fqNameSafe)
      return result

    return DeclarationIrBuilder(irCtx, result.symbol)
      .irBlock {
        val graphContext = GraphContext(graph, result.startOffset)
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
