package com.ivianuu.injekt.compiler.di.old

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.scopes.*
import kotlin.experimental.*

fun DeclarationDescriptor.isExternalDeclaration(ctx: Context): Boolean =
  moduleName(ctx) != ctx.module.moduleName(ctx)

fun Annotated.hasAnnotation(classId: ClassId): Boolean =
  annotations.hasAnnotation(classId.asSingleFqName())

fun Annotated.getTags(): List<AnnotationDescriptor> =
  annotations.filter {
    val inner = it.type.constructor.declarationDescriptor as ClassDescriptor
    inner.hasAnnotation(InjektFqNames.Tag) || it.fqName == InjektFqNames.Composable.asSingleFqName()
  }

val DISPATCH_RECEIVER_NAME = Name.identifier("\$dispatchReceiver")
val EXTENSION_RECEIVER_NAME = Name.identifier("\$extensionReceiver")

fun ParameterDescriptor.injektName(): Name = when (injektIndex()) {
  DISPATCH_RECEIVER_INDEX -> DISPATCH_RECEIVER_NAME
  EXTENSION_RECEIVER_INDEX -> EXTENSION_RECEIVER_NAME
  else -> name
}

fun classifierDescriptorForFqName(
  fqName: FqName,
  lookupLocation: LookupLocation,
  ctx: Context
): ClassifierDescriptor? = if (fqName.isRoot) null
else memberScopeForFqName(fqName.parent(), lookupLocation, ctx)
  ?.getContributedClassifier(fqName.shortName(), lookupLocation)

fun memberScopeForFqName(
  fqName: FqName,
  lookupLocation: LookupLocation,
  ctx: Context
): MemberScope? {
  val pkg = ctx.module.getPackage(fqName)

  if (fqName.isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope

  val parentMemberScope = memberScopeForFqName(fqName.parent(), lookupLocation, ctx) ?: return null

  val classDescriptor = parentMemberScope.getContributedClassifier(
    fqName.shortName(),
    lookupLocation
  ) as? ClassDescriptor ?: return null

  return classDescriptor.unsubstitutedMemberScope
}

fun ParameterDescriptor.injektIndex(): Int = if (this is ValueParameterDescriptor) index else {
  val callable = containingDeclaration as? CallableDescriptor
  when {
    original == callable?.dispatchReceiverParameter?.original ||
        (this is ReceiverParameterDescriptor && containingDeclaration is ClassDescriptor) -> DISPATCH_RECEIVER_INDEX
    original == callable?.extensionReceiverParameter?.original -> EXTENSION_RECEIVER_INDEX
    else -> throw AssertionError("Unexpected descriptor $this")
  }
}

fun DeclarationDescriptor.moduleName(ctx: Context): String =
  getJvmModuleNameForDeserializedDescriptor(this)
    ?.removeSurrounding("<", ">")
    ?: ctx.module.name.asString().removeSurrounding("<", ">")

@OptIn(ExperimentalTypeInference::class)
inline fun <T, R> Collection<T>.transform(@BuilderInference block: MutableList<R>.(T) -> Unit): List<R> =
  mutableListOf<R>().apply {
    for (item in this@transform)
      block(item)
  }

const val DISPATCH_RECEIVER_INDEX = -2
const val EXTENSION_RECEIVER_INDEX = -1
