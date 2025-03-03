/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package injekt.compiler

import injekt.compiler.fir.*
import injekt.compiler.fir.callableInfo
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import kotlin.experimental.*

fun String.asNameId() = Name.identifier(this)

fun FirBasedSymbol<*>.isInjectable(ctx: InjektContext): Boolean {
  if (hasAnnotation(InjektFqNames.Provide, ctx.session)) return true

  if (this is FirConstructorSymbol && isPrimary &&
    resolvedReturnType.toRegularClassSymbol(ctx.session)!!.isInjectable(ctx))
    return true

  if (this is FirValueParameterSymbol) {
    val callableInfo = containingFunctionSymbol.callableInfo(ctx)
    val index = containingFunctionSymbol.valueParameterSymbols.indexOf(this)
    if (index in callableInfo.injectParameters)
      return true
  }

  return false
}

fun List<FirAnnotation>.getTags(ctx: InjektContext): List<FirAnnotation> = filter {
  it.resolvedType.toRegularClassSymbol(ctx.session)?.isTag(ctx) == true
}

fun FirClassifierSymbol<*>.isTag(ctx: InjektContext): Boolean =
  this is FirRegularClassSymbol &&
      (hasAnnotation(InjektFqNames.Tag, ctx.session) || classId == InjektFqNames.Composable)

val FirBasedSymbol<*>.fqName: FqName
  get() = when (this) {
    is FirClassLikeSymbol<*> -> classId.asSingleFqName()
    is FirConstructorSymbol -> callableId.asSingleFqName().parent().child(SpecialNames.INIT)
    is FirValueParameterSymbol -> containingFunctionSymbol.fqName.child(name)
    is FirCallableSymbol<*> -> callableId.asSingleFqName()
    is FirTypeParameterSymbol -> containingDeclarationSymbol.fqName.child(name)
    else -> throw AssertionError("Unexpected $this")
  }

fun FirBasedSymbol<*>.uniqueKey(ctx: InjektContext): String =
  ctx.cached("unique_key", this) {
    when (this) {
      is FirTypeParameterSymbol -> "typeparameter:${containingDeclarationSymbol.uniqueKey(ctx)}:$name"
      is FirClassLikeSymbol<*> -> "class_like:$fqName"
      is FirCallableSymbol<*> -> if (this != originalOrSelf()) originalOrSelf().uniqueKey(ctx)
      else "callable:$fqName:" +
          typeParameterSymbols.joinToString(",") { it.name.asString() } + ":" +
          listOfNotNull(dispatchReceiverType, receiverParameter?.typeRef?.coneType)
            .plus(
              (safeAs<FirFunctionSymbol<*>>()?.valueParameterSymbols ?: emptyList())
                .map { it.resolvedReturnType }
            ).joinToString(",") { it.uniqueTypeKey(ctx) } + ":" +
          resolvedReturnType.uniqueTypeKey(ctx)
      else -> error("Unexpected declaration $this")
    }
  }

fun ConeKotlinType.uniqueTypeKey(ctx: InjektContext): String = ctx.cached("unique_key", this) {
  buildString {
    val finalType = fullyExpandedType(ctx.session)
    append(
      finalType.safeAs<ConeClassLikeType>()?.classId?.asString()
        ?: finalType.safeAs<ConeLookupTagBasedType>()?.lookupTag?.name?.asString()
    )
    finalType.typeArguments.joinToString(",") {
      if (it.isStarProjection) "*"
      else it.type?.uniqueTypeKey(ctx).orEmpty()
    }.let { append(it) }
    if (finalType.isMarkedNullable) append("?")
  }
}

@OptIn(ExperimentalTypeInference::class)
inline fun <T, R> Collection<T>.transform(@BuilderInference block: MutableList<R>.(T) -> Unit): List<R> =
  mutableListOf<R>().apply {
    for (item in this@transform)
      block(item)
  }

val DISPATCH_RECEIVER_NAME = Name.identifier("\$dispatchReceiver")
val EXTENSION_RECEIVER_NAME = Name.identifier("\$extensionReceiver")

const val DISPATCH_RECEIVER_INDEX = -2
const val EXTENSION_RECEIVER_INDEX = -1

fun IrDeclaration.injektIndex(): Int {
  if (parent is IrClass) return DISPATCH_RECEIVER_INDEX
  val callable = parent.cast<IrFunction>()
  return when {
    this == callable.dispatchReceiverParameter -> DISPATCH_RECEIVER_INDEX
    this == callable.extensionReceiverParameter -> EXTENSION_RECEIVER_INDEX
    else -> callable.valueParameters.indexOf(this)
  }
}

fun findCallableForKey(
  callableKey: String,
  callableFqName: FqName,
  ctx: InjektContext,
): FirCallableSymbol<*> = ctx.cached("callable_for_key", callableKey) {
  collectDeclarationsInFqName(callableFqName.parent(), ctx)
    .filterIsInstance<FirCallableSymbol<*>>()
    .singleOrNull { it.uniqueKey(ctx) == callableKey }
    ?: error("Could not find callable for $callableKey $callableFqName " +
        "parent ${callableFqName.parent()} " +
        "in ${collectDeclarationsInFqName(callableFqName.parent(), ctx).map { it to it.uniqueKey(ctx) }}")
}

fun findClassifierForKey(
  classifierKey: String,
  classifierFqName: FqName,
  ctx: InjektContext,
): FirClassifierSymbol<*> = ctx.cached("classifier_for_key", classifierKey) {
  findClassifierForFqName(classifierFqName, ctx)
    ?: collectDeclarationsInFqName(classifierFqName.parent().parent(), ctx)
      .filter { it.fqName.shortName() == classifierFqName.parent().shortName() }
      .flatMap { it.typeParameterSymbols ?: emptyList() }
      .singleOrNull { it.uniqueKey(ctx) == classifierKey }
    ?: error("Could not find classifier for $classifierKey $classifierFqName " +
        "${collectDeclarationsInFqName(classifierFqName.parent(), ctx).map { it.uniqueKey(ctx) }} " +
        "${collectDeclarationsInFqName(classifierFqName.parent().parent(), ctx).map { it.uniqueKey(ctx) }}")
}

fun findClassifierForFqName(fqName: FqName, ctx: InjektContext): FirClassifierSymbol<*>? =
  ctx.cached("classifier_for_fq_name", fqName) {
    if (fqName == InjektFqNames.Any.asSingleFqName())
      return@cached ctx.anyType.classifier.symbol
    // todo find a way to support ALL function kinds
    if (fqName.asString().startsWith(InjektFqNames.function) ||
      fqName.asString().startsWith(InjektFqNames.kFunction) ||
      fqName.asString().startsWith(InjektFqNames.suspendFunction) ||
      fqName.asString().startsWith(InjektFqNames.kSuspendFunction) ||
      fqName.asString().startsWith(InjektFqNames.composableFunction) ||
      fqName.asString().startsWith(InjektFqNames.kComposableFunction))
      ctx.session.symbolProvider.getClassLikeSymbolByClassId(ClassId.topLevel(fqName))
    else collectDeclarationsInFqName(fqName.parent(), ctx)
      .filterIsInstance<FirClassifierSymbol<*>>()
      .singleOrNull { it.fqName == fqName }
  }

fun collectDeclarationsInFqName(fqName: FqName, ctx: InjektContext): List<FirBasedSymbol<*>> =
  ctx.cached("declarations_in_fq_name", fqName) {
    val packageFqName = ctx.session.symbolProvider.getPackage(fqName)

    if (fqName.isRoot || packageFqName != null)
      return@cached buildList {
        ctx.session.symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(fqName)
          ?.mapNotNull {
            ctx.session.symbolProvider.getClassLikeSymbolByClassId(ClassId(fqName, it))
          }
          ?.forEach { add(it) }

        ctx.session.symbolProvider.symbolNamesProvider.getTopLevelCallableNamesInPackage(fqName)
          ?.flatMap { name ->
            ctx.session.symbolProvider.getTopLevelCallableSymbols(fqName, name)
          }
          ?.forEach { add(it) }
      }

    val parentDeclarations = collectDeclarationsInFqName(fqName.parent(), ctx)
      .takeIf { it.isNotEmpty() } ?: return@cached emptyList()

    val classSymbol = parentDeclarations
      .filterIsInstance<FirRegularClassSymbol>()
      .singleOrNull { it.fqName.shortName() == fqName.shortName() }

    return@cached classSymbol?.declarationSymbols ?: emptyList()
  }
