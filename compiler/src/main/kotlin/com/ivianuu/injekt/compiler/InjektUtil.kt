/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(IDEAPluginsCompatibilityAPI::class)

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.*
import kotlin.experimental.*

fun KtFunction.getArgumentDescriptor(ctx: Context): ValueParameterDescriptor? {
  val call = KtPsiUtil.getParentCallIfPresent(this) ?: return null
  val resolvedCall = call.getResolvedCall(ctx.trace!!.bindingContext) ?: return null
  val valueArgument = resolvedCall.call.getValueArgumentForExpression(this) ?: return null
  val mapping = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return null
  return mapping.valueParameter
}

fun <D : DeclarationDescriptor> KtDeclaration.descriptor(ctx: Context) =
  ctx.trace!!.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? D

fun DeclarationDescriptor.isExternalDeclaration(ctx: Context): Boolean =
  moduleName(ctx) != ctx.module.moduleName(ctx)

fun String.asNameId() = Name.identifier(this)

fun Annotated.hasAnnotation(fqName: FqName): Boolean =
  annotations.hasAnnotation(fqName)

fun Annotated.getTags(): List<AnnotationDescriptor> =
  annotations.filter {
    val inner = it.type.constructor.declarationDescriptor as ClassDescriptor
    inner.hasAnnotation(InjektFqNames.Tag) || it.fqName == InjektFqNames.Composable
  }

fun DeclarationDescriptor.uniqueKey(ctx: Context): String = ctx.cached("unique_key", original) {
  when (val original = this.original) {
    is ConstructorDescriptor -> "constructor:${original.constructedClass.fqNameSafe}:" +
        "${original.visibility.name}:" +
        "${
          original.valueParameters
            .joinToString(",") {
              it.type
                .fullyAbbreviatedType
                .uniqueTypeKey()
            }
        }:${original.returnType.fullyAbbreviatedType.uniqueTypeKey()}"
    is ClassDescriptor -> "class:$fqNameSafe"
    is FunctionDescriptor -> "function:$fqNameSafe:" +
        "${original.visibility.name}:" +
        original.typeParameters.joinToString {
          buildString {
            append(it.name.asString())
            it.upperBounds.forEach { upperBound ->
              append(
                upperBound
                  .fullyAbbreviatedType
                  .uniqueTypeKey()
              )
            }
          }
        } +
        listOfNotNull(original.dispatchReceiverParameter, original.extensionReceiverParameter)
          .plus(original.valueParameters)
          .joinToString(",") { parameter ->
            buildString {
              when {
                parameter === original.dispatchReceiverParameter -> append("d:")
                parameter === original.extensionReceiverParameter -> append("e:")
                else -> append("p:")
              }
              append(
                parameter.type
                  .fullyAbbreviatedType
                  .uniqueTypeKey()
              )
            }
          } +
        ":" +
        original.returnType?.fullyAbbreviatedType?.uniqueTypeKey().orEmpty()
    is PropertyDescriptor -> "property:$fqNameSafe:" +
        "${original.visibility.name}:" +
        original.typeParameters.joinToString {
          buildString {
            append(it.name.asString())
            it.upperBounds.forEach { upperBound ->
              append(
                upperBound
                  .fullyAbbreviatedType
                  .uniqueTypeKey()
              )
            }
          }
        } +
        listOfNotNull(original.dispatchReceiverParameter, original.extensionReceiverParameter)
          .joinToString(",") { parameter ->
            buildString {
              when {
                parameter === original.dispatchReceiverParameter -> append("d:")
                parameter === original.extensionReceiverParameter -> append("e:")
                else -> append("p:")
              }
              append(
                parameter.type
                  .fullyAbbreviatedType
                  .uniqueTypeKey()
              )
            }
          } +
        ":" +
        original.returnType?.fullyAbbreviatedType?.uniqueTypeKey().orEmpty()
    is TypeAliasDescriptor -> "typealias:$fqNameSafe"
    is TypeParameterDescriptor ->
      "typeparameter:$fqNameSafe:${containingDeclaration!!.uniqueKey(ctx)}"
    is ReceiverParameterDescriptor -> "receiver:$fqNameSafe:${original.type.fullyAbbreviatedType.uniqueTypeKey()}"
    is ValueParameterDescriptor -> "value_parameter:$fqNameSafe:${original.type.fullyAbbreviatedType.uniqueTypeKey()}"
    is VariableDescriptor -> "variable:${fqNameSafe}:${original.type.fullyAbbreviatedType.uniqueTypeKey()}"
    else -> error("Unexpected declaration $this")
  }
}

fun KotlinType.uniqueTypeKey(depth: Int = 0): String {
  if (depth > 15) return ""
  return buildString {
    append(constructor.declarationDescriptor!!.fqNameSafe)
    arguments.forEachIndexed { index, typeArgument ->
      if (index == 0) append("<")
      append(typeArgument.type.fullyAbbreviatedType.uniqueTypeKey(depth + 1))
      if (index != arguments.lastIndex) append(", ")
      else append(">")
    }
    if (isMarkedNullable) append("?")
  }
}

val KotlinType.fullyAbbreviatedType: KotlinType
  get() {
    val abbreviatedType = getAbbreviatedType()
    return if (abbreviatedType != null && abbreviatedType != this) abbreviatedType.fullyAbbreviatedType else this
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
