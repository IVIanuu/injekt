/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, ObsoleteDescriptorBasedAPI::class, FirIncompatiblePluginAPI::class,
  SymbolInternals::class, UnsafeDuringIrConstructionAPI::class
)

package com.ivianuu.injekt.compiler.ir

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import kotlin.collections.*

class InjectCallTransformer(
  private val context: IrPluginContext,
  private val cache: InjektCache
) : IrElementTransformerVoidWithContext() {
  private inner class RootContext(val result: InjectionResult.Success) {
    val statements = mutableListOf<IrStatement>()

    val highestScope = mutableMapOf<ResolutionResult.Success.Value, InjectablesScope>()
    val usages = buildMap<Any, MutableSet<InjectableRequest>> {
      fun ResolutionResult.Success.collectUsagesRecursive(request: InjectableRequest) {
        if (this !is ResolutionResult.Success.Value) return
        getOrPut(usageKey(this@RootContext)) { mutableSetOf() } += request
        dependencyResults.forEach { it.value.collectUsagesRecursive(it.key) }
      }

      result.results.forEach { it.value.collectUsagesRecursive(it.key) }
    }

    fun mapScopeIfNeeded(scope: InjectablesScope) =
      if (scope in result.scope.allScopes) result.scope else scope
  }

  private fun ResolutionResult.Success.Value.usageKey(ctx: RootContext): Any =
    listOf(candidate::class, candidate.type, highestScope(ctx))
  
  private fun ResolutionResult.Success.Value.highestScope(ctx: RootContext): InjectablesScope =
    ctx.highestScope.getOrPut(this) {
      val anchorScopes = mutableSetOf<InjectablesScope>()

      fun collectScopesRecursive(result: ResolutionResult.Success.Value) {
        if (result.candidate is CallableInjectable)
          anchorScopes += result.candidate.ownerScope
        for (dependency in result.dependencyResults.values)
          if (dependency is ResolutionResult.Success.Value)
            collectScopesRecursive(dependency)
      }

      collectScopesRecursive(this)

      scope.allScopes
        .sortedBy { it.nesting }
        .firstOrNull { candidateScope ->
          anchorScopes.all {
            it in candidateScope.allScopes ||
                scope in candidateScope.allScopes
          }
        } ?: scope
    }

  private inner class ScopeContext(
    val parent: ScopeContext?,
    val rootContext: RootContext,
    val scope: InjectablesScope,
    val irScope: Scope
  ) {
    val irBuilder = DeclarationIrBuilder(context, irScope.scopeOwnerSymbol)
    val functionWrappedExpressions = mutableMapOf<ConeKotlinType, ScopeContext.() -> IrExpression>()
    val statements =
      if (scope == rootContext.result.scope) rootContext.statements else mutableListOf()
    val lambdaParametersMap: MutableMap<FirValueParameterSymbol, IrValueParameterSymbol> =
      parent?.lambdaParametersMap ?: mutableMapOf()

    fun findScopeContext(scopeToFind: InjectablesScope): ScopeContext {
      val finalScope = rootContext.mapScopeIfNeeded(scopeToFind)
      if (finalScope == scope) return this@ScopeContext
      return parent!!.findScopeContext(finalScope)
    }

    fun expressionFor(result: ResolutionResult.Success.Value): IrExpression {
      val scopeContext = findScopeContext(result.scope)
      return scopeContext.expressionForImpl(result)
    }

    private fun expressionForImpl(
      result: ResolutionResult.Success.Value
    ): IrExpression = wrapExpressionInFunctionIfNeeded(result) {
      val expression = when (val candidate = result.candidate) {
        is CallableInjectable -> callableExpression(result, candidate)
        is LambdaInjectable -> lambdaExpression(result, candidate)
        is ListInjectable -> listExpression(result, candidate)
        is TypeKeyInjectable -> typeKeyExpression(result, candidate)
      }

      if (!result.candidate.type.isNullable ||
          result.dependencyResults.keys.firstOrNull()?.parameterIndex != DISPATCH_RECEIVER_INDEX
      ) expression
      else irBuilder.run {
        irBlock {
          expression as IrFunctionAccessExpression
          val tmpDispatchReceiver = irTemporary(expression.dispatchReceiver!!)
          expression.dispatchReceiver = irGet(tmpDispatchReceiver)

          +irIfNull(
            expression.type,
            irGet(tmpDispatchReceiver),
            irNull(),
            expression
          )
        }
      }
    }
  }

  private fun IrFunctionAccessExpression.inject(
    ctx: ScopeContext,
    results: Map<InjectableRequest, ResolutionResult.Success>
  ) {
    for ((request, result) in results) {
      if (result !is ResolutionResult.Success.Value) continue
      val expression = ctx.expressionFor(result)
      when (request.parameterIndex) {
        DISPATCH_RECEIVER_INDEX -> dispatchReceiver = expression
        EXTENSION_RECEIVER_INDEX -> extensionReceiver = expression
        else -> putValueArgument(request.parameterIndex, expression)
      }
    }
  }

  private fun IrFunctionAccessExpression.fillTypeParameters(callable: InjektCallable, scope: ScopeContext) {
    callable
      .typeArguments
      .values
      .forEachIndexed { index, typeArgument ->
        putTypeArgument(index, typeArgument.toIrType(scope).typeOrNull)
      }
  }

  private fun ResolutionResult.Success.Value.shouldWrap(ctx: RootContext): Boolean =
    dependencyResults.isNotEmpty() && ctx.usages[usageKey(ctx)]!!.size > 1

  private fun ScopeContext.wrapExpressionInFunctionIfNeeded(
    result: ResolutionResult.Success.Value,
    unwrappedExpression: () -> IrExpression
  ): IrExpression = if (!result.shouldWrap(rootContext)) unwrappedExpression()
  else with(result.safeAs<ResolutionResult.Success.Value>()
    ?.highestScope(rootContext)?.let { findScopeContext(it) } ?: this) {
    functionWrappedExpressions.getOrPut(result.candidate.type) expression@ {
      val function = IrFactoryImpl.buildFun {
        origin = IrDeclarationOrigin.DEFINED
        name = irScope.inventNameForTemporary("function").asNameId()
        returnType = result.candidate.type.toIrType(this@with).typeOrNull!!
        visibility = DescriptorVisibilities.LOCAL
      }.apply {
        parent = irScope.getLocalDeclarationParent()

        body = DeclarationIrBuilder(context, symbol).run {
          irBlockBody {
            +irReturn(unwrappedExpression())
          }
        }

        statements += this
      }

      return@expression {
        irBuilder.irCall(
          function.symbol,
          result.candidate.type.toIrType(this).typeOrNull!!
        )
      }
    }
  }.invoke(this)

  private fun ScopeContext.lambdaExpression(
    result: ResolutionResult.Success.Value,
    injectable: LambdaInjectable
  ): IrExpression = irBuilder
    .irLambda(injectable.type.toIrType(this).typeOrNull!!) { function ->
      val dependencyResult = result.dependencyResults.values.single()
      val dependencyScopeContext = if (injectable.dependencyScope == this@lambdaExpression.scope) null
      else ScopeContext(
        this@lambdaExpression, rootContext,
        injectable.dependencyScope, scope
      )

      fun ScopeContext.createExpression(): IrExpression {
        for ((index, parameter) in injectable.valueParameterSymbols.withIndex())
          lambdaParametersMap[parameter] = function.valueParameters[index].symbol
        return expressionFor(dependencyResult.cast())
          .also {
            injectable.valueParameterSymbols.forEach {
              lambdaParametersMap -= it
            }
          }
      }

      irBlock {
        +(dependencyScopeContext?.run { createExpression() } ?: createExpression())
          .also { dependencyScopeContext?.statements?.forEach { +it } }
      }
    }

  private val mutableListOf = context.referenceFunctions(
    CallableId(FqName("kotlin.collections"), "mutableListOf".asNameId())
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
    result: ResolutionResult.Success.Value,
    injectable: ListInjectable
  ): IrExpression = irBuilder.irBlock {
    val tmpList = irTemporary(
      irCall(mutableListOf)
        .apply {
          putTypeArgument(0, injectable.singleElementType.toIrType(this@listExpression).typeOrNull)
        }
    )

    result.dependencyResults.values.forEach { dependencyResult ->
      dependencyResult as ResolutionResult.Success.Value
      +irCall(
        if (dependencyResult.candidate.type.isSubtypeOf(injectable.collectionElementType, cache.session))
          listAddAll else listAdd
      ).apply {
        dispatchReceiver = irGet(tmpList)
        putValueArgument(0, expressionFor(dependencyResult))
      }
    }

    +irGet(tmpList)
  }

  private val typeKey = context.referenceClass(InjektFqNames.TypeKey)
  private val typeKeyValue = typeKey?.owner?.properties
    ?.single { it.name.asString() == "value" }
  private val typeKeyConstructor = typeKey?.constructors?.single()
  private val stringPlus = context.irBuiltIns.stringClass
    .functions
    .map { it.owner }
    .first { it.name.asString() == "plus" }

  private fun ScopeContext.typeKeyExpression(
    result: ResolutionResult.Success.Value,
    injectable: TypeKeyInjectable
  ): IrExpression = irBuilder.run {
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

    injectable.type.typeArguments.single().render(
      renderType = { typeToRender ->
        if (typeToRender !is ConeTypeParameterType) true else {
          appendTypeParameterExpression(
            irCall(typeKeyValue!!.getter!!).apply {
              val dependencyResult = result.dependencyResults.values.single {
                 it.cast<ResolutionResult.Success.Value>()
                   .candidate.type.typeArguments.single() == typeToRender
              }
              dispatchReceiver = expressionFor(dependencyResult.cast())
            }
          )
          false
        }
      },
      append = { appendToCurrentString(it) }
    )

    commitCurrentString()

    val stringExpression = if (expressions.size == 1) expressions.single()
    else expressions.reduce { acc, expression ->
      irCall(stringPlus).apply {
        dispatchReceiver = acc
        putValueArgument(0, expression)
      }
    }

    irCall(typeKeyConstructor!!).apply {
      putTypeArgument(0, injectable.type.typeArguments.single().toIrType(this@typeKeyExpression).cast())
      putValueArgument(0, stringExpression)
    }
  }

  private fun ScopeContext.callableExpression(
    result: ResolutionResult.Success.Value,
    injectable: CallableInjectable
  ): IrExpression = when (injectable.callable.callable) {
    is FirValueParameterSymbol -> irBuilder.irGet(
      injectable.type.toIrType(this).typeOrNull!!,
      injectable.callable.callable.toIrValueParameterSymbol(this)
    )
    is FirConstructorSymbol -> {
      val classSymbol = injectable.callable.type.toRegularClassSymbol(cache.session)!!
      if (classSymbol.classKind == ClassKind.OBJECT)
        irBuilder.irGetObject(classSymbol.toIrClassSymbol(this))
      else
        functionExpression(result, injectable, injectable.callable.callable.toIrFunctionSymbol(this))
    }
    is FirPropertySymbol -> {
      if (injectable.callable.callable.isLocal) {
        if (injectable.callable.callable.fir.getter != null)
          irBuilder.irCall(
            injectable.callable.callable.fir.getter!!.symbol.toIrFunctionSymbol(this),
            injectable.type.toIrType(this).typeOrNull!!
          )
        else
          irBuilder.irGet(
            injectable.type.toIrType(this).typeOrNull!!,
            localVariables.last { it.name == injectable.callable.callable.name }.symbol
          )
      } else
        functionExpression(result, injectable, injectable.callable.callable.toIrPropertySymbol(this).owner.getter!!.symbol)
    }
    is FirFunctionSymbol<*> -> functionExpression(result, injectable, injectable.callable.callable.toIrFunctionSymbol(this))
    else -> throw AssertionError("Unexpected callable $result $injectable ${injectable.callable}")
  }

  private fun ScopeContext.functionExpression(
    result: ResolutionResult.Success.Value,
    injectable: CallableInjectable,
    functionSymbol: IrFunctionSymbol
  ): IrExpression = irBuilder
    .irCall(functionSymbol, injectable.type.toIrType(this).typeOrNull!!)
    .apply {
      fillTypeParameters(injectable.callable, this@functionExpression)
      inject(this@functionExpression, result.dependencyResults)
    }

  private fun ConeTypeProjection.toIrType(scope: ScopeContext): IrTypeArgument {
    if (isStarProjection) return IrStarProjectionImpl
    else if (this is ConeKotlinTypeProjectionOut) return this.type.toIrType(scope)
    else if (this is ConeKotlinTypeProjectionIn) return this.type.toIrType(scope)
    this as ConeLookupTagBasedType
    return IrSimpleTypeImpl(
      if (this is ConeTypeParameterType) {
        when (val container = lookupTag.typeParameterSymbol.containingDeclarationSymbol) {
          is FirFunctionSymbol<*> -> container.toIrFunctionSymbol(scope).owner.typeParameters
          is FirClassSymbol<*> -> container.toIrClassSymbol(scope).owner.typeParameters
          else -> throw AssertionError("Unexpected container $container")
        }.single { it.descriptor.name == lookupTag.name }.symbol
      } else when (val classLikeSymbol = lookupTag.toSymbol(cache.session)) {
        is FirRegularClassSymbol -> classLikeSymbol.toIrClassSymbol(scope)
        else ->
          throw IllegalArgumentException("Unexpected class symbol: $classLikeSymbol")
      },
      isMarkedNullable,
      typeArguments.map { it.toIrType(scope) },
      emptyList()
    )
  }

  private fun FirClassSymbol<*>.toIrClassSymbol(scope: ScopeContext): IrClassSymbol =
    context.referenceClass(classId)!!

  private fun FirFunctionSymbol<*>.toIrFunctionSymbol(scope: ScopeContext): IrFunctionSymbol =
    when (this) {
      is FirConstructorSymbol -> context.referenceConstructors(callableId.classId!!).single()
      else -> context.referenceFunctions(callableId).single()
    }

  private fun FirPropertySymbol.toIrPropertySymbol(scope: ScopeContext): IrPropertySymbol =
    context.referenceProperties(callableId).single()

  private fun FirValueParameterSymbol.toIrValueParameterSymbol(scope: ScopeContext): IrValueParameterSymbol =
    scope.lambdaParametersMap[this] ?: containingFunctionSymbol.toIrFunctionSymbol(scope)
      .owner.valueParameters.single { it.name == name }.symbol

  private fun IrBuilderWithScope.irLambda(
    type: IrType,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET,
    body: IrBuilderWithScope.(IrFunction) -> IrExpression,
  ): IrExpression {
    type as IrSimpleType
    val returnType = type.arguments.last().typeOrNull!!

    val lambda = IrFactoryImpl.buildFun {
      origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
      name = Name.special("<anonymous>")
      this.returnType = returnType
      visibility = DescriptorVisibilities.LOCAL
    }.apply {
      parent = scope.getLocalDeclarationParent()
      type.arguments.forEachIndexed { index, typeArgument ->
        if (index < type.arguments.lastIndex) {
          addValueParameter(
            scope.inventNameForTemporary("p"),
            typeArgument.typeOrNull!!
          )
        }
      }
      annotations = annotations + type.annotations.map {
        it.deepCopyWithSymbols()
      }
      this.body = DeclarationIrBuilder(context, symbol).run {
        irBlockBody {
          +irReturn(body(this, this@apply))
        }
      }
    }

    return IrFunctionExpressionImpl(
      startOffset = startOffset,
      endOffset = endOffset,
      type = type,
      function = lambda,
      origin = IrStatementOrigin.LAMBDA
    )
  }

  private val localVariables = mutableListOf<IrVariable>()
  override fun visitVariable(declaration: IrVariable): IrStatement {
    localVariables += declaration
    return super.visitVariable(declaration)
  }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression

    val injectionResult = cache.cachedOrNull<_, InjectionResult.Success>(
      INJECTION_RESULT_KEY,
      SourcePosition(currentFile.fileEntry.name, result.startOffset, result.endOffset)
    ) ?: return result

    return DeclarationIrBuilder(context, result.symbol)
      .irBlock {
        val rootContext = RootContext(injectionResult)
        try {
          ScopeContext(
            parent = null,
            rootContext = rootContext,
            scope = injectionResult.scope,
            irScope = scope
          ).run {
            val replacement = expressionFor(injectionResult.results.values.single().cast())
            rootContext.statements.forEach { +it }
            +replacement
          }
        } catch (e: Throwable) {
          throw RuntimeException("Wtf ${expression.dump()}", e)
        }
      }
  }
}
