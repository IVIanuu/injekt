/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, ObsoleteDescriptorBasedAPI::class,
  FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class
)

package com.ivianuu.injekt.compiler.backend

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
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.io.*
import kotlin.collections.*

class InjectCallTransformer(
  private val localDeclarations: LocalDeclarations,
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
    val functionWrappedExpressions = mutableMapOf<InjektType, ScopeContext.() -> IrExpression>()
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
        for ((index, parameter) in injectable.parameterDescriptors.withIndex())
          parameterMap[parameter] = valueParameters[index]
        return expressionFor(dependencyResult.cast())
          .also {
            injectable.parameterDescriptors.forEach {
              parameterMap -= it
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
    is ReceiverParameterDescriptor -> parameterExpression(injectable.callable.callable, injectable)
    is ValueParameterDescriptor -> parameterExpression(injectable.callable.callable, injectable)
    is LocalVariableDescriptor -> localVariableExpression(injectable.callable.callable, injectable)
    else -> functionExpression(result, injectable, injectable.callable.callable)
  }

  private fun ScopeContext.objectExpression(type: InjektType): IrExpression =
    irBuilder.irGetObject(irCtx.referenceClass(type.classifier.fqName)!!)

  private fun ScopeContext.functionExpression(
    result: ResolutionResult.Success.Value,
    injectable: CallableInjectable,
    descriptor: CallableDescriptor
  ): IrExpression = irBuilder.irCall(
    descriptor.irCallable().symbol,
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
        injectable.type.toIrType().typeOrNull!!,
        (parameterMap[descriptor] ?: containingDeclaration.irCallable()
          .allParameters
          .single { it.descriptor.injektIndex() == descriptor.injektIndex() })
          .symbol
      )
      else -> error("Unexpected parent $descriptor $containingDeclaration")
    }

  private fun IrFunctionAccessExpression.fillTypeParameters(callable: InjektCallable) {
    callable
      .typeArguments
      .values
      .forEachIndexed { index, typeArgument ->
        putTypeArgument(index, typeArgument.toIrType().typeOrNull)
      }
  }

  private fun ScopeContext.localVariableExpression(
    descriptor: LocalVariableDescriptor,
    injectable: CallableInjectable
  ): IrExpression = if (descriptor.getter != null) irBuilder.irCall(
    descriptor.getter!!.irCallable().symbol,
    injectable.type.toIrType().typeOrNull!!
  )
  else irBuilder.irGet(
    injectable.type.toIrType().typeOrNull!!,
    localDeclarations.variables.single { it.descriptor == descriptor }.symbol
  )

  private fun CallableDescriptor.irCallable(): IrFunction {
    when (this) {
      is ClassConstructorDescriptor -> {
        if (constructedClass.visibility == DescriptorVisibilities.LOCAL)
          return localDeclarations.classes
            .single { it.descriptor.uniqueKey(ctx) == constructedClass.uniqueKey(ctx) }
            .constructors
            .single { it.descriptor.uniqueKey(ctx) == uniqueKey(ctx) }

        return irCtx.referenceConstructors(constructedClass.fqNameSafe)
          .single { it.descriptor.uniqueKey(ctx) == uniqueKey(ctx) }
          .owner
      }
      is FunctionDescriptor -> {
        if (visibility == DescriptorVisibilities.LOCAL)
          return localDeclarations.functions.single {
            it.descriptor.uniqueKey(ctx) == uniqueKey(ctx)
          }

        if (containingDeclaration.safeAs<DeclarationDescriptorWithVisibility>()
            ?.visibility == DescriptorVisibilities.LOCAL)
          return localDeclarations.classes.flatMap { it.declarations }
            .single { it.descriptor.uniqueKey(ctx) == uniqueKey(ctx) }
            .cast()

        return irCtx.referenceFunctions(fqNameSafe)
          .single { it.descriptor.uniqueKey(ctx) == uniqueKey(ctx) }
          .owner
      }
      is PropertyDescriptor -> {
        if (containingDeclaration.safeAs<DeclarationDescriptorWithVisibility>()
            ?.visibility == DescriptorVisibilities.LOCAL)
          return localDeclarations.classes.flatMap { it.declarations }
            .single { it.descriptor.uniqueKey(ctx) == uniqueKey(ctx) }
            .cast()

        return irCtx.referenceProperties(fqNameSafe)
          .single { it.descriptor.uniqueKey(ctx) == uniqueKey(ctx) }
          .owner
          .getter!!
      }
      else -> throw AssertionError("Unexpected callable $this")
    }
  }

  private fun InjektType.toIrType(): IrTypeArgument {
    if (isStarProjection) return IrStarProjectionImpl
    return when {
      classifier.isTag -> arguments.last().toIrType()
        .typeOrNull!!
        .cast<IrSimpleType>()
        .let { type ->
          val tagConstructor = irCtx.referenceClass(classifier.fqName)!!
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
        val key = classifier.descriptor!!.uniqueKey(ctx)
        val fqName = FqName(key.split(":")[1])
        val irClassifier = localDeclarations.classes.singleOrNull {
          it.descriptor.uniqueKey(ctx) == key
        }
          ?.symbol
          ?: irCtx.referenceClass(fqName)
          ?: irCtx.referenceFunctions(fqName.parent())
            .flatMap { it.owner.typeParameters }
            .singleOrNull { it.descriptor.uniqueKey(ctx) == key }
            ?.symbol
          ?: irCtx.referenceProperties(fqName.parent())
            .flatMap { it.owner.getter!!.typeParameters }
            .singleOrNull { it.descriptor.uniqueKey(ctx) == key }
            ?.symbol
          ?: (irCtx.referenceClass(fqName.parent()) ?: irCtx.referenceTypeAlias(fqName.parent()))
            ?.owner
            ?.typeParameters
            ?.singleOrNull { it.descriptor.uniqueKey(ctx) == key }
            ?.symbol
          ?: error("Could not get for $fqName $key")
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

    val injectionResult = ctx.cachedOrNull<_, InjectionResult.Success?>(
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

class LocalDeclarations : IrElementTransformerVoid() {
  val classes = mutableSetOf<IrClass>()
  val functions = mutableSetOf<IrFunction>()
  val variables = mutableSetOf<IrVariable>()

  override fun visitClass(declaration: IrClass): IrStatement {
    if (declaration.visibility == DescriptorVisibilities.LOCAL)
      classes += declaration
    return super.visitClass(declaration)
  }

  override fun visitFunction(declaration: IrFunction): IrStatement {
    if (declaration.visibility == DescriptorVisibilities.LOCAL)
      functions += declaration
    return super.visitFunction(declaration)
  }

  override fun visitVariable(declaration: IrVariable): IrStatement {
    variables += declaration
    return super.visitVariable(declaration)
  }
}
