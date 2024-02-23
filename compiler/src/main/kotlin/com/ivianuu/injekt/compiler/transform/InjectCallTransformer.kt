/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, ObsoleteDescriptorBasedAPI::class,
  FirIncompatiblePluginAPI::class)

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
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
import java.io.*
import kotlin.collections.*

class InjectCallTransformer(
  private val irCtx: IrPluginContext,
  private val ctx: Context
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
    val functionWrappedExpressions = mutableMapOf<TypeRef, ScopeContext.() -> IrExpression>()
    val statements =
      if (scope == rootContext.result.scope) rootContext.statements else mutableListOf()
    val parameterMap: MutableMap<ParameterDescriptor, IrValueParameter> =
      parent?.parameterMap ?: mutableMapOf()

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
            .first { it.descriptor.injektIndex() == request.parameterIndex }
            .index,
          expression
        )
      }
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
        returnType = result.candidate.type.toIrType(irCtx).typeOrNull!!
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
          result.candidate.type.toIrType(irCtx).typeOrNull!!
        )
      }
    }
  }.invoke(this)

  private fun ScopeContext.lambdaExpression(
    result: ResolutionResult.Success.Value,
    injectable: LambdaInjectable
  ): IrExpression = irBuilder.irLambda(injectable.type.toIrType(irCtx).typeOrNull!!) { function ->
      val dependencyResult = result.dependencyResults.values.single()
      val dependencyScopeContext = if (injectable.dependencyScope == this@lambdaExpression.scope) null
      else ScopeContext(
        this@lambdaExpression, rootContext,
        injectable.dependencyScope, scope
      )

      fun ScopeContext.createExpression(): IrExpression {
        for ((index, parameter) in injectable.parameterDescriptors.withIndex())
          parameterMap[parameter] = function.valueParameters[index]
        return expressionFor(dependencyResult.cast())
          .also {
            injectable.parameterDescriptors.forEach {
              parameterMap -= it
            }
          }
      }

      irBlock {
        +(dependencyScopeContext?.run { createExpression() } ?: createExpression())
          .also { dependencyScopeContext?.statements?.forEach { +it } }
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
    result: ResolutionResult.Success.Value,
    injectable: ListInjectable
  ): IrExpression = irBuilder.irBlock {
    val tmpList = irTemporary(
      irCall(mutableListOf)
        .apply {
          putTypeArgument(0, injectable.singleElementType.toIrType(irCtx).typeOrNull)
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
  ): IrExpression = when (injectable.callable.callable) {
    is ReceiverParameterDescriptor -> if (injectable.callable.type.unwrapTags().classifier.isObject)
      objectExpression(injectable.callable.type.unwrapTags())
    else parameterExpression(injectable.callable.callable, injectable)
    is ValueParameterDescriptor -> parameterExpression(injectable.callable.callable, injectable)
    is LocalVariableDescriptor -> localVariableExpression(injectable.callable.callable, injectable)
    else -> functionExpression(result, injectable, injectable.callable.callable)
  }

  private fun ScopeContext.objectExpression(type: TypeRef): IrExpression =
    irBuilder.irGetObject(irCtx.referenceClass(type.classifier.fqName)!!)

  private fun ScopeContext.functionExpression(
    result: ResolutionResult.Success.Value,
    injectable: CallableInjectable,
    descriptor: CallableDescriptor
  ): IrExpression = irBuilder.irCall(
    descriptor.irCallable(irCtx).symbol,
    injectable.type.toIrType(irCtx).typeOrNull!!
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
            element.descriptor == descriptor.type.constructor.declarationDescriptor ->
          irGet(element.thisReceiver!!)
        element is IrFunction &&
            element.dispatchReceiverParameter?.descriptor?.type?.constructor?.declarationDescriptor ==
            descriptor.type.constructor.declarationDescriptor ->
          irGet(element.dispatchReceiverParameter!!)
        element is IrProperty &&
            allScopes.getOrNull(allScopes.indexOf(scope) + 1)?.irElement !is IrField &&
            element.parentClassOrNull?.descriptor == descriptor.type.constructor.declarationDescriptor ->
          irGet(element.getter!!.dispatchReceiverParameter!!)
        else -> null
      }
    } ?: error("unexpected $descriptor")
  }

  private fun ScopeContext.parameterExpression(
    descriptor: ParameterDescriptor,
    injectable: CallableInjectable
  ): IrExpression =
    when (val containingDeclaration = descriptor.containingDeclaration) {
      is ClassDescriptor -> receiverExpression(descriptor)
      is CallableDescriptor -> irBuilder.irGet(
        injectable.type.toIrType(irCtx).typeOrNull!!,
        (parameterMap[descriptor] ?: containingDeclaration.irCallable(irCtx)
          .allParameters
          .single { it.descriptor.injektIndex() == descriptor.injektIndex() })
          .symbol
      )
      else -> error("Unexpected parent $descriptor $containingDeclaration")
    }

  private fun IrFunctionAccessExpression.fillTypeParameters(callable: CallableRef) {
    callable
      .typeArguments
      .values
      .forEachIndexed { index, typeArgument ->
        putTypeArgument(index, typeArgument.toIrType(irCtx).typeOrNull)
      }
  }

  private fun ScopeContext.localVariableExpression(
    descriptor: LocalVariableDescriptor,
    injectable: CallableInjectable
  ): IrExpression = if (descriptor.getter != null) irBuilder.irCall(
    irCtx.symbolTable.descriptorExtension.referenceSimpleFunction(descriptor.getter!!),
    injectable.type.toIrType(irCtx).typeOrNull!!
  )
  else irBuilder.irGet(
    injectable.type.toIrType(irCtx).typeOrNull!!,
    localVariables.single { it.descriptor == descriptor }.symbol
  )

  private fun ClassDescriptor.irClass(irCtx: IrPluginContext): IrClass =
    irCtx.symbolTable.descriptorExtension.referenceClass(this).ensureBound(irCtx).owner

  private fun CallableDescriptor.irCallable(irCtx: IrPluginContext): IrFunction =
    if (this is PropertyDescriptor)
      irCtx.symbolTable.descriptorExtension.referenceProperty(this).ensureBound(irCtx).owner.getter!!
    else irCtx.symbolTable.referenceFunction(this).ensureBound(irCtx).owner

  private fun TypeRef.toIrType(irCtx: IrPluginContext): IrTypeArgument {
    if (isStarProjection) return IrStarProjectionImpl
    return when {
      classifier.isTag -> arguments.last().toIrType(irCtx)
        .typeOrNull!!
        .cast<IrSimpleType>()
        .let { type ->
          val tagConstructor = irCtx.referenceClass(classifier.fqName)!!.constructors.single()
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
                        .map { it.toIrType(irCtx).typeOrNull ?: irCtx.irBuiltIns.anyNType }
                    )
                ).apply {
                  tagConstructor.owner.typeParameters.indices
                    .forEach { index ->
                      putTypeArgument(index, arguments[index].toIrType(irCtx).typeOrNull!!)
                    }
                }
            ) + type.annotations,
            type.abbreviation
          )
        }
      else -> IrSimpleTypeImpl(
        if (classifier.isTypeParameter) {
          when (val container = classifier.descriptor!!.containingDeclaration) {
            is CallableDescriptor -> container.irCallable(irCtx).typeParameters
            is ClassDescriptor -> container.irClass(irCtx).typeParameters
            else -> throw AssertionError("Unexpected container $container")
          }.single { it.descriptor.name == classifier.descriptor.name }.symbol
        } else irCtx.symbolTable.referenceClassifier(classifier.descriptor!!).ensureBound(irCtx),
        isMarkedNullable,
        arguments.map { it.toIrType(irCtx) },
        emptyList()
      )
    }
  }

  private fun <T : IrSymbol> T.ensureBound(irCtx: IrPluginContext): T = apply {
    if (!isBound)
      (irCtx as IrPluginContextImpl).linker.run {
        getDeclaration(this@ensureBound)
        postProcess(false)
      }
  }

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

    val injectionResult = ctx.cachedOrNull<_, InjectionResult.Success?>(
      INJECTION_RESULT_KEY,
      SourcePosition(currentFile.fileEntry.name, result.startOffset, result.endOffset)
    ) ?: return result

    // some ir transformations reuse the start and end offsets
    // we ensure that were not transforming wrong calls
    if (!expression.symbol.owner.isPropertyAccessor &&
      expression.symbol.owner.descriptor.containingDeclaration
        .safeAs<ClassDescriptor>()
        ?.defaultType
        ?.isFunctionOrSuspendFunctionType != true &&
      injectionResult.callee.callable.fqNameSafe != result.symbol.owner.descriptor.fqNameSafe)
      return result

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
