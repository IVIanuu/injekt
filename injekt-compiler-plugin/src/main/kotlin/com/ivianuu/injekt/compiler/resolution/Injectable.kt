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

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.compiler.analysis.AnalysisContext
import com.ivianuu.injekt.compiler.analysis.hasDefaultValueIgnoringInject
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.injektName
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class Injectable {
  abstract val type: KotlinType
  abstract val originalType: KotlinType
  abstract val dependencies: List<InjectableRequest>
  abstract val dependencyScope: InjectablesScope?
  abstract val callableFqName: FqName
  abstract val callContext: CallContext
  abstract val ownerScope: InjectablesScope
  abstract val cacheExpressionResultIfPossible: Boolean
}

class CallableInjectable(
  override val type: KotlinType,
  override val dependencies: List<InjectableRequest>,
  override val ownerScope: InjectablesScope,
  val callable: CallableDescriptor,
) : Injectable() {
  override val callableFqName: FqName = if (callable is ClassConstructorDescriptor)
    callable.constructedClass.fqNameSafe
  else callable.fqNameSafe
  override val callContext: CallContext
    get() = callable.callContext()
  override val dependencyScope: InjectablesScope?
    get() = null
  override val originalType: KotlinType
    get() = callable.original.returnType!!
  override val cacheExpressionResultIfPossible: Boolean
    get() = false
}

class SetInjectable(
  override val type: KotlinType,
  override val ownerScope: InjectablesScope,
  override val dependencies: List<InjectableRequest>,
  val singleElementType: KotlinType,
  val collectionElementType: KotlinType
) : Injectable() {
  override val callableFqName: FqName =
    FqName("com.ivianuu.injekt.injectSetOf<${type.arguments[0].renderToString()}>")
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val dependencyScope: InjectablesScope?
    get() = null
  override val originalType: KotlinType
    get() = type.constructor.declarationDescriptor!!.defaultType
  override val cacheExpressionResultIfPossible: Boolean
    get() = false
}

class ProviderInjectable(
  override val type: KotlinType,
  @Provide override val ownerScope: InjectablesScope,
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
      type = type.arguments.last().type,
      defaultStrategy = if (type.arguments.last().type.isMarkedNullable)
        if (type.defaultOnAllErrors)
          InjectableRequest.DefaultStrategy.DEFAULT_ON_ALL_ERRORS
        else InjectableRequest.DefaultStrategy.DEFAULT_IF_NOT_PROVIDED
      else InjectableRequest.DefaultStrategy.NONE,
      callableFqName = callableFqName,
      parameterName = "instance".asNameId(),
      parameterIndex = 0,
      parameterDescriptor = null,
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
    file = null,
    initialInjectables = type
      .memberScope
      .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
      .first()
      .valueParameters
      .onEach { parameterDescriptors += it },
    imports = emptyList(),
    typeParameters = emptyList(),
    nesting = ownerScope.nesting + 1
  )
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val originalType: KotlinType
    get() = type.constructor.declarationDescriptor!!.defaultType
  override val cacheExpressionResultIfPossible: Boolean
    get() = true
}

class SourceKeyInjectable(
  override val type: KotlinType,
  override val ownerScope: InjectablesScope
) : Injectable() {
  override val callableFqName: FqName = FqName("com.ivianuu.injekt.common.sourceKey")
  override val dependencies: List<InjectableRequest> = emptyList()
  override val dependencyScope: InjectablesScope
    get() = ownerScope
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val originalType: KotlinType
    get() = type
  override val cacheExpressionResultIfPossible: Boolean
    get() = false
}

class TypeKeyInjectable(
  override val type: KotlinType,
  override val ownerScope: InjectablesScope
) : Injectable() {
  override val callableFqName: FqName = FqName("com.ivianuu.injekt.common.typeKeyOf<${type.renderToString()}>")
  override val dependencies: List<InjectableRequest> = run {
    val typeParameterDependencies = mutableSetOf<ClassifierDescriptor>()
    type.allTypes.forEach {
      if (it.isTypeParameter())
        typeParameterDependencies += it.constructor.declarationDescriptor!!
    }
    typeParameterDependencies
      .mapIndexed { index, typeParameter ->
        InjectableRequest(
          type = ownerScope.context.injektContext.typeKey.defaultType
            .replace(listOf(typeParameter.defaultType.asTypeProjection())),
          defaultStrategy = InjectableRequest.DefaultStrategy.NONE,
          callableFqName = callableFqName,
          parameterName = "${typeParameter.name}Key".asNameId(),
          parameterIndex = index,
          parameterDescriptor = null,
          isInline = false,
          isLazy = false
        )
      }
  }
  override val dependencyScope: InjectablesScope
    get() = ownerScope
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val originalType: KotlinType
    get() = type
  override val cacheExpressionResultIfPossible: Boolean
    get() = false
}

fun CallableDescriptor.getInjectableRequests(
  @Inject context: AnalysisContext
): List<InjectableRequest> = allParameters
  .filter { this !is ClassConstructorDescriptor || it.name.asString() != "<this>" }
  .filter {
    it === dispatchReceiverParameter ||
        it === extensionReceiverParameter ||
        it.isProvide() ||
        it.type.isProvide
  }
  .map { it.toInjectableRequest(this) }
  .toList()

data class InjectableRequest(
  val type: KotlinType,
  val defaultStrategy: DefaultStrategy,
  val callableFqName: FqName,
  val parameterName: Name,
  val parameterIndex: Int,
  val parameterDescriptor: ParameterDescriptor?,
  val isInline: Boolean,
  val isLazy: Boolean
) {
  enum class DefaultStrategy {
    NONE, DEFAULT_IF_NOT_PROVIDED, DEFAULT_ON_ALL_ERRORS
  }
}

fun ParameterDescriptor.toInjectableRequest(
  callable: CallableDescriptor,
  @Inject context: AnalysisContext
): InjectableRequest {
  val index = injektIndex()
  return InjectableRequest(
    type = type,
    defaultStrategy = if (this is ValueParameterDescriptor && hasDefaultValueIgnoringInject) {
      if (index in callable.callableInfo().defaultOnAllErrorsParameters)
        InjectableRequest.DefaultStrategy.DEFAULT_ON_ALL_ERRORS
      else InjectableRequest.DefaultStrategy.DEFAULT_IF_NOT_PROVIDED
    } else InjectableRequest.DefaultStrategy.NONE,
    callableFqName = containingDeclaration.fqNameSafe,
    parameterName = injektName(),
    parameterIndex = injektIndex(),
    parameterDescriptor = this,
    isInline = callable.safeAs<FunctionDescriptor>()?.isInline == true &&
        InlineUtil.isInlineParameter(this),
    isLazy = false
  )
}
