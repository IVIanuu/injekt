/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, UnsafeDuringIrConstructionAPI::class)

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.frontend.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
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
    when {
      this is FirTypeParameterSymbol -> typeParameterUniqueKey(
        name,
        containingDeclarationSymbol.fqName,
        containingDeclarationSymbol.uniqueKey(ctx)
      )
      this is FirClassLikeSymbol<*> -> classLikeUniqueKey(fqName)
      this is FirPropertySymbol && isLocal -> localVariableUniqueKey(
        name,
        source!!.startOffset,
        source!!.endOffset,
        resolvedReturnType.uniqueTypeKey(ctx)
      )
      this is FirCallableSymbol<*> -> callableUniqueKey(
        fqName,
        typeParameterSymbols.map { it.name },
        listOfNotNull(dispatchReceiverType, receiverParameter?.typeRef?.coneType)
          .plus(
            (safeAs<FirFunctionSymbol<*>>()?.valueParameterSymbols ?: emptyList())
              .map { it.resolvedReturnType }
          )
          .map { it.uniqueTypeKey(ctx) },
        resolvedReturnType.uniqueTypeKey(ctx)
      )
      else -> error("Unexpected declaration $this")
    }
  }

fun ConeKotlinType.uniqueTypeKey(ctx: InjektContext): String = ctx.cached("unique_key", this) {
  uniqueTypeKey(
    classIdOrName = {
      safeAs<ConeClassLikeType>()?.classId?.asString()
        ?: safeAs<ConeLookupTagBasedType>()?.lookupTag?.name?.asString()
    },
    arguments = {
      typeArguments.map { if (it.isStarProjection) null else it.type }
    },
    isMarkedNullable = { isMarkedNullable }
  )
}

fun IrSymbol.uniqueKey(ctx: InjektContext): String = ctx.cached("unique_key", this) {
  when (this) {
    is IrTypeParameterSymbol -> typeParameterUniqueKey(
      owner.name,
      owner.parent.kotlinFqName,
      owner.parent.cast<IrDeclaration>().symbol.uniqueKey(ctx)
    )
    is IrClassSymbol -> classLikeUniqueKey(owner.fqNameForIrSerialization)
    is IrFunctionSymbol -> callableUniqueKey(
      safeAs<IrSimpleFunctionSymbol>()?.owner?.correspondingPropertySymbol?.owner
        ?.let { it.parent.kotlinFqName.child(it.name) } ?: owner.kotlinFqName,
      (if (this is IrConstructorSymbol) owner.constructedClass.typeParameters else owner.typeParameters).map { it.name },
      listOfNotNull(owner.dispatchReceiverParameter, owner.extensionReceiverParameter)
        .plus(owner.valueParameters)
        .map { it.type.uniqueTypeKey(ctx) },
      owner.returnType.uniqueTypeKey(ctx)
    )
    is IrPropertySymbol -> callableUniqueKey(
      owner.parent.kotlinFqName.child(owner.name),
      owner.getter!!.typeParameters.map { it.name },
      listOfNotNull(owner.getter!!.dispatchReceiverParameter, owner.getter!!.extensionReceiverParameter)
        .map { it.type.uniqueTypeKey(ctx) },
      owner.getter!!.returnType.uniqueTypeKey(ctx)
    )
    is IrVariableSymbol ->
      localVariableUniqueKey(owner.name, owner.startOffset, owner.endOffset, owner.type.uniqueTypeKey(ctx))
    is IrValueParameterSymbol -> callableUniqueKey(
      owner.parent.kotlinFqName.child(owner.name),
      emptyList(),
      emptyList(),
      owner.type.uniqueTypeKey(ctx)
    )
    else -> error("Unexpected declaration $this")
  }
}

fun IrType.uniqueTypeKey(ctx: InjektContext): String = ctx.cached("unique_key", this) {
  uniqueTypeKey(
    classIdOrName = {
      classOrNull?.owner?.classId?.asString()
        ?: classifierOrNull?.owner?.cast<IrDeclarationWithName>()?.name
          ?.takeIf { it != SpecialNames.NO_NAME_PROVIDED }
          ?.asString()
    },
    arguments = { safeAs<IrSimpleType>()?.arguments?.map { it.typeOrNull } ?: emptyList() },
    isMarkedNullable = { isMarkedNullable() }
  )
}

private fun classLikeUniqueKey(fqName: FqName) = "class_like:$fqName"

private fun callableUniqueKey(
  fqName: FqName,
  typeParameterNames: List<Name>,
  parameterUniqueKeys: List<String>,
  returnTypeUniqueKey: String
) = "callable:$fqName:" +
    typeParameterNames.joinToString(",") { it.asString() } + ":" +
    parameterUniqueKeys.joinToString(",") + ":" +
    returnTypeUniqueKey

private fun localVariableUniqueKey(
  name: Name,
  startOffset: Int,
  endOffset: Int,
  returnTypeUniqueKey: String
) = "variable:${name}:$startOffset:$endOffset:$returnTypeUniqueKey"

private fun typeParameterUniqueKey(
  name: Name,
  parentFqName: FqName,
  parentUniqueKey: String
) = "typeparameter:${parentFqName}.$name:${parentUniqueKey}"

private fun <T> T.uniqueTypeKey(
  classIdOrName: T.() -> String?,
  arguments: T. () -> List<T?>,
  isMarkedNullable: T.() -> Boolean
): String = buildString {
  append(classIdOrName())
  append(arguments().joinToString(",") {
    it?.uniqueTypeKey(classIdOrName, arguments, isMarkedNullable) ?: "*"
  })
  if (isMarkedNullable()) append("?")
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

fun findCallableSymbol(
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

fun findClassifierSymbol(
  classifierKey: String,
  classifierFqName: FqName,
  ctx: InjektContext,
): FirClassifierSymbol<*> = ctx.cached("classifier_for_key", classifierKey) {
  getClassifierForFqName(classifierFqName, ctx)
    ?: (getClassifierForFqName(classifierFqName.parent(), ctx)
      ?.typeParameterSymbols
      ?: collectDeclarationsInFqName(classifierFqName.parent().parent(), ctx)
        .filterIsInstance<FirCallableSymbol<*>>()
        .filter { it.name == classifierFqName.parent().shortName() }
        .flatMap { it.typeParameterSymbols })
      .singleOrNull { it.uniqueKey(ctx) == classifierKey }
    ?: error("Could not find classifier for $classifierKey $classifierFqName ${collectDeclarationsInFqName(classifierFqName.parent(), ctx).filter {
      it.fqName == classifierFqName
    }.map { it.uniqueKey(ctx) }}")
}

fun getClassifierForFqName(fqName: FqName, ctx: InjektContext): FirClassifierSymbol<*>? =
  ctx.cached("classifier_for_fq_name", fqName) {
    if (fqName == InjektFqNames.Any.asSingleFqName())
      return@cached ctx.anyType.classifier.symbol

    if (fqName.asString().startsWith(InjektFqNames.function) ||
      fqName.asString().startsWith(InjektFqNames.kFunction) ||
      fqName.asString().startsWith(InjektFqNames.suspendFunction) ||
      fqName.asString().startsWith(InjektFqNames.kSuspendFunction))
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
            ctx.session.symbolProvider.getRegularClassSymbolByClassId(ClassId(fqName, it))
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
      .singleOrNull { it.fqName.shortName() == fqName.shortName() }
      ?.safeAs<FirRegularClassSymbol>()

    return@cached classSymbol?.declarationSymbols ?: emptyList()
  }
