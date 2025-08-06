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
        getOrPut(usageKey()) { mutableSetOf() } += request
        dependencyResults.forEach { it.value.collectUsagesRecursive(it.key) }
      }

      result.results.forEach { it.value.collectUsagesRecursive(it.key) }
    }

    fun mapScopeIfNeeded(scope: InjectablesScope) =
      if (scope in result.scope.allScopes) result.scope else scope
  }

  context(rootCtx: RootContext)
  private fun ResolutionResult.Success.Value.usageKey(): Any =
    listOf(candidate::class, candidate.type, highestScope())

  context(rootCtx: RootContext)
  private fun ResolutionResult.Success.Value.highestScope(): InjectablesScope =
    rootCtx.highestScope.getOrPut(this) {
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
      else with(rootContext) {
        wrapExpressionInFunctionIfNeeded(result) {
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
  }

  context(scopeCtx: ScopeContext)
  private fun IrFunctionAccessExpression.injectParameters(
    results: Map<InjectableRequest, ResolutionResult.Success>
  ) {
    for ((request, result) in results) {
      if (result !is ResolutionResult.Success.Value) continue
      val expression = scopeCtx.expressionFor(result)
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

  context(rootCtx: RootContext)
  private fun ResolutionResult.Success.Value.shouldWrap(): Boolean =
    rootCtx.usages[usageKey()]!!.size >= 2 && dependencyResults.size > 1

  context(scopeCtx: ScopeContext, rootCtx: RootContext)
  private fun wrapExpressionInFunctionIfNeeded(
    result: ResolutionResult.Success.Value,
    unwrappedExpression: () -> IrExpression
  ): IrExpression = if (!result.shouldWrap()) unwrappedExpression()
  else with(result.safeAs<ResolutionResult.Success.Value>()
    ?.highestScope()?.let { scopeCtx.findScopeContext(it) } ?: this) {
    scopeCtx.functionWrappedExpressions.getOrPut(result.candidate.type) expression@ {
      val function = IrFactoryImpl.buildFun {
        origin = IrDeclarationOrigin.DEFINED
        name = scopeCtx.irScope.inventNameForTemporary("function").asNameId()
        returnType = result.candidate.type.toIrType().typeOrNull!!
        visibility = DescriptorVisibilities.LOCAL
        isSuspend = scopeCtx.scope.callContext == CallContext.SUSPEND
      }.apply {
        parent = scopeCtx.irScope.getLocalDeclarationParent()

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

        scopeCtx.statements += this
      }

      return@expression {
        irBuilder.irCall(
          function.symbol,
          result.candidate.type.toIrType().typeOrNull!!
        )
      }
    }
  }.invoke(scopeCtx)

  context(scopeCtx: ScopeContext)
  private fun lambdaExpression(
    result: ResolutionResult.Success.Value,
    injectable: LambdaInjectable
  ): IrExpression {
    val type = injectable.type.toIrType().typeOrNull.cast<IrSimpleType>()
    val lambda = IrFactoryImpl.buildFun {
      origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
      name = Name.special("<anonymous>")
      returnType = type.arguments.last().typeOrNull!!
      visibility = DescriptorVisibilities.LOCAL
      isSuspend = injectable.dependencyCallContext == CallContext.SUSPEND
    }.apply {
      parent = scopeCtx.irBuilder.scope.getLocalDeclarationParent()

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
          scopeCtx, scopeCtx.rootContext,
          it, irBuilder.scope
        )
      }

      context(scopeCtx: ScopeContext)
      fun createExpression(): IrExpression {
        for ((index, parameter) in injectable.valueParameterSymbols.withIndex())
          scopeCtx.lambdaParametersMap[parameter] = valueParameters[index].symbol
        return scopeCtx.expressionFor(dependencyResult.cast())
          .also {
            injectable.valueParameterSymbols.forEach {
              scopeCtx.lambdaParametersMap -= it
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

  context(scopeCtx: ScopeContext)
  private fun listExpression(
    result: ResolutionResult.Success.Value,
    injectable: ListInjectable
  ): IrExpression = scopeCtx.irBuilder.irBlock {
    val tmpList = irTemporary(
      irCall(mutableListOf).apply {
        putTypeArgument(
          0,
          injectable.singleElementType.toIrType().typeOrNull
        )
      }
    )

    result.dependencyResults.forEach { (_, dependencyResult) ->
      dependencyResult as ResolutionResult.Success.Value
      +irCall(
        if (
          with(ctx) {
            dependencyResult.candidate.type.isSubTypeOf(injectable.collectionElementType)
          }
          )
          listAddAll else listAdd
      ).apply {
        dispatchReceiver = irGet(tmpList)
        putValueArgument(0, scopeCtx.expressionFor(dependencyResult))
      }
    }

    +irGet(tmpList)
  }

  private val sourceKeyConstructor =
    irCtx.referenceClass(InjektFqNames.SourceKey)?.constructors?.single()

  context(scopeCtx: ScopeContext)
  private fun sourceKeyExpression(): IrExpression =
    scopeCtx.irBuilder.irCall(sourceKeyConstructor!!).apply {
      putValueArgument(
        0,
        scopeCtx.irBuilder.irString(
          buildString {
            append(currentFile.name)
            append(":")
            append(currentFile.fileEntry.getLineNumber(scopeCtx.rootContext.startOffset) + 1)
            append(":")
            append(currentFile.fileEntry.getColumnNumber(scopeCtx.rootContext.startOffset))
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

  context(scopeCtx: ScopeContext)
  private fun typeKeyExpression(
    result: ResolutionResult.Success.Value,
    injectable: TypeKeyInjectable
  ): IrExpression = scopeCtx.irBuilder.run {
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
              dispatchReceiver = scopeCtx.expressionFor(dependencyResult.cast())
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
        injectable.type.arguments.single().toIrType().cast()
      )
      putValueArgument(0, stringExpression)
    }
  }

  context(scopeCtx: ScopeContext)
  private fun callableExpression(
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

  context(scopeCtx: ScopeContext)
  private fun objectExpression(type: InjektType): IrExpression =
    scopeCtx.irBuilder.irGetObject(irCtx.referenceClass(type.classifier.classId!!)!!)

  context(scopeCtx: ScopeContext)
  private fun functionExpression(
    result: ResolutionResult.Success.Value,
    injectable: CallableInjectable,
    symbol: FirCallableSymbol<*>
  ): IrExpression = scopeCtx.irBuilder.irCall(
    symbol.toIrCallableSymbol(),
    injectable.type.toIrType().typeOrNull!!
  ).apply {
    injectable.callable
      .typeArguments
      .values
      .forEachIndexed { index, typeArgument ->
        putTypeArgument(index, typeArgument.toIrType().typeOrNull)
      }
    injectParameters(result.dependencyResults)
  }

  context(scopeCtx: ScopeContext)
  private fun parameterExpression(
    injectable: CallableInjectable,
    symbol: FirValueParameterSymbol,
  ): IrExpression = scopeCtx.irBuilder.irGet(
    injectable.type.toIrType().typeOrNull!!,
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
      ?: scopeCtx.lambdaParametersMap[symbol] ?: symbol.containingDeclarationSymbol.toIrCallableSymbol()
        .owner
        .valueParameters
        .singleOrNull { it.injektName() == symbol.injektName() }
        ?.symbol
      ?: error("wtf $symbol")
  )

  context(scopeCtx: ScopeContext)
  private fun localVariableExpression(
    injectable: CallableInjectable,
    symbol: FirPropertySymbol,
  ): IrExpression = if (symbol.getterSymbol != null) scopeCtx.irBuilder.irCall(
    symbol.getterSymbol!!.toIrCallableSymbol(),
    injectable.type.toIrType().typeOrNull!!
  )
  else scopeCtx.irBuilder.irGet(
    injectable.type.toIrType().typeOrNull!!,
    compilationDeclarations.declarations
      .singleOrNull {
        it.owner is IrVariable &&
            (it.owner as IrVariable).name == symbol.name &&
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
      ?: irCtx.referenceProperties(callableId!!)
        .singleOrNull { it.toFirSymbol<FirPropertySymbol>() == this })
      ?.owner
      ?.getter
      ?.symbol
      ?: error("wtf $this")
    else -> throw AssertionError("Unexpected callable $this")
  }

  context(scopeCtx: ScopeContext)
  private fun InjektType.toIrType(): IrTypeArgument = when {
    isStarProjection -> IrStarProjectionImpl
    classifier.isTag && !classifier.isTypeAlias -> arguments.last().toIrType()
      .typeOrFail
      .addAnnotations(
        listOf(
          scopeCtx.irBuilder.irCallConstructor(
            irCtx.referenceClass(classifier.classId!!)!!.constructors.single(),
            arguments.dropLast(1).map {
              it.toIrType().typeOrNull
                ?: irCtx.irBuiltIns.anyType.makeNullable()
            }
          )
        )
      ).cast()
    classifier.isTag && classifier.isTypeAlias -> classifier.symbol!!
      .cast<FirTypeAliasSymbol>()
      .resolvedExpandedTypeRef
      .coneType
      .let { with(ctx) { it.toInjektType() } }
      .toIrType()
    else -> IrSimpleTypeImpl(
      classifier.symbol!!.toIrClassifierSymbol().cast(),
      isMarkedNullable,
      arguments.map { it.toIrType() },
      emptyList()
    )
  }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression = with(ctx) {
    val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression

    val injectionResult = cachedOrNull<_, InjectionResult.Success>(
      INJECTION_RESULT_KEY,
      SourcePosition(currentFile.fileEntry.name, result.endOffset)
    ) ?: return result

    // some ir transformations reuse the start and end offsets
    // we ensure that were not transforming wrong calls
    if (injectionResult.callee.symbol.fqName != result.symbol.owner.kotlinFqName)
      return result

    DeclarationIrBuilder(irCtx, result.symbol).run {
      val rootContext = RootContext(injectionResult, result.startOffset)
      try {
        ScopeContext(
          parent = null,
          rootContext = rootContext,
          scope = injectionResult.scope,
          irScope = scope
        ).run { result.injectParameters(injectionResult.results) }
      } catch (e: Throwable) {
        throw RuntimeException("Wtf ${result.dump()}", e)
      }

      return@run if (rootContext.statements.isEmpty()) result
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
