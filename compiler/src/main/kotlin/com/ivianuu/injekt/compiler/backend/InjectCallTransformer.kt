/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, UnsafeDuringIrConstructionAPI::class)

package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
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
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.io.*
import kotlin.collections.*

class InjectCallTransformer(
  private val compilationDeclarations: CompilationDeclarations,
  private val irCtx: IrPluginContext,
  private val ctx: InjektContext
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
    val irBuilder = DeclarationIrBuilder(irCtx, irScope.scopeOwnerSymbol)
    val functionWrappedExpressions = mutableMapOf<InjektType, ScopeContext.() -> IrExpression>()
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
      }

      if (!result.candidate.type.isNullableType ||
          result.dependencyResults.keys.firstOrNull()?.parameterIndex != DISPATCH_RECEIVER_INDEX) expression
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
        else -> putValueArgument(
          symbol.owner
            .valueParameters
            .first { it.injektIndex() == request.parameterIndex }
            .index,
          expression
        )
      }
    }
  }

  private fun IrFunctionAccessExpression.fillTypeParameters(callable: InjektCallable) {
    callable
      .typeArguments
      .values
      .forEachIndexed { index, typeArgument ->
        putTypeArgument(index, typeArgument.toIrType().typeOrNull)
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
        returnType = result.candidate.type.toIrType().typeOrNull!!
        visibility = DescriptorVisibilities.LOCAL
      }.apply {
        parent = irScope.getLocalDeclarationParent()

        body = DeclarationIrBuilder(irCtx, symbol).run {
          irBlockBody {
            +irReturn(unwrappedExpression())
          }
        }

        statements += this
      }

      return@expression {
        irBuilder.irCall(
          function.symbol,
          result.candidate.type.toIrType().typeOrNull!!
        )
      }
    }
  }.invoke(this)

  private fun ScopeContext.lambdaExpression(
    result: ResolutionResult.Success.Value,
    injectable: LambdaInjectable
  ): IrExpression {
    val type = injectable.type.toIrType().typeOrNull.cast<IrSimpleType>()
    val returnType = type.arguments.last().typeOrNull!!

    val lambda = IrFactoryImpl.buildFun {
      origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
      name = Name.special("<anonymous>")
      this.returnType = returnType
      visibility = DescriptorVisibilities.LOCAL
    }.apply {
      parent = irBuilder.scope.getLocalDeclarationParent()
      annotations = annotations + type.annotations.map {
        it.deepCopyWithSymbols()
      }

      val irBuilder = DeclarationIrBuilder(irCtx, symbol)

      type.arguments.forEachIndexed { index, typeArgument ->
        if (index < type.arguments.lastIndex) {
          addValueParameter(
            irBuilder.scope.inventNameForTemporary("p"),
            typeArgument.typeOrNull!!
          )
        }
      }

      val dependencyResult = result.dependencyResults.values.single()
      val dependencyScopeContext = if (injectable.dependencyScope == this@lambdaExpression.scope) null
      else ScopeContext(
        this@lambdaExpression, rootContext,
        injectable.dependencyScope, irBuilder.scope
      )

      fun ScopeContext.createExpression(): IrExpression {
        for ((index, parameter) in injectable.valueParameterSymbols.withIndex())
          lambdaParametersMap[parameter] = valueParameters[index].symbol
        return expressionFor(dependencyResult.cast())
          .also {
            injectable.valueParameterSymbols.forEach {
              lambdaParametersMap -= it
            }
          }
      }

      this.body = irBuilder.run {
        irBlockBody {
          +irReturn(
            (dependencyScopeContext?.run { createExpression() } ?: createExpression())
              .also { dependencyScopeContext?.statements?.forEach { +it } }
          )
        }
      }
    }

    return IrFunctionExpressionImpl(
      startOffset = UNDEFINED_OFFSET,
      endOffset = UNDEFINED_OFFSET,
      type = type,
      function = lambda,
      origin = IrStatementOrigin.LAMBDA
    )
  }

  private val mutableListOf = irCtx.referenceFunctions(
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
          putTypeArgument(0, injectable.singleElementType.toIrType().typeOrNull)
        }
    )

    result.dependencyResults.values.forEach { dependencyResult ->
      dependencyResult as ResolutionResult.Success.Value
      +irCall(
        if (dependencyResult.candidate.type.isSubTypeOf(injectable.collectionElementType, ctx))
          listAddAll else listAdd
      ).apply {
        dispatchReceiver = irGet(tmpList)
        putValueArgument(0, expressionFor(dependencyResult))
      }
    }

    +irGet(tmpList)
  }

  private fun ScopeContext.callableExpression(
    result: ResolutionResult.Success.Value,
    injectable: CallableInjectable
  ): IrExpression = if (injectable.callable.type.unwrapTags().classifier.isObject)
    objectExpression(injectable.callable.type.unwrapTags())
  else when (injectable.callable.callable) {
    //is ReceiverParameterDescriptor -> parameterExpression(injectable.callable.callable, injectable)
    //is ValueParameterDescriptor -> parameterExpression(injectable.callable.callable, injectable)
    //is LocalVariableDescriptor -> localVariableExpression(injectable.callable.callable, injectable)
    else -> functionExpression(result, injectable, injectable.callable.symbol!!.cast())
  }

  private fun ScopeContext.objectExpression(type: InjektType): IrExpression =
    irBuilder.irGetObject(irCtx.referenceClass(type.classifier.classId!!)!!)

  private fun ScopeContext.functionExpression(
    result: ResolutionResult.Success.Value,
    injectable: CallableInjectable,
    firFunctionSymbol: FirFunctionSymbol<*>
  ): IrExpression = irBuilder.irCall(
    firFunctionSymbol.toIrCallableSymbol(),
    injectable.type.toIrType().typeOrNull!!
  )
    .apply {
      fillTypeParameters(injectable.callable)
      inject(this@functionExpression, result.dependencyResults)
    }

  private fun ScopeContext.receiverExpression(
    descriptor: ParameterDescriptor
  ): IrExpression = irBuilder.run {
    allScopes.reversed().firstNotNullOfOrNull { scope ->
      val element = scope.irElement
      when {
        element is IrClass &&
            element.symbol.uniqueKey(ctx) == descriptor.type.constructor.declarationDescriptor!!.uniqueKey(ctx) ->
          irGet(element.thisReceiver!!)
        element is IrFunction &&
            element.dispatchReceiverParameter?.type?.uniqueTypeKey() == descriptor.type.uniqueTypeKey() ->
          irGet(element.dispatchReceiverParameter!!)
        element is IrProperty &&
            allScopes.getOrNull(allScopes.indexOf(scope) + 1)?.irElement !is IrField &&
            element.parentClassOrNull?.symbol?.uniqueKey(ctx) ==
            descriptor.type.constructor.declarationDescriptor!!.uniqueKey(ctx) ->
          irGet(element.getter!!.dispatchReceiverParameter!!)
        else -> null
      }
    } ?: error("unexpected $descriptor")
  }

  /*private fun ScopeContext.parameterExpression(
    descriptor: ParameterDescriptor,
    injectable: CallableInjectable
  ): IrExpression =
    when (val containingDeclaration = descriptor.containingDeclaration) {
      is ClassDescriptor -> receiverExpression(descriptor)
      is CallableDescriptor -> irBuilder.irGet(
        injectable.type.toIrType().typeOrNull!!,
        lambdaParametersMap[descriptor] ?: (containingDeclaration.irCallable()
          .allParameters
          .single { it.injektIndex() == descriptor.injektIndex() })
          .symbol
      )
      else -> error("Unexpected parent $descriptor $containingDeclaration")
    }*/

  /*private fun ScopeContext.localVariableExpression(
    descriptor: LocalVariableDescriptor,
    injectable: CallableInjectable
  ): IrExpression = if (descriptor.getter != null) irBuilder.irCall(
    descriptor.getter!!.irCallable().symbol,
    injectable.type.toIrType().typeOrNull!!
  )
  else irBuilder.irGet(
    injectable.type.toIrType().typeOrNull!!,
    compilationDeclarations.variables
      .singleOrNull { it.uniqueKey(ctx) == descriptor.uniqueKey(ctx) }
      ?.symbol
      ?: error("Couldn't find ${descriptor.uniqueKey(ctx)} in ${compilationDeclarations.variables.map { it.uniqueKey(ctx) }}")
  )*/

  private fun FirCallableSymbol<*>.toIrCallableSymbol(): IrFunctionSymbol = when (this) {
    is FirConstructorSymbol -> compilationDeclarations.constructors
      .singleOrNull { it.uniqueKey(ctx) == uniqueKey(ctx) }
      ?: irCtx.referenceConstructors(resolvedReturnType.classId!!)
        .singleOrNull { it.uniqueKey(ctx) == uniqueKey(ctx) }
      ?: error("Nope couldn't find ${uniqueKey(ctx)} in ${compilationDeclarations.constructors.map { it.uniqueKey(ctx) }}")
    is FirFunctionSymbol<*> -> compilationDeclarations.functions.singleOrNull {
        it.uniqueKey(ctx) == uniqueKey(ctx)
      } ?: irCtx.referenceFunctions(callableId)
        .singleOrNull { it.uniqueKey(ctx) == uniqueKey(ctx) }
    ?: error("Nope couldn't find ${uniqueKey(ctx)} in ${irCtx.referenceFunctions(callableId).map { it.uniqueKey(ctx) }}")
    is FirPropertySymbol -> (compilationDeclarations.properties
      .singleOrNull { it.uniqueKey(ctx) == uniqueKey(ctx) }
      ?: irCtx.referenceProperties(callableId)
      .singleOrNull { it.uniqueKey(ctx) == uniqueKey(ctx) })
      ?.owner
      ?.getter
      ?.symbol
      ?: error("Nope couldn't find ${uniqueKey(ctx)} in ${irCtx.referenceProperties(callableId).map { it.uniqueKey(ctx) }}")
    else -> throw AssertionError("Unexpected callable $this")
  }

  private fun InjektType.toIrType(): IrTypeArgument {
    if (isStarProjection) return IrStarProjectionImpl
    return when {
      classifier.isTag -> arguments.last().toIrType()
        .typeOrNull!!
        .cast<IrSimpleType>()
        .let { type ->
          val tagConstructor = irCtx.referenceClass(classifier.classId!!)!!
            .constructors.single()
          IrSimpleTypeImpl(
            type.originalKotlinType,
            type.classifier,
            type.nullability,
            type.arguments,
            listOf(
              DeclarationIrBuilder(irCtx, tagConstructor)
                .irCall(
                  tagConstructor,
                  tagConstructor.owner.returnType
                    .classifierOrFail
                    .typeWith(
                      arguments.dropLast(1)
                        .map {
                          it.toIrType().typeOrNull ?: irCtx.irBuiltIns.anyNType
                        }
                    )
                ).apply {
                  tagConstructor.owner.typeParameters.indices
                    .forEach { index ->
                      putTypeArgument(
                        index,
                        arguments[index].toIrType().typeOrNull!!
                      )
                    }
                }
            ) + type.annotations,
            type.abbreviation
          )
        }
      else -> {
        val key = classifier.key
        val irClassifier = compilationDeclarations.classes.singleOrNull {
          it.uniqueKey(ctx) == key
        }
          ?: classifier.symbol
            .safeAs<FirRegularClassSymbol>()
            ?.let { irCtx.referenceClass(it.classId) }
          ?: classifier.symbol.safeAs<FirTypeParameterSymbol>()
            ?.let { firTypeParameterSymbol ->
              firTypeParameterSymbol.containingDeclarationSymbol
                .safeAs<FirFunctionSymbol<*>>()
                ?.let { containingFunctionSymbol ->
                  compilationDeclarations.functions
                    .filter { it.uniqueKey(ctx) == containingFunctionSymbol.uniqueKey(ctx) } +
                      irCtx.referenceFunctions(containingFunctionSymbol.callableId)
                }
                ?.flatMap { it.owner.typeParameters }
                ?.singleOrNull { it.symbol.uniqueKey(ctx) == key }
                ?.symbol
                ?: firTypeParameterSymbol.containingDeclarationSymbol
                  .safeAs<FirPropertySymbol>()
                  ?.let { containingPropertySymbol ->
                    compilationDeclarations.properties
                      .filter { it.uniqueKey(ctx) == containingPropertySymbol.uniqueKey(ctx) } +
                        irCtx.referenceProperties(containingPropertySymbol.callableId)
                  }
                  ?.flatMap { it.owner.getter!!.typeParameters }
                  ?.singleOrNull { it.symbol.uniqueKey(ctx) == key }
                  ?.symbol
                ?: firTypeParameterSymbol.containingDeclarationSymbol
                  .safeAs<FirRegularClassSymbol>()
                  ?.let { containingClassSymbol ->
                    compilationDeclarations.classes
                      .filter { it.uniqueKey(ctx) == containingClassSymbol.uniqueKey(ctx) } +
                        listOfNotNull(irCtx.referenceClass(containingClassSymbol.classId))
                  }
                  ?.flatMap { it.owner.typeParameters }
                  ?.singleOrNull { it.symbol.uniqueKey(ctx) == key }
                  ?.symbol
                  ?: firTypeParameterSymbol.containingDeclarationSymbol
                    .safeAs<FirTypeAliasSymbol>()
                    ?.let { containingTypeAlias ->
                      compilationDeclarations.typeAliases
                        .filter { it.uniqueKey(ctx) == containingTypeAlias.uniqueKey(ctx) } +
                          listOfNotNull(irCtx.referenceTypeAlias(containingTypeAlias.classId))
                    }
                    ?.flatMap { it.owner.typeParameters }
                    ?.singleOrNull { it.symbol.uniqueKey(ctx) == key }
                  ?.symbol
            } ?: error("Could not get for $classifier $key")
        IrSimpleTypeImpl(
          irClassifier,
          isMarkedNullable,
          arguments.map { it.toIrType() },
          emptyList()
        )
      }
    }
  }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression

    val injectionResult = ctx.cachedOrNull<_, InjectionResult.Success>(
      INJECTION_RESULT_KEY,
      SourcePosition(currentFile.fileEntry.name, result.startOffset, result.endOffset)
    ) ?: return result

    return DeclarationIrBuilder(irCtx, result.symbol)
      .irBlock {
        val rootContext = RootContext(injectionResult)
        try {
          ScopeContext(
            parent = null,
            rootContext = rootContext,
            scope = injectionResult.scope,
            irScope = scope
          ).run { result.inject(this, injectionResult.results) }
        } catch (e: Throwable) {
          throw RuntimeException("Wtf ${expression.dump()}", e)
        }
        rootContext.statements.forEach { +it }
        +result
      }
  }
}

class CompilationDeclarations : IrElementTransformerVoid() {
  val classes = mutableSetOf<IrClassSymbol>()
  val constructors = mutableSetOf<IrConstructorSymbol>()
  val functions = mutableSetOf<IrFunctionSymbol>()
  val properties = mutableSetOf<IrPropertySymbol>()
  val typeAliases = mutableSetOf<IrTypeAliasSymbol>()
  val variables = mutableSetOf<IrVariableSymbol>()

  override fun visitClass(declaration: IrClass): IrStatement {
    classes += declaration.symbol
    return super.visitClass(declaration)
  }

  override fun visitConstructor(declaration: IrConstructor): IrStatement {
    constructors += declaration.symbol
    return super.visitConstructor(declaration)
  }

  override fun visitFunction(declaration: IrFunction): IrStatement {
    functions += declaration.symbol
    return super.visitFunction(declaration)
  }

  override fun visitProperty(declaration: IrProperty): IrStatement {
    properties += declaration.symbol
    return super.visitProperty(declaration)
  }

  override fun visitTypeAlias(declaration: IrTypeAlias): IrStatement {
    typeAliases += declaration.symbol
    return super.visitTypeAlias(declaration)
  }

  override fun visitVariable(declaration: IrVariable): IrStatement {
    variables += declaration.symbol
    return super.visitVariable(declaration)
  }
}
