/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.allParametersWithContext
import com.ivianuu.injekt.compiler.analysis.hasDefaultValueIgnoringContext
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.transform
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed interface Provider {
  val type: TypeRef
  val originalType: TypeRef get() = type
  val dependencies: List<ContextRequest> get() = emptyList()
  val dependencyScopes: Map<ContextRequest, ContextScope> get() = emptyMap()
  val callableFqName: FqName
  val callContext: CallContext get() = CallContext.DEFAULT
  val ownerScope: ContextScope
  val usageKey: Any get() = type
}

class CallableProvider(
  override val ownerScope: ContextScope,
  val callable: CallableRef
) : Provider {
  override val type: TypeRef
    get() = callable.type
  override val dependencies: List<ContextRequest> = callable.getContextRequests(ownerScope.ctx)
  override val callableFqName: FqName = if (callable.callable is ClassConstructorDescriptor)
    callable.callable.constructedClass.fqNameSafe
  else callable.callable.fqNameSafe
  override val callContext: CallContext
    get() = callable.callable.callContext(ownerScope.ctx)
  override val originalType: TypeRef
    get() = callable.originalType
  override val usageKey: Any =
    listOf(callable.callable.uniqueKey(ownerScope.ctx), callable.parameterTypes, callable.type)

  override fun equals(other: Any?): Boolean =
    other is CallableProvider && other.usageKey == usageKey

  override fun hashCode(): Int = usageKey.hashCode()
}

class ListProvider(
  override val type: TypeRef,
  override val ownerScope: ContextScope,
  elements: List<TypeRef>,
  val singleElementType: TypeRef,
  val collectionElementType: TypeRef
) : Provider {
  override val callableFqName: FqName = FqName("com.ivianuu.injekt.contextListOf")
  override val dependencies: List<ContextRequest> = elements
    .mapIndexed { index, element ->
      ContextRequest(
        type = element,
        callableFqName = callableFqName,
        callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
        parameterName = "element$index".asNameId(),
        parameterIndex = index
      )
    }
  override val dependencyScopes = dependencies.associateWith { ownerScope }
}

class FunctionProvider(
  override val type: TypeRef,
  override val ownerScope: ContextScope,
  val isInline: Boolean,
  dependencyCallContext: CallContext
) : Provider {
  override val callableFqName: FqName = when (type.callContext) {
    CallContext.DEFAULT -> FqName("providerOf")
    CallContext.COMPOSABLE -> FqName("composableProviderOf")
    CallContext.SUSPEND -> FqName("suspendProviderOf")
  }
  override val dependencies: List<ContextRequest> = listOf(
    ContextRequest(
      type = type.unwrapTags().arguments.last(),
      callableFqName = callableFqName,
      parameterName = "instance".asNameId(),
      parameterIndex = 0,
      isInline = isInline
    )
  )

  val parameterDescriptors = type
    .unwrapTags()
    .classifier
    .descriptor!!
    .cast<ClassDescriptor>()
    .unsubstitutedMemberScope
    .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
    .first()
    .valueParameters
    .map { ProviderValueParameterDescriptor(it) }

  // only create a new scope if we have parameters or a different call context then our parent
  override val dependencyScopes = mapOf(
    dependencies.single() to if (parameterDescriptors.isEmpty() &&
      ownerScope.callContext == dependencyCallContext) ownerScope
    else ContextScope(
      name = "PROVIDER $type",
      parent = ownerScope,
      ctx = ownerScope.ctx,
      callContext = dependencyCallContext,
      initialProviders = parameterDescriptors
        .mapIndexed { index, parameter ->
          parameter
            .toCallableRef(ownerScope.ctx)
            .copy(type = type.unwrapTags().arguments[index])
        }
    )
  )

  override val originalType: TypeRef
    get() = type.unwrapTags().classifier.defaultType

  // required to distinct between individual providers in codegen
  class ProviderValueParameterDescriptor(
    private val delegate: ValueParameterDescriptor
  ) : ValueParameterDescriptor by delegate
}

class SourceKeyProvider(
  override val type: TypeRef,
  override val ownerScope: ContextScope
) : Provider {
  override val callableFqName: FqName = FqName("com.ivianuu.injekt.common.sourceKey")
}

class TypeKeyProvider(
  override val type: TypeRef,
  override val ownerScope: ContextScope
) : Provider {
  override val callableFqName: FqName = FqName("com.ivianuu.injekt.common.typeKeyOf<${type.renderToString()}>")
  override val dependencies: List<ContextRequest> = run {
    val typeParameterDependencies = mutableSetOf<ClassifierRef>()
    type.allTypes.forEach {
      if (it.classifier.isTypeParameter)
        typeParameterDependencies += it.classifier
    }
    typeParameterDependencies
      .mapIndexed { index, typeParameter ->
        ContextRequest(
          type = ownerScope.ctx.typeKeyClassifier!!.defaultType
            .withArguments(listOf(typeParameter.defaultType)),
          callableFqName = callableFqName,
          callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
          parameterName = "${typeParameter.fqName.shortName()}Key".asNameId(),
          parameterIndex = index
        )
      }
  }
}

fun CallableRef.getContextRequests(ctx: Context): List<ContextRequest> = callable.allParametersWithContext
  .transform {
    if (it === callable.dispatchReceiverParameter ||
        it === callable.extensionReceiverParameter ||
        it in callable.contextReceiverParameters ||
        it.isProvide(ctx))
          add(it.toContextRequest(this@getContextRequests, ctx))
  }

data class ContextRequest(
  val type: TypeRef,
  val callableFqName: FqName,
  val callableTypeArguments: Map<ClassifierRef, TypeRef> = emptyMap(),
  val parameterName: Name,
  val parameterIndex: Int,
  val isRequired: Boolean = true,
  val isInline: Boolean = false
)

fun ParameterDescriptor.toContextRequest(callable: CallableRef, ctx: Context) =
  ContextRequest(
    type = callable.parameterTypes[injektIndex(ctx)]!!,
    callableFqName = containingDeclaration.safeAs<ConstructorDescriptor>()
      ?.constructedClass?.fqNameSafe ?: containingDeclaration.fqNameSafe,
    callableTypeArguments = callable.typeArguments,
    parameterName = injektName(ctx),
    parameterIndex = injektIndex(ctx),
    isRequired = this !is ValueParameterDescriptor || !hasDefaultValueIgnoringContext,
    isInline = callable.callable.safeAs<FunctionDescriptor>()?.isInline == true &&
        InlineUtil.isInlineParameter(this) &&
        safeAs<ValueParameterDescriptor>()?.isCrossinline != true
  )
