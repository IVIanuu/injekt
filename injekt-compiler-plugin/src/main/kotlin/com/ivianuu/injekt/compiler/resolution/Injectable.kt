/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.analysis.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.inline.*
import org.jetbrains.kotlin.utils.addToStdlib.*

sealed interface Injectable {
  val type: TypeRef
  val originalType: TypeRef get() = type
  val dependencies: List<InjectableRequest> get() = emptyList()
  val dependencyScopes: Map<InjectableRequest, InjectablesScope> get() = emptyMap()
  val callableFqName: FqName
  val callContext: CallContext get() = CallContext.DEFAULT
  val ownerScope: InjectablesScope
  val usageKey: Any get() = type
}

class CallableInjectable(
  override val ownerScope: InjectablesScope,
  val callable: CallableRef
) : Injectable {
  override val type: TypeRef
    get() = callable.type
  override val dependencies: List<InjectableRequest> = callable.getInjectableRequests(ownerScope.ctx)
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
    other is CallableInjectable && other.usageKey == usageKey

  override fun hashCode(): Int = usageKey.hashCode()
}

class ListInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope,
  elements: List<TypeRef>,
  val singleElementType: TypeRef,
  val collectionElementType: TypeRef
) : Injectable {
  override val callableFqName: FqName = FqName("com.ivianuu.injekt.injectListOf")
  override val dependencies: List<InjectableRequest> = elements
    .mapIndexed { index, element ->
      InjectableRequest(
        type = element,
        callableFqName = callableFqName,
        callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
        parameterName = "element$index".asNameId(),
        parameterIndex = index
      )
    }
  override val dependencyScopes = dependencies.associateWith { ownerScope }
}

class ProviderInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope,
  val isInline: Boolean,
  dependencyCallContext: CallContext
) : Injectable {
  override val callableFqName: FqName = when (type.callContext(ownerScope.ctx)) {
    CallContext.DEFAULT -> FqName("providerOf")
    CallContext.COMPOSABLE -> FqName("composableProviderOf")
    CallContext.SUSPEND -> FqName("suspendProviderOf")
  }
  override val dependencies: List<InjectableRequest> = listOf(
    InjectableRequest(
      type = type.arguments.last(),
      callableFqName = callableFqName,
      parameterName = "instance".asNameId(),
      parameterIndex = 0,
      isInline = isInline,
      isLazy = !isInline,
      isRequired = !type.arguments.last().isNullableType
    )
  )

  val parameterDescriptors = type
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
    else InjectablesScope(
      name = "PROVIDER $type",
      parent = ownerScope,
      ctx = ownerScope.ctx,
      callContext = dependencyCallContext,
      initialInjectables = parameterDescriptors
        .mapIndexed { index, parameter ->
          parameter
            .toCallableRef(ownerScope.ctx)
            .copy(type = type.arguments[index])
        }
    )
  )

  override val originalType: TypeRef
    get() = type.classifier.defaultType

  // required to distinct between individual providers in codegen
  class ProviderValueParameterDescriptor(
    private val delegate: ValueParameterDescriptor
  ) : ValueParameterDescriptor by delegate
}

class SourceKeyInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope
) : Injectable {
  override val callableFqName: FqName = FqName("com.ivianuu.injekt.common.sourceKey")
}

class TypeKeyInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope
) : Injectable {
  override val callableFqName: FqName = FqName("com.ivianuu.injekt.common.typeKeyOf<${type.renderToString()}>")
  override val dependencies: List<InjectableRequest> = run {
    val typeParameterDependencies = mutableSetOf<ClassifierRef>()
    type.allTypes.forEach {
      if (it.classifier.isTypeParameter)
        typeParameterDependencies += it.classifier
    }
    typeParameterDependencies
      .mapIndexed { index, typeParameter ->
        InjectableRequest(
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

fun CallableRef.getInjectableRequests(ctx: Context): List<InjectableRequest> = callable.allParameters
  .transform {
    if ((callable !is ClassConstructorDescriptor || it.name.asString() != "<this>") &&
        it === callable.dispatchReceiverParameter ||
        it === callable.extensionReceiverParameter ||
        it.isProvide(ctx))
          add(it.toInjectableRequest(this@getInjectableRequests, ctx))
  }

data class InjectableRequest(
  val type: TypeRef,
  val callableFqName: FqName,
  val callableTypeArguments: Map<ClassifierRef, TypeRef> = emptyMap(),
  val parameterName: Name,
  val parameterIndex: Int,
  val isRequired: Boolean = true,
  val isInline: Boolean = false,
  val isLazy: Boolean = false
)

fun ParameterDescriptor.toInjectableRequest(
  callable: CallableRef,
  ctx: Context
): InjectableRequest = InjectableRequest(
  type = callable.parameterTypes[injektIndex()]!!,
  callableFqName = containingDeclaration.safeAs<ConstructorDescriptor>()
    ?.constructedClass?.fqNameSafe ?: containingDeclaration.fqNameSafe,
  callableTypeArguments = callable.typeArguments,
  parameterName = injektName(),
  parameterIndex = injektIndex(),
  isRequired = this !is ValueParameterDescriptor || !hasDefaultValueIgnoringInject,
  isInline = callable.callable.safeAs<FunctionDescriptor>()?.isInline == true &&
      InlineUtil.isInlineParameter(this)
)
