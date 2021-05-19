/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.transform.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.inline.*
import org.jetbrains.kotlin.utils.addToStdlib.*

sealed class Injectable {
  abstract val type: TypeRef
  abstract val originalType: TypeRef
  abstract val dependencies: List<InjectableRequest>
  abstract val dependencyScope: InjectablesScope?
  abstract val callableFqName: FqName
  abstract val callContext: CallContext
  abstract val ownerScope: InjectablesScope
  abstract val cacheExpressionResultIfPossible: Boolean
}

class CallableInjectable(
  override val type: TypeRef,
  override val dependencies: List<InjectableRequest>,
  override val ownerScope: InjectablesScope,
  val callable: CallableRef,
) : Injectable() {
  override val callableFqName: FqName = if (callable.callable is ClassConstructorDescriptor)
    callable.callable.constructedClass.fqNameSafe
  else callable.callable.fqNameSafe
  override val callContext: CallContext
    get() = callable.callContext
  override val dependencyScope: InjectablesScope?
    get() = null
  override val originalType: TypeRef
    get() = callable.originalType
  override val cacheExpressionResultIfPossible: Boolean
    get() = false
}

class SetInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope,
  override val dependencies: List<InjectableRequest>,
  val singleElementType: TypeRef,
  val collectionElementType: TypeRef
) : Injectable() {
  override val callableFqName: FqName =
    FqName("com.ivianuu.injekt.injectSetOf<${type.arguments[0].render()}>")
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val dependencyScope: InjectablesScope?
    get() = null
  override val originalType: TypeRef
    get() = type.classifier.defaultType
  override val cacheExpressionResultIfPossible: Boolean
    get() = false
}

class ProviderInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope,
  val isInline: Boolean,
  dependencyCallContext: CallContext
) : Injectable() {
  override val callableFqName: FqName = when (type.callContext) {
    CallContext.DEFAULT -> FqName("com.ivianuu.injekt.providerOf")
    CallContext.COMPOSABLE -> FqName("com.ivianuu.injekt.composableProviderOf")
    CallContext.SUSPEND -> FqName("com.ivianuu.injekt.suspendProviderOf")
  }
  override val dependencies: List<InjectableRequest> = listOf(
    InjectableRequest(
      type = type.arguments.last(),
      defaultStrategy = if (type.arguments.last().isNullableType)
        if (type.defaultOnAllErrors)
          InjectableRequest.DefaultStrategy.DEFAULT_ON_ALL_ERRORS
        else InjectableRequest.DefaultStrategy.DEFAULT_IF_NOT_PROVIDED
      else InjectableRequest.DefaultStrategy.NONE,
      callableFqName = callableFqName,
      parameterName = "instance".asNameId(),
      isInline = isInline,
      isLazy = !isInline
    )
  )

  val parameterDescriptors = mutableListOf<ParameterDescriptor>()

  override val dependencyScope = InjectablesScope(
    name = "PROVIDER $type",
    parent = ownerScope,
    context = ownerScope.context,
    callContext = dependencyCallContext,
    ownerDescriptor = ownerScope.ownerDescriptor,
    trace = ownerScope.trace,
    initialInjectables = type
      .toKotlinType(ownerScope.context)
      .memberScope
      .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
      .first()
      .valueParameters
      .asSequence()
      .onEach { parameterDescriptors += it }
      .mapIndexed { index, parameter ->
        parameter
          .toCallableRef(ownerScope.context, ownerScope.trace)
          .copy(isProvide = true, type = type.arguments[index])
      }
      .toList(),
    imports = emptyList(),
    typeParameters = emptyList()
  )
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val originalType: TypeRef
    get() = type.classifier.defaultType
  override val cacheExpressionResultIfPossible: Boolean
    get() = true
}

fun CallableRef.getInjectableRequests(
  context: InjektContext,
  trace: BindingTrace
): List<InjectableRequest> = callable.allParameters
  .asSequence()
  .filter {
    callable !is ClassConstructorDescriptor || it.name.asString() != "<this>"
  }
  .filter {
    it === callable.dispatchReceiverParameter ||
        it === callable.extensionReceiverParameter ||
        it.isProvide(context, trace) ||
        parameterTypes[it.injektName()]!!.isProvide
  }
  .map { parameter ->
    val name = parameter.injektName()
    InjectableRequest(
      type = parameterTypes[name]!!,
      defaultStrategy = if (parameter is ValueParameterDescriptor && parameter.hasDefaultValueIgnoringInject) {
        if (name in defaultOnAllErrorParameters) InjectableRequest.DefaultStrategy.DEFAULT_ON_ALL_ERRORS
        else InjectableRequest.DefaultStrategy.DEFAULT_IF_NOT_PROVIDED
      } else InjectableRequest.DefaultStrategy.NONE,
      callableFqName = parameter.containingDeclaration.fqNameSafe,
      parameterName = name.asNameId(),
      isInline = callable.safeAs<FunctionDescriptor>()?.isInline == true &&
          InlineUtil.isInlineParameter(parameter),
      isLazy = false
    )
  }
  .toList()

data class InjectableRequest(
  val type: TypeRef,
  val defaultStrategy: DefaultStrategy,
  val callableFqName: FqName,
  val parameterName: Name,
  val isInline: Boolean,
  val isLazy: Boolean
) {
  enum class DefaultStrategy {
    NONE, DEFAULT_IF_NOT_PROVIDED, DEFAULT_ON_ALL_ERRORS
  }
}
