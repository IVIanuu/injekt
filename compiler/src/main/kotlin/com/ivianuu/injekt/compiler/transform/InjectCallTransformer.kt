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
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import kotlin.collections.*

class InjectCallTransformer(
  private val irCtx: IrPluginContext,
  private val ctx: Context
) : IrElementTransformerVoidWithContext() {
  private inner class RootContext(val result: InjectionResult.Success, val startOffset: Int) {
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
    val symbol = irScope.scopeOwnerSymbol
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
        is TypeKeyInjectable -> typeKeyExpression(result, candidate)
      }

      if (!result.candidate.type.isNullableType ||
          result.dependencyResults.keys.firstOrNull()?.parameterIndex != DISPATCH_RECEIVER_INDEX) expression
      else DeclarationIrBuilder(irCtx, symbol).run {
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
        DeclarationIrBuilder(irCtx, symbol)
          .irCall(
            function.symbol,
            result.candidate.type.toIrType(irCtx).typeOrNull!!
          )
      }
    }
  }.invoke(this)

  private fun ScopeContext.lambdaExpression(
    result: ResolutionResult.Success.Value,
    injectable: LambdaInjectable
  ): IrExpression = DeclarationIrBuilder(irCtx, symbol)
    .irLambda(injectable.type.toIrType(irCtx).typeOrNull!!) { function ->
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
  ): IrExpression = DeclarationIrBuilder(irCtx, symbol).irBlock {
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

  private val typeKey = irCtx.referenceClass(InjektFqNames.TypeKey)
  private val typeKeyValue = typeKey?.owner?.properties
    ?.single { it.name.asString() == "value" }
  private val typeKeyConstructor = typeKey?.constructors?.single()
  private val stringPlus = irCtx.irBuiltIns.stringClass
    .functions
    .map { it.owner }
    .first { it.name.asString() == "plus" }

  private fun ScopeContext.typeKeyExpression(
    result: ResolutionResult.Success.Value,
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
              val dependencyResult = result.dependencyResults.values.single {
                 it.cast<ResolutionResult.Success.Value>()
                   .candidate.type.arguments.single().classifier == typeToRender.classifier
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
      putTypeArgument(0, injectable.type.arguments.single().toIrType(irCtx).cast())
      putValueArgument(0, stringExpression)
    }
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
    DeclarationIrBuilder(irCtx, symbol)
      .irGetObject(irCtx.referenceClass(type.classifier.fqName)!!)

  private fun ScopeContext.functionExpression(
    result: ResolutionResult.Success.Value,
    injectable: CallableInjectable,
    descriptor: CallableDescriptor
  ): IrExpression = DeclarationIrBuilder(irCtx, symbol)
    .irCall(descriptor.irCallable(irCtx).symbol, injectable.type.toIrType(irCtx).typeOrNull!!)
    .apply {
      fillTypeParameters(injectable.callable)
      inject(this@functionExpression, result.dependencyResults)
    }

  private fun ScopeContext.receiverExpression(
    descriptor: ParameterDescriptor
  ): IrExpression = DeclarationIrBuilder(irCtx, symbol).run {
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
      is CallableDescriptor -> DeclarationIrBuilder(irCtx, symbol)
        .irGet(
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
  ): IrExpression = if (descriptor.getter != null)
    DeclarationIrBuilder(irCtx, symbol)
      .irCall(
        irCtx.symbolTable.descriptorExtension.referenceSimpleFunction(descriptor.getter!!),
        injectable.type.toIrType(irCtx).typeOrNull!!
      )
  else
    DeclarationIrBuilder(irCtx, symbol)
      .irGet(
        injectable.type.toIrType(irCtx).typeOrNull!!,
        localVariables.single { it.descriptor == descriptor }.symbol
      )

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
        val rootContext = RootContext(injectionResult, result.startOffset)
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
