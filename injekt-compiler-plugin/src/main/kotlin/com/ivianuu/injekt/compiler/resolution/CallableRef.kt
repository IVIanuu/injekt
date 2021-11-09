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

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.analysis.InjectNParameterDescriptor
import com.ivianuu.injekt.compiler.analysis.substitute
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class CallableRef(
  val callable: CallableDescriptor,
  val type: TypeRef,
  val originalType: TypeRef,
  val typeParameters: List<ClassifierRef>,
  val parameterTypes: Map<Int, TypeRef>,
  val scopeComponentType: TypeRef?,
  val isEager: Boolean,
  val typeArguments: Map<ClassifierRef, TypeRef>,
  val import: ResolvedProviderImport?,
  val injectNParameters: List<InjectNParameterDescriptor>,
  val customErrorMessages: CustomErrorMessages?
)

fun CallableRef.substitute(
  map: Map<ClassifierRef, TypeRef>,
  @Inject ctx: Context
): CallableRef {
  if (map.isEmpty()) return this
  val substitutedTypeParameters = typeParameters.substitute(map)
  val typeParameterSubstitutionMap = substitutedTypeParameters.associateWith {
    it.defaultType
  }
  return copy(
    type = type.substitute(map).substitute(typeParameterSubstitutionMap),
    parameterTypes = parameterTypes
      .mapValues {
        it.value
          .substitute(map)
          .substitute(typeParameterSubstitutionMap)
      },
    typeParameters = substitutedTypeParameters,
    typeArguments = typeArguments
      .mapValues {
        it.value
          .substitute(map)
          .substitute(typeParameterSubstitutionMap)
      },
    scopeComponentType = scopeComponentType?.substitute(map),
    injectNParameters = injectNParameters.map { it.substitute(map) }
  )
}

fun CallableDescriptor.toCallableRef(@Inject ctx: Context): CallableRef =
  trace()!!.getOrPut(InjektWritableSlices.CALLABLE_REF, this) {
    val info = callableInfo()
    val typeParameters = typeParameters.map { it.toClassifierRef() }

    val typeParametersForErrorMessages = typeParameters
      .takeIf { it.isNotEmpty() }
      ?: containingDeclaration
        .safeAs<CallableDescriptor>()
        ?.typeParameters
        ?.map { it.toClassifierRef() }
      ?: emptyList()

    val customErrorMessages = (annotations + (safeAs<ConstructorDescriptor>()
      ?.constructedClass?.annotations?.toList() ?: emptyList()))
      .customErrorMessages(typeParametersForErrorMessages)

    val parameterTypes = if (typeParametersForErrorMessages.isNotEmpty())
      info.parameterTypes
        .mapValues { it.value.formatCustomErrorMessages(typeParametersForErrorMessages) }
      else info.parameterTypes

    CallableRef(
      callable = this,
      type = if (this is InjectNParameterDescriptor) typeRef else info.type,
      originalType = info.type,
      typeParameters = typeParameters,
      parameterTypes = parameterTypes,
      scopeComponentType = info.scopeComponentType,
      isEager = info.isEager,
      typeArguments = typeParameters
        .map { it to it.defaultType }
        .toMap(),
      import = null,
      injectNParameters = info.injectNParameters,
      customErrorMessages = customErrorMessages
    )
  }
