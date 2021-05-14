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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.*

data class CallableRef(
  val callable: CallableDescriptor,
  val type: TypeRef,
  val originalType: TypeRef,
  val typeParameters: List<ClassifierRef>,
  val parameterTypes: Map<String, TypeRef>,
  val givenParameters: Set<String>,
  val defaultOnAllErrorParameters: Set<String>,
  val typeArguments: Map<ClassifierRef, TypeRef>,
  val isGiven: Boolean,
  val source: CallableRef?,
  val callContext: CallContext,
  val owner: ClassifierRef?,
  val overriddenDepth: Int,
  val doNotIncludeChildren: Boolean,
  val import: GivenImport?
)

fun CallableRef.substitute(map: Map<ClassifierRef, TypeRef>): CallableRef {
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
      }
  )
}

fun CallableRef.makeGiven(): CallableRef = if (isGiven) this else copy(isGiven = true)

fun CallableDescriptor.toCallableRef(
  context: InjektContext,
  trace: BindingTrace
): CallableRef {
  trace.get(InjektWritableSlices.CALLABLE_REF_FOR_DESCRIPTOR, this)?.let { return it }
  val info = callableInfo(context, trace)
  val typeParameters = typeParameters.map { it.toClassifierRef(context, trace) }
  return CallableRef(
    callable = this,
    type = info.type,
    originalType = info.type,
    typeParameters = typeParameters,
    parameterTypes = info.parameterTypes,
    givenParameters = info.givenParameters,
    defaultOnAllErrorParameters = info.defaultOnAllErrorsParameters,
    typeArguments = typeParameters
      .map { it to it.defaultType }
      .toMap(),
    isGiven = isGiven(context, trace),
    source = null,
    callContext = callContext(trace.bindingContext),
    owner = null,
    overriddenDepth = 0,
    doNotIncludeChildren = false,
    import = null
  ).also {
    trace.record(InjektWritableSlices.CALLABLE_REF_FOR_DESCRIPTOR, this, it)
  }
}
