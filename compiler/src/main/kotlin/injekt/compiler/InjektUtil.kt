/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, SymbolInternals::class)

package injekt.compiler

import injekt.compiler.fir.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.java.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.jvm.*
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
    val metadata = containingDeclarationSymbol.cast<FirFunctionSymbol<*>>().callableMetadata(ctx)
    if (name in metadata.injectParameters)
      return true
  }

  return false
}

fun List<FirAnnotation>.getTagAnnotations(ctx: InjektContext): List<FirAnnotation> = filter {
  it.resolvedType.toRegularClassSymbol(ctx.session)?.isTagAnnotation(ctx) == true
}

fun FirClassifierSymbol<*>.isTagAnnotation(ctx: InjektContext): Boolean =
  this is FirRegularClassSymbol && hasAnnotation(InjektFqNames.Tag, ctx.session)

val FirBasedSymbol<*>.fqName: FqName
  get() = when (this) {
    is FirClassLikeSymbol<*> -> classId.asSingleFqName()
    is FirConstructorSymbol -> callableId.asSingleFqName().parent().child(SpecialNames.INIT)
    is FirValueParameterSymbol -> containingDeclarationSymbol.fqName.child(name)
    is FirCallableSymbol<*> -> callableId.asSingleFqName()
    is FirTypeParameterSymbol -> containingDeclarationSymbol.fqName.child(name)
    else -> throw AssertionError("Unexpected $this")
  }

private fun FirTypeRef.resolveJavaTypeIfNeeded(ctx: InjektContext): FirTypeRef =
  if (this is FirJavaTypeRef)
    resolveIfJavaType(ctx.session, MutableJavaTypeParameterStack(), null, FirJavaTypeConversionMode.DEFAULT)
  else this

fun FirBasedSymbol<*>.uniqueKey(ctx: InjektContext): String =
  ctx.cached("unique_key", this) {
    try {
      when (this) {
        is FirTypeParameterSymbol -> "typeparameter:${containingDeclarationSymbol.uniqueKey(ctx)}:$name"
        is FirClassLikeSymbol<*> -> "class_like:$fqName"
        is FirCallableSymbol<*> -> if (this != originalOrSelf()) originalOrSelf().uniqueKey(ctx)
        else {
          "callable:$fqName:" +
              typeParameterSymbols.joinToString(",") { it.name.asString() } + ":" +
              listOfNotNull(dispatchReceiverType, receiverParameter?.typeRef?.coneType)
                .plus(
                  resolvedContextParameters
                    .map {
                      it.returnTypeRef
                        .resolveJavaTypeIfNeeded(ctx)
                        .coneType
                    }
                )
                .plus(
                  (safeAs<FirFunctionSymbol<*>>()?.valueParameterSymbols ?: emptyList())
                    .map {
                      it.fir.returnTypeRef
                        .resolveJavaTypeIfNeeded(ctx)
                        .coneType
                    }
                )
                .joinToString(",") { it.uniqueTypeKey(ctx) } + ":" +
              fir.returnTypeRef.resolveJavaTypeIfNeeded(ctx).coneType.uniqueTypeKey(ctx)
        }
        else -> error("Unexpected declaration $this")
      }
    } catch (e: Throwable) {
      throw e
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

fun findCallableForKey(
  callableKey: String,
  callableFqName: FqName,
  ctx: InjektContext,
): FirCallableSymbol<*> = ctx.cached("callable_for_key", callableKey) {
  collectDeclarationsInFqName(callableFqName.parent(), ctx)
    .filterIsInstance<FirCallableSymbol<*>>()
    .firstOrNull { it.uniqueKey(ctx) == callableKey }
    ?: error("Could not find callable for $callableKey $callableFqName " +
        "parent ${callableFqName.parent()} " +
        "in ${collectDeclarationsInFqName(callableFqName.parent(), ctx).map { it to it.uniqueKey(ctx) }}")
}

fun findClassifier(
  classifierKey: String,
  classifierFqName: FqName,
  classifierClassId: ClassId?,
  ctx: InjektContext,
): FirClassifierSymbol<*> = ctx.cached("classifier_for_key", classifierKey) {
  classifierClassId?.let { ctx.session.symbolProvider.getClassLikeSymbolByClassId(it) }
    ?: collectDeclarationsInFqName(classifierFqName.parent(), ctx)
      .filterIsInstance<FirClassifierSymbol<*>>()
      .firstOrNull { it.fqName == classifierFqName }
    ?: collectDeclarationsInFqName(classifierFqName.parent().parent(), ctx)
      .filter { it.fqName.shortName() == classifierFqName.parent().shortName() }
      .flatMap { it.typeParameterSymbols ?: emptyList() }
      .firstOrNull { it.uniqueKey(ctx) == classifierKey }
    ?: error("Could not find classifier for $classifierKey $classifierFqName " +
        "${collectDeclarationsInFqName(classifierFqName.parent(), ctx).map { it.uniqueKey(ctx) }} " +
        "${collectDeclarationsInFqName(classifierFqName.parent().parent(), ctx).map { it.uniqueKey(ctx) }}")
}

fun collectDeclarationsInFqName(fqName: FqName, ctx: InjektContext): List<FirBasedSymbol<*>> =
  ctx.cached("declarations_in_fq_name", fqName) {
    val hasPackage = ctx.session.symbolProvider.hasPackage(fqName)

    if (fqName.isRoot || hasPackage)
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
      .firstOrNull { it.fqName.shortName() == fqName.shortName() }

    return@cached classSymbol?.declarationSymbols ?: emptyList()
  }

val isIde = try {
  Class.forName("org.jetbrains.kotlin.com.intellij.psi.PsiElement")
  false
} catch (_: ClassNotFoundException) {
  true
}
