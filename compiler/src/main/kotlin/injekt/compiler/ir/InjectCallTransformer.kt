/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, UnsafeDuringIrConstructionAPI::class,
  DeprecatedForRemovalCompilerApi::class
)

package injekt.compiler.ir

import injekt.compiler.*
import injekt.compiler.resolution.*
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.lazy.*
import org.jetbrains.kotlin.fir.resolve.dfa.*
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
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.io.*
import kotlin.collections.*

class InjectCallTransformer(
  private val compilationDeclarations: CompilationDeclarations,
  private val irCtx: IrPluginContext,
  private val ctx: InjektContext
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
          } &&
              candidateScope.callContext.canCall(candidate.callContext)
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
      return if (this != scopeContext) scopeContext.expressionFor(result)
      else wrapExpressionInFunctionIfNeeded(result) {
        val expression = when (val candidate = result.candidate) {
          is CallableInjectable -> callableExpression(result, candidate)
          is LambdaInjectable -> lambdaExpression(result, candidate)
          is ListInjectable -> listExpression(result, candidate)
          is SourceKeyInjectable -> sourceKeyExpression()
          is TypeKeyInjectable -> typeKeyExpression(result, candidate)
        }

        if (!result.candidate.type.isNullableType ||
          result.dependencyResults.keys.none { it.parameterName == DISPATCH_RECEIVER_NAME }) expression
        else irBuilder.irBlock {
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

  private fun IrFunctionAccessExpression.injectParameters(
    ctx: ScopeContext,
    results: Map<InjectableRequest, ResolutionResult.Success>
  ) {
    for ((request, result) in results) {
      if (result !is ResolutionResult.Success.Value) continue
      val expression = ctx.expressionFor(result)
      when (request.parameterName) {
        DISPATCH_RECEIVER_NAME -> dispatchReceiver = expression
        EXTENSION_RECEIVER_NAME -> extensionReceiver = expression
        else -> putValueArgument(
          symbol.owner
            .valueParameters
            .singleOrNull { it.injektName() == request.parameterName }
            ?.index
            ?: error("Wtf $request ${symbol.owner.dump()}"),
          expression
        )
      }
    }
  }

  private fun ResolutionResult.Success.Value.shouldWrap(ctx: RootContext): Boolean =
    ctx.usages[usageKey(ctx)]!!.size >= 2 && dependencyResults.size > 1

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
        isSuspend = scope.callContext == CallContext.SUSPEND
      }.apply {
        parent = irScope.getLocalDeclarationParent()

        if (result.candidate.callContext == CallContext.COMPOSABLE) {
          annotations = annotations + DeclarationIrBuilder(irCtx, symbol)
            .irCallConstructor(
              irCtx.referenceConstructors(InjektFqNames.Composable)
                .single(),
              emptyList()
            )
        }

        body = DeclarationIrBuilder(irCtx, symbol).run {
          irExprBody(irReturn(unwrappedExpression()))
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
  ): IrExpression {
    val type = injectable.type.toIrType(this).typeOrNull.cast<IrSimpleType>()
    val lambda = IrFactoryImpl.buildFun {
      origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
      name = Name.special("<anonymous>")
      returnType = type.arguments.last().typeOrNull!!
      visibility = DescriptorVisibilities.LOCAL
      isSuspend = injectable.dependencyCallContext == CallContext.SUSPEND
    }.apply {
      parent = irBuilder.scope.getLocalDeclarationParent()

      if (injectable.dependencyCallContext == CallContext.COMPOSABLE) {
        annotations = annotations + DeclarationIrBuilder(irCtx, symbol)
          .irCallConstructor(
            irCtx.referenceConstructors(InjektFqNames.Composable)
              .single(),
            emptyList()
          )
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
      val dependencyScopeContext = injectable.dependencyScope?.let {
        ScopeContext(
          this@lambdaExpression, rootContext,
          it, irBuilder.scope
        )
      }

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
        val expression = irReturn(
          dependencyScopeContext?.run { createExpression() }
            ?: createExpression()
        )

        if (dependencyScopeContext == null || dependencyScopeContext.statements.isEmpty())
          irExprBody(expression)
        else {
          irBlockBody {
            +dependencyScopeContext.statements
            +expression
          }
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
      irCall(mutableListOf).apply {
        putTypeArgument(
          0,
          injectable.singleElementType.toIrType(this@listExpression).typeOrNull
        )
      }
    )

    result.dependencyResults.forEach { (_, dependencyResult) ->
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

  private val sourceKeyConstructor =
    irCtx.referenceClass(InjektFqNames.SourceKey)?.constructors?.single()

  private fun ScopeContext.sourceKeyExpression(): IrExpression =
    irBuilder.irCall(sourceKeyConstructor!!).apply {
      putValueArgument(
        0,
        irBuilder.irString(
          buildString {
            append(currentFile.name)
            append(":")
            append(currentFile.fileEntry.getLineNumber(rootContext.startOffset) + 1)
            append(":")
            append(currentFile.fileEntry.getColumnNumber(rootContext.startOffset))
          }
        )
      )
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
      putTypeArgument(
        0,
        injectable.type.arguments.single().toIrType(this@typeKeyExpression).cast()
      )
      putValueArgument(0, stringExpression)
    }
  }

  private fun ScopeContext.callableExpression(
    result: ResolutionResult.Success.Value,
    injectable: CallableInjectable
  ): IrExpression = when {
    injectable.callable.type.unwrapTags().classifier.isObject &&
        (injectable.callable.symbol.name == DISPATCH_RECEIVER_NAME ||
            injectable.callable.symbol is FirConstructorSymbol)->
      objectExpression(injectable.callable.type.unwrapTags())
    else -> when {
      injectable.callable.symbol is FirPropertySymbol &&
          injectable.callable.symbol.isLocal ->
        localVariableExpression(injectable, injectable.callable.symbol)
      injectable.callable.symbol is FirValueParameterSymbol ->
        parameterExpression(injectable, injectable.callable.symbol)
      else -> functionExpression(result, injectable, injectable.callable.symbol)
    }
  }

  private fun ScopeContext.objectExpression(type: InjektType): IrExpression =
    irBuilder.irGetObject(irCtx.referenceClass(type.classifier.classId!!)!!)

  private fun ScopeContext.functionExpression(
    result: ResolutionResult.Success.Value,
    injectable: CallableInjectable,
    symbol: FirCallableSymbol<*>
  ): IrExpression = irBuilder.irCall(
    symbol.toIrCallableSymbol(),
    injectable.type.toIrType(this).typeOrNull!!
  ).apply {
    injectable.callable
      .typeArguments
      .values
      .forEachIndexed { index, typeArgument ->
        putTypeArgument(index, typeArgument.toIrType(this@functionExpression).typeOrNull)
      }
    injectParameters(this@functionExpression, result.dependencyResults)
  }

  private fun ScopeContext.parameterExpression(
    injectable: CallableInjectable,
    symbol: FirValueParameterSymbol,
  ): IrExpression = irBuilder.irGet(
    injectable.type.toIrType(this).typeOrNull!!,
    (if (symbol.name == DISPATCH_RECEIVER_NAME || symbol.name == EXTENSION_RECEIVER_NAME)
      allScopes.reversed().firstNotNullOfOrNull { scope ->
        val element = scope.irElement
        val originalInjectableClassifier = injectable.callable.originalType.classifier.symbol
        when {
          element is IrClass &&
              element.symbol.toFirSymbol<FirClassifierSymbol<*>>() ==
              originalInjectableClassifier -> element.thisReceiver!!.symbol
          symbol.name == DISPATCH_RECEIVER_NAME &&
              element is IrFunction &&
              element.dispatchReceiverParameter?.type?.classOrFail?.toFirSymbol<FirClassSymbol<*>>() == originalInjectableClassifier ->
            element.dispatchReceiverParameter!!.symbol
          symbol.name == EXTENSION_RECEIVER_NAME &&
              element is IrFunction &&
              element.extensionReceiverParameter?.startOffset == symbol.source!!.startOffset ->
            element.extensionReceiverParameter!!.symbol
          symbol.name == DISPATCH_RECEIVER_NAME &&
              element is IrProperty &&
              allScopes.getOrNull(allScopes.indexOf(scope) + 1)?.irElement !is IrField &&
              element.getter!!.dispatchReceiverParameter?.type?.classOrFail?.toFirSymbol<FirClassSymbol<*>>() == originalInjectableClassifier ->
            element.getter!!.dispatchReceiverParameter!!.symbol
          symbol.name == EXTENSION_RECEIVER_NAME &&
              element is IrProperty &&
              element.getter!!.extensionReceiverParameter?.startOffset == symbol.source!!.startOffset ->
            element.getter!!.extensionReceiverParameter!!.symbol
          else -> null
        }
      } else null)
      ?: lambdaParametersMap[symbol] ?: symbol.containingDeclarationSymbol.toIrCallableSymbol()
        .owner
        .valueParameters
        .singleOrNull { it.injektName() == symbol.injektName() }
        ?.symbol
      ?: error("wtf $symbol")
  )

  private fun ScopeContext.localVariableExpression(
    injectable: CallableInjectable,
    symbol: FirPropertySymbol,
  ): IrExpression = if (symbol.getterSymbol != null) irBuilder.irCall(
    symbol.getterSymbol!!.toIrCallableSymbol(),
    injectable.type.toIrType(this).typeOrNull!!
  )
  else irBuilder.irGet(
    injectable.type.toIrType(this).typeOrNull!!,
    compilationDeclarations.declarations
      .singleOrNull {
        it.owner.startOffset == symbol.source!!.startOffset &&
            it.owner.endOffset == symbol.source!!.endOffset
      }
      ?.cast()
      ?: error("wtf $this")
  )

  private inline fun <reified T : FirBasedSymbol<*>> IrSymbol.toFirSymbol() =
    (owner.safeAs<IrMetadataSourceOwner>()?.metadata?.safeAs<FirMetadataSource>()?.fir?.symbol ?:
    owner.safeAs<AbstractFir2IrLazyDeclaration<*>>()?.fir?.safeAs<FirMemberDeclaration>()?.symbol)
      ?.safeAs<T>()

  private fun FirClassifierSymbol<*>.toIrClassifierSymbol(): IrSymbol = when (this) {
    is FirClassSymbol<*> -> compilationDeclarations.declarations
      .singleOrNull { it.toFirSymbol<FirClassSymbol<*>>() == this }
      ?: irCtx.referenceClass(classId)
        ?.takeIf { it.toFirSymbol<FirClassSymbol<*>>() == this }
      ?: error("wtf $this")
    is FirTypeAliasSymbol -> irCtx.referenceTypeAlias(classId) ?: error("wtf $this")
    is FirTypeParameterSymbol -> (containingDeclarationSymbol
      .safeAs<FirCallableSymbol<*>>()
      ?.toIrCallableSymbol()
      ?.owner
      ?.typeParameters
      ?: containingDeclarationSymbol
        .safeAs<FirClassifierSymbol<*>>()
        ?.toIrClassifierSymbol()
        ?.owner
        ?.cast<IrTypeParametersContainer>()
        ?.typeParameters)
      ?.singleOrNull { it.name == name }
      ?.symbol
      ?: error("wtf $this")
    else -> throw AssertionError("Unexpected classifier $this")
  }

  private fun FirBasedSymbol<*>.toIrCallableSymbol(): IrFunctionSymbol = when (this) {
    is FirConstructorSymbol -> compilationDeclarations.declarations
      .singleOrNull { it.toFirSymbol<FirConstructorSymbol>() == this }
      ?.cast<IrConstructorSymbol>()
      ?: irCtx.referenceConstructors(resolvedReturnType.classId!!)
        .singleOrNull { it.toFirSymbol<FirConstructorSymbol>() == this }
      ?: error("wtf $this")
    is FirFunctionSymbol<*> -> compilationDeclarations.declarations.singleOrNull {
      it.toFirSymbol<FirFunctionSymbol<*>>() == this
    }
      ?.cast<IrFunctionSymbol>()
      ?: irCtx.referenceFunctions(callableId)
        .singleOrNull { it.toFirSymbol<FirFunctionSymbol<*>>() == this }
      ?: error("wtf $this")
    is FirPropertySymbol -> (compilationDeclarations.declarations
      .singleOrNull { it.toFirSymbol<FirPropertySymbol>() == this }
      ?.cast<IrPropertySymbol>()
      ?: irCtx.referenceProperties(callableId)
        .singleOrNull { it.toFirSymbol<FirPropertySymbol>() == this })
      ?.owner
      ?.getter
      ?.symbol
      ?: error("wtf $this")
    else -> throw AssertionError("Unexpected callable $this")
  }

  private fun InjektType.toIrType(ctx: ScopeContext): IrTypeArgument = when {
    isStarProjection -> IrStarProjectionImpl
    classifier.isTag && !classifier.isTypeAlias -> arguments.last().toIrType(ctx)
      .typeOrFail
      .addAnnotations(
        listOf(
          ctx.irBuilder.irCallConstructor(
            irCtx.referenceClass(classifier.classId!!)!!.constructors.single(),
            arguments.dropLast(1).map {
              it.toIrType(ctx).typeOrNull
                ?: irCtx.irBuiltIns.anyType.makeNullable()
            }
          )
        )
      ).cast()
    classifier.isTag && classifier.isTypeAlias -> classifier.symbol!!
      .cast<FirTypeAliasSymbol>()
      .resolvedExpandedTypeRef
      .coneType
      .toInjektType(this@InjectCallTransformer.ctx)
      .toIrType(ctx)
    else -> IrSimpleTypeImpl(
      classifier.symbol!!.toIrClassifierSymbol().cast(),
      isMarkedNullable,
      arguments.map { it.toIrType(ctx) },
      emptyList()
    )
  }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression

    val injectionResult = ctx.cachedOrNull<_, InjectionResult.Success>(
      INJECTION_RESULT_KEY,
      SourcePosition(currentFile.fileEntry.name, result.endOffset)
    ) ?: return result

    // some ir transformations reuse the start and end offsets
    // we ensure that were not transforming wrong calls
    if (injectionResult.callee.symbol.fqName != result.symbol.owner.kotlinFqName)
      return result

    return DeclarationIrBuilder(irCtx, result.symbol).run {
      val rootContext = RootContext(injectionResult, result.startOffset)
      try {
        ScopeContext(
          parent = null,
          rootContext = rootContext,
          scope = injectionResult.scope,
          irScope = scope
        ).run { result.injectParameters(this, injectionResult.results) }
      } catch (e: Throwable) {
        throw RuntimeException("Wtf ${result.dump()}", e)
      }

      return if (rootContext.statements.isEmpty()) result
      else irBlock {
        rootContext.statements.forEach { +it }
        +result
      }
    }
  }
}

class CompilationDeclarations : IrElementTransformerVoid() {
  val declarations = mutableListOf<IrSymbol>()
  override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
    declarations += declaration.symbol
    return super.visitDeclaration(declaration)
  }
}
