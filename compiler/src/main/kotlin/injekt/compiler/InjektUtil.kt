/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, SymbolInternals::class,
  DeprecatedForRemovalCompilerApi::class, DirectDeclarationsAccess::class
)

package injekt.compiler

import injekt.compiler.fir.*
import injekt.compiler.resolution.*
import org.jetbrains.kotlin.*
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
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import kotlin.experimental.*

fun String.asNameId() = Name.identifier(this)

context(ctx: InjektContext)
fun FirBasedSymbol<*>.isInjectable(): Boolean {
  if (hasAnnotation(InjektFqNames.Provide, session)) return true

  if (this is FirConstructorSymbol && isPrimary &&
    resolvedReturnType.toRegularClassSymbol(session)!!.isInjectable())
    return true

  if (this is FirValueParameterSymbol) {
    if (resolvedReturnType.toInjektType().isProvide)
      return true
    val metadata = containingDeclarationSymbol.cast<FirFunctionSymbol<*>>().callableMetadata()
    if (name in metadata.injectParameters)
      return true
  }

  return false
}

context(ctx: InjektContext)
fun List<FirAnnotation>.getTagAnnotations(): List<FirAnnotation> = filter {
  it.resolvedType.toRegularClassSymbol(session)?.isTagAnnotation() == true
}

context(ctx: InjektContext)
fun FirClassifierSymbol<*>.isTagAnnotation(): Boolean =
  this is FirRegularClassSymbol && hasAnnotation(InjektFqNames.Tag, session)

val FirBasedSymbol<*>.fqName: FqName
  get() = when (this) {
    is FirClassLikeSymbol<*> -> classId.asSingleFqName()
    is FirConstructorSymbol -> callableId.asSingleFqName().parent().child(SpecialNames.INIT)
    is FirValueParameterSymbol -> containingDeclarationSymbol.fqName.child(injektName())
    is FirCallableSymbol<*> -> callableId?.asSingleFqName() ?: FqName(name.asString())
    is FirTypeParameterSymbol -> containingDeclarationSymbol.fqName.child(name)
    else -> throw AssertionError("Unexpected $this")
  }

context(ctx: InjektContext)
private fun FirTypeRef.resolveJavaTypeIfNeeded(): FirTypeRef =
  if (this is FirJavaTypeRef)
    resolveIfJavaType(
      session,
      MutableJavaTypeParameterStack(),
      null,
      FirJavaTypeConversionMode.DEFAULT
    )
  else this

context(ctx: InjektContext)
fun FirBasedSymbol<*>.uniqueKey(): String = cached("unique_key", this) {
  try {
    when (this) {
      is FirTypeParameterSymbol -> "typeparameter:${containingDeclarationSymbol.uniqueKey()}:$name"
      is FirClassLikeSymbol<*> -> "class_like:$fqName"
      is FirCallableSymbol<*> -> if (this != originalOrSelf()) originalOrSelf().uniqueKey()
      else {
        "callable:$fqName:" +
            typeParameterSymbols.joinToString(",") { it.name.asString() } + ":" +
            listOfNotNull(dispatchReceiverType, resolvedReceiverType)
              .plus(
                contextParameterSymbols
                  .map {
                    it.fir.returnTypeRef
                      .resolveJavaTypeIfNeeded()
                      .coneType
                  }
              )
              .plus(
                (this.safeAs<FirFunctionSymbol<*>>()?.valueParameterSymbols ?: emptyList())
                  .map {
                    it.fir.returnTypeRef
                      .resolveJavaTypeIfNeeded()
                      .coneType
                  }
              )
              .joinToString(",") { it.uniqueTypeKey() } + ":" +
            fir.returnTypeRef.resolveJavaTypeIfNeeded().coneType.uniqueTypeKey()
      }
      else -> error("Unexpected declaration $this")
    }
  } catch (e: Throwable) {
    throw e
  }
}

context(ctx: InjektContext)
fun ConeKotlinType.uniqueTypeKey(): String = cached("unique_key", this) {
  buildString {
    val finalType = fullyExpandedType(session)
    append(
      finalType.safeAs<ConeClassLikeType>()?.classId?.asString()
        ?: finalType.safeAs<ConeLookupTagBasedType>()?.lookupTag?.name?.asString()
    )
    finalType.typeArguments.joinToString(",") {
      if (it.isStarProjection) "*"
      else it.type?.uniqueTypeKey().orEmpty()
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

fun FirValueParameterSymbol.injektName(): Name {
  if (!name.isSpecial) return name
  val containing = containingDeclarationSymbol.cast<FirCallableSymbol<*>>()
  val indexInContextParameters = containing.fir.contextParameters.indexOf(fir)
  if (indexInContextParameters != -1)
    return Name.identifier("\$contextParameter$indexInContextParameters")
  return name
}

fun IrValueParameter.injektName(): Name {
  if (!name.isSpecial) return name
  val containing = parent.cast<IrFunction>()
  val indexInContextParameters = containing.valueParameters
    .filter { it.kind == IrParameterKind.Context }
    .indexOf(this)
  if (indexInContextParameters != -1)
    return Name.identifier("\$contextParameter$indexInContextParameters")
  return name
}

context(ctx: InjektContext)
fun findCallableForKey(
  callableKey: String,
  callableFqName: FqName
): FirCallableSymbol<*> = cached("callable_for_key", callableKey) {
  collectDeclarationsInFqName(callableFqName.parent())
    .filterIsInstance<FirCallableSymbol<*>>()
    .firstOrNull { it.uniqueKey() == callableKey }
    ?: error("Could not find callable for $callableKey $callableFqName " +
        "parent ${callableFqName.parent()} " +
        "in ${collectDeclarationsInFqName(callableFqName.parent()).map { it to it.uniqueKey() }}")
}

context(ctx: InjektContext)
fun findClassifier(
  classifierKey: String,
  classifierFqName: FqName,
  classifierClassId: ClassId?
): FirClassifierSymbol<*> = cached("classifier_for_key", classifierKey) {
  classifierClassId?.let { session.symbolProvider.getClassLikeSymbolByClassId(it) }
    ?: collectDeclarationsInFqName(classifierFqName.parent())
      .filterIsInstance<FirClassifierSymbol<*>>()
      .firstOrNull { it.fqName == classifierFqName }
    ?: collectDeclarationsInFqName(classifierFqName.parent().parent())
      .filter { it.fqName.shortName() == classifierFqName.parent().shortName() }
      .flatMap { it.typeParameterSymbols ?: emptyList() }
      .firstOrNull { it.uniqueKey() == classifierKey }
    ?: error("Could not find classifier for $classifierKey $classifierFqName " +
        "${collectDeclarationsInFqName(classifierFqName.parent()).map { it.uniqueKey() }} " +
        "${collectDeclarationsInFqName(classifierFqName.parent().parent()).map { it.uniqueKey() }}")
}

context(ctx: InjektContext)
fun collectDeclarationsInFqName(fqName: FqName): List<FirBasedSymbol<*>> =
  cached("declarations_in_fq_name", fqName) {
    val hasPackage = session.symbolProvider.hasPackage(fqName)

    if (fqName.isRoot || hasPackage)
      return@cached buildList {
        session.symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(fqName)
          ?.mapNotNull {
            session.symbolProvider.getClassLikeSymbolByClassId(ClassId(fqName, it))
          }
          ?.forEach { add(it) }

        session.symbolProvider.symbolNamesProvider.getTopLevelCallableNamesInPackage(fqName)
          ?.flatMap { name ->
            session.symbolProvider.getTopLevelCallableSymbols(fqName, name)
          }
          ?.forEach { add(it) }
      }

    val parentDeclarations = collectDeclarationsInFqName(fqName.parent())
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
