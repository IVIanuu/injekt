/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, IDEAPluginsCompatibilityAPI::class)

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.kotlin.getJvmModuleNameForDeserializedDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentForExpression
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.experimental.ExperimentalTypeInference

fun KtFunction.getArgumentDescriptor(ctx: Context): ValueParameterDescriptor? {
  val call = KtPsiUtil.getParentCallIfPresent(this) ?: return null
  val resolvedCall = call.getResolvedCall(ctx.trace!!.bindingContext) ?: return null
  val valueArgument = resolvedCall.call.getValueArgumentForExpression(this) ?: return null
  val mapping = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return null
  return mapping.valueParameter
}

fun PropertyDescriptor.primaryConstructorPropertyValueParameter(
  ctx: Context
): ValueParameterDescriptor? = overriddenTreeUniqueAsSequence(false)
  .map { it.containingDeclaration }
  .filterIsInstance<ClassDescriptor>()
  .mapNotNull { clazz ->
    if (clazz.isDeserializedDeclaration()) {
      clazz.unsubstitutedPrimaryConstructor
        ?.valueParameters
        ?.firstOrNull {
          it.name == name &&
              it.name.asString() in clazz.classifierInfo(ctx).primaryConstructorPropertyParameters
        }
    } else {
      clazz.unsubstitutedPrimaryConstructor
        ?.valueParameters
        ?.firstOrNull {
          it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true &&
              it.name == name
        }
    }
  }
  .firstOrNull()

fun <D : DeclarationDescriptor> KtDeclaration.descriptor(ctx: Context) =
  ctx.trace!!.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? D

fun DeclarationDescriptor.isExternalDeclaration(ctx: Context): Boolean =
  moduleName(ctx) != ctx.module.moduleName(ctx)

fun DeclarationDescriptor.isDeserializedDeclaration(): Boolean = this is DeserializedDescriptor ||
    (this is PropertyAccessorDescriptor && correspondingProperty.isDeserializedDeclaration()) ||
    this is DeserializedTypeParameterDescriptor ||
    this is JavaClassDescriptor ||
    this is FunctionClassDescriptor

fun String.asNameId() = Name.identifier(this)

fun Annotated.hasAnnotation(fqName: FqName): Boolean =
  annotations.hasAnnotation(fqName)

fun Annotated.getTags(): List<AnnotationDescriptor> =
  annotations.filter {
    val inner = it.type.constructor.declarationDescriptor as ClassDescriptor
    inner.hasAnnotation(InjektClassIds.Tag) || it.fqName == InjektClassIds.Composable
  }

fun DeclarationDescriptor.uniqueKey(ctx: Context): String = ctx.cached("unique_key", original) {
  when (val original = this.original) {
    is ConstructorDescriptor -> "constructor:${original.constructedClass.fqNameSafe}:" +
        "${original.visibility.name}:" +
        "${
          original.contextReceiverParameters
            .plus(original.valueParameters)
            .joinToString(",") {
              it.type
                .fullyAbbreviatedType
                .uniqueTypeKey()
            }
        }:${original.returnType.fullyAbbreviatedType.uniqueTypeKey()}"
    is ClassDescriptor -> "class:$fqNameSafe"
    is AnonymousFunctionDescriptor -> "anonymous_function:${findPsi()!!.let {
      "${it.containingFile.cast<KtFile>().virtualFilePath}_${it.startOffset}_${it.endOffset}"
    }}:${original.returnType?.fullyAbbreviatedType?.uniqueTypeKey().orEmpty()}"
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
          .plus(original.contextReceiverParameters)
          .plus(original.valueParameters)
          .joinToString(",") { parameter ->
            buildString {
              when {
                parameter === original.dispatchReceiverParameter -> append("d:")
                parameter === original.extensionReceiverParameter -> append("e:")
                parameter in original.contextReceiverParameters -> append(":c")
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
          .plus(original.contextReceiverParameters)
          .joinToString(",") { parameter ->
            buildString {
              when {
                parameter === original.dispatchReceiverParameter -> append("d:")
                parameter === original.extensionReceiverParameter -> append("e:")
                parameter in original.contextReceiverParameters -> append(":c")
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

fun ParameterDescriptor.injektName(ctx: Context): Name {
  val index = injektIndex(ctx)
  val parentContextReceivers = containingDeclaration.safeAs<CallableDescriptor>()?.contextReceiverParameters
    ?: contextReceiverParameters.safeAs<ClassDescriptor>()?.contextReceivers
    ?: emptyList()

  return when  {
    index == DISPATCH_RECEIVER_INDEX -> DISPATCH_RECEIVER_NAME
    index == EXTENSION_RECEIVER_INDEX -> EXTENSION_RECEIVER_NAME
    this is ReceiverParameterDescriptor &&
        parentContextReceivers.isNotEmpty() &&
        index <= parentContextReceivers.size ->
      Name.identifier("\$contextReceiver_$index")
    else -> name
  }
}

const val DISPATCH_RECEIVER_INDEX = -2
const val EXTENSION_RECEIVER_INDEX = -1

fun ParameterDescriptor.injektIndex(ctx: Context): Int =
  ctx.cached("injekt_index", this) {
    val callable = containingDeclaration as? CallableDescriptor
    when (original) {
      callable?.dispatchReceiverParameter?.original, is LazyClassReceiverParameterDescriptor -> DISPATCH_RECEIVER_INDEX
      callable?.extensionReceiverParameter?.original -> EXTENSION_RECEIVER_INDEX
      else -> {
        val contextReceivers = (containingDeclaration
          .safeAs<ReceiverParameterDescriptor>()
          ?.value
          ?.safeAs<ImplicitClassReceiver>()
          ?.classDescriptor
          ?.contextReceivers ?: callable?.contextReceiverParameters ?: containingDeclaration.safeAs<ClassDescriptor>()?.contextReceivers)

        val contextReceiverIndex = contextReceivers?.indexOfFirst {
          // todo find a better way to get the correct index
          it.type.fullyAbbreviatedType == type.fullyAbbreviatedType
        }

        if (contextReceiverIndex != null && contextReceiverIndex != -1)
          contextReceiverIndex
        else {
          val valueParameterIndex = callable?.valueParameters?.indexOfFirst {
            original === it.original
          }
          if (valueParameterIndex != null && valueParameterIndex != -1)
            callable.contextReceiverParameters.size + valueParameterIndex
          else if (this is ReceiverParameterDescriptor && containingDeclaration is ClassDescriptor)
            DISPATCH_RECEIVER_INDEX
          else
            throw AssertionError("Unexpected descriptor $this $javaClass $fqNameSafe $type")
        }
      }
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

val CallableDescriptor.allParametersWithContext: List<ParameterDescriptor>
  get() = buildList {
    addIfNotNull(dispatchReceiverParameter)
    addIfNotNull(extensionReceiverParameter)
    addAll(contextReceiverParameters)
    addAll(valueParameters)
  }

val IrFunction.allParametersWithContext: List<IrValueParameter>
  get() = buildList {
    addIfNotNull(dispatchReceiverParameter)
    addIfNotNull(extensionReceiverParameter)
    addAll(valueParameters)
  }
