/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, UnsafeDuringIrConstructionAPI::class)

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import kotlin.experimental.*

fun <D : DeclarationDescriptor> KtDeclaration.descriptor(ctx: InjektContext): D? =
  TODO()

fun String.asNameId() = Name.identifier(this)

fun List<FirAnnotation>.getTags(ctx: InjektContext): List<FirAnnotation> = filter {
  it.resolvedType.toRegularClassSymbol(ctx.session)?.isTag(ctx) == true
}

fun FirClassifierSymbol<*>.isTag(ctx: InjektContext): Boolean =
  this is FirRegularClassSymbol &&
      (hasAnnotation(InjektFqNames.Tag, ctx.session) || classId == InjektFqNames.Composable)

fun DeclarationDescriptor.uniqueKey(ctx: InjektContext): String = ctx.cached("unique_key", original) {
  when (this) {
    is TypeParameterDescriptor -> typeParameterUniqueKey(name, containingDeclaration.fqNameSafe, containingDeclaration.uniqueKey(ctx))
    is ClassifierDescriptor -> classLikeUniqueKey(fqNameSafe)
    is LocalVariableDescriptor -> localVariableUniqueKey(
      fqNameSafe,
      returnType.uniqueTypeKey(),
      containingDeclaration.uniqueKey(ctx)
    )
    is CallableDescriptor -> callableUniqueKey(
      fqNameSafe,
      typeParameters.map { it.name },
      listOfNotNull(original.dispatchReceiverParameter, original.extensionReceiverParameter)
        .plus(original.valueParameters)
        .map { it.type.uniqueTypeKey() },
      returnType!!.uniqueTypeKey()
    )
    else -> error("Unexpected declaration $this")
  }
}

fun KotlinType.uniqueTypeKey(): String = uniqueTypeKey(
  classIdOrName = {
    constructor.declarationDescriptor.safeAs<TypeParameterDescriptor>()?.name?.asString()
      ?: constructor.declarationDescriptor?.classId?.asString()
      ?: constructor.declarationDescriptor.safeAs<ClassDescriptor>()
        ?.takeIf { it.visibility == DescriptorVisibilities.LOCAL }
        ?.name
        ?.takeIf { it != SpecialNames.NO_NAME_PROVIDED }
        ?.asString()
  },
  arguments = {
    arguments.map { if (it.isStarProjection) null else it.type }
  },
  isMarkedNullable = { isMarkedNullable }
)

val FirBasedSymbol<*>.fqName: FqName
  get() = when (this) {
    is FirClassLikeSymbol<*> -> classId.asSingleFqName()
    is FirCallableSymbol<*> -> callableId.asSingleFqName()
    is FirTypeParameterSymbol -> containingDeclarationSymbol.fqName.child(name)
    else -> throw AssertionError("Unexpected $this")
  }

fun FirBasedSymbol<*>.uniqueKey(ctx: InjektContext): String =
  when (this) {
    is FirTypeParameterSymbol -> typeParameterUniqueKey(
      name,
      containingDeclarationSymbol.fqName,
      containingDeclarationSymbol.uniqueKey(ctx)
    )
    is FirClassLikeSymbol<*> -> classLikeUniqueKey(fqName)
    is FirCallableSymbol<*> -> callableUniqueKey(
      fqName,
      typeParameterSymbols.map { it.name },
      listOfNotNull(dispatchReceiverType, receiverParameter?.typeRef?.coneType)
        .plus(
          (safeAs<FirFunctionSymbol<*>>()?.valueParameterSymbols ?: emptyList())
            .map { it.resolvedReturnType }
        )
        .map { it.uniqueTypeKey() },
      resolvedReturnType.uniqueTypeKey()
    )
    else -> error("Unexpected declaration $this")
  }

fun ConeKotlinType.uniqueTypeKey(): String = uniqueTypeKey(
  classIdOrName = {
    safeAs<ConeClassLikeType>()?.classId?.asString()
      ?: safeAs<ConeLookupTagBasedType>()?.lookupTag?.name?.asString()
  },
  arguments = {
    typeArguments.map { if (it.isStarProjection) null else it.type }
  },
  isMarkedNullable = { isMarkedNullable }
)

fun IrSymbol.uniqueKey(ctx: InjektContext): String = when (this) {
  is IrTypeParameterSymbol -> typeParameterUniqueKey(
    owner.name,
    owner.parent.kotlinFqName,
    owner.parent.cast<IrDeclaration>().symbol.uniqueKey(ctx)
  )
  is IrClassSymbol -> classLikeUniqueKey(owner.fqNameForIrSerialization)
  is IrFunctionSymbol -> callableUniqueKey(
    owner.kotlinFqName,
    (if (this is IrConstructor) constructedClass.typeParameters else owner.typeParameters).map { it.name },
    listOfNotNull(owner.dispatchReceiverParameter, owner.extensionReceiverParameter)
      .plus(owner.valueParameters)
      .map { it.type.uniqueTypeKey() },
    owner.returnType.uniqueTypeKey()
  )
  is IrPropertySymbol -> callableUniqueKey(
    owner.parent.kotlinFqName.child(owner.name),
    owner.getter!!.typeParameters.map { it.name },
    listOfNotNull(owner.getter!!.dispatchReceiverParameter, owner.getter!!.extensionReceiverParameter)
      .map { it.type.uniqueTypeKey() },
    owner.getter!!.returnType.uniqueTypeKey()
  )
  is IrVariableSymbol -> localVariableUniqueKey(
    owner.parent.kotlinFqName.child(owner.name),
    owner.type.uniqueTypeKey(),
    owner.parent.cast<IrDeclaration>().symbol.uniqueKey(ctx)
  )
  else -> error("Unexpected declaration $this")
}

fun IrType.uniqueTypeKey(): String = uniqueTypeKey(
  classIdOrName = {
    classOrNull?.owner?.classId?.asString()
      ?: classifierOrNull?.owner?.cast<IrDeclarationWithName>()?.name
        ?.takeIf { it != SpecialNames.NO_NAME_PROVIDED }
        ?.asString()
  },
  arguments = { safeAs<IrSimpleType>()?.arguments?.map { it.typeOrNull } ?: emptyList() },
  isMarkedNullable = { isMarkedNullable() }
)

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
  fqName: FqName,
  typeUniqueKey: String,
  parentUniqueKey: String
) = "variable:${fqName}:$typeUniqueKey:${parentUniqueKey}"

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

fun ParameterDescriptor.injektName(): Name = when (injektIndex()) {
  DISPATCH_RECEIVER_INDEX -> DISPATCH_RECEIVER_NAME
  EXTENSION_RECEIVER_INDEX -> EXTENSION_RECEIVER_NAME
  else -> name
}

const val DISPATCH_RECEIVER_INDEX = -2
const val EXTENSION_RECEIVER_INDEX = -1

val CallableDescriptor.callableId get() = CallableId(
  findPackage().fqName,
  containingDeclaration.safeAs<ClassDescriptor>()?.classId?.relativeClassName,
  name
)

fun ParameterDescriptor.injektIndex(): Int = if (this is ValueParameterDescriptor) index else {
  val callable = containingDeclaration as? CallableDescriptor
  when {
    original == callable?.dispatchReceiverParameter?.original ||
        (this is ReceiverParameterDescriptor && containingDeclaration is ClassDescriptor) -> DISPATCH_RECEIVER_INDEX
    original == callable?.extensionReceiverParameter?.original -> EXTENSION_RECEIVER_INDEX
    else -> throw AssertionError("Unexpected descriptor $this")
  }
}

fun IrDeclaration.injektIndex(): Int {
  if (parent is IrClass) return DISPATCH_RECEIVER_INDEX
  val callable = parent.cast<IrFunction>()
  return when {
    this == callable.dispatchReceiverParameter -> DISPATCH_RECEIVER_INDEX
    this == callable.extensionReceiverParameter -> EXTENSION_RECEIVER_INDEX
    else -> callable.valueParameters.indexOf(this)
  }
}

fun classifierDescriptorForFqName(
  fqName: FqName,
  lookupLocation: LookupLocation,
  ctx: InjektContext
): ClassifierDescriptor? =
  if (fqName.isRoot) null else memberScopeForFqName(fqName.parent(), lookupLocation, ctx)
    ?.getContributedClassifier(fqName.shortName(), lookupLocation)

fun findCallableSymbol(
  callableKey: String,
  callableFqName: FqName,
  ctx: InjektContext,
): FirCallableSymbol<*> = collectDeclarationsInFqName(callableFqName.parent(), ctx)
  .filterIsInstance<FirCallableSymbol<*>>()
  .singleOrNull { it.uniqueKey(ctx) == callableKey }
  ?: error("Could not find callable for $callableKey $callableFqName")

fun findClassifierSymbol(
  classifierKey: String,
  classifierFqName: FqName,
  ctx: InjektContext,
): FirClassifierSymbol<*> = getClassifierForFqName(classifierFqName, ctx)
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

fun getClassifierForFqName(fqName: FqName, ctx: InjektContext) =
  collectDeclarationsInFqName(fqName.parent(), ctx)
    .singleOrNull { it.fqName == fqName }
    ?.safeAs<FirClassLikeSymbol<*>>()

fun collectDeclarationsInFqName(fqName: FqName, ctx: InjektContext): List<FirBasedSymbol<*>> {
  val packageFqName = ctx.session.symbolProvider.getPackage(fqName)

  if (fqName.isRoot || packageFqName != null)
    return buildList {
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
    .takeIf { it.isNotEmpty() } ?: return emptyList()

  val classSymbol = parentDeclarations
    .singleOrNull { it.fqName.shortName() == fqName.shortName() }
    ?.safeAs<FirRegularClassSymbol>()

  return classSymbol?.declarationSymbols ?: emptyList()
}

fun memberScopeForFqName(
  fqName: FqName,
  lookupLocation: LookupLocation,
  ctx: InjektContext
): MemberScope? {
  return TODO()
}
