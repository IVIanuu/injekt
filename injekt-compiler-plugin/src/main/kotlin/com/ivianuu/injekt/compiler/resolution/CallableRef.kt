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
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance

data class CallableRef(
  val callable: CallableDescriptor,
  val type: KotlinType,
  val originalType: KotlinType,
  val typeParameters: List<TypeParameterDescriptor>,
  val parameterTypes: Map<Int, KotlinType>,
  val scopeComponentType: KotlinType?,
  val isEager: Boolean,
  val typeArguments: Map<TypeParameterDescriptor, KotlinType>,
  val isProvide: Boolean,
  val import: ResolvedProviderImport?,
  val injectNParameters: List<InjectNParameterDescriptor>
)

fun CallableRef.substitute(
  substitutor: TypeSubstitutor,
  @Inject ctx: Context
): CallableRef {
  if (substitutor.isEmpty) return this
  return copy(
    type = substitutor.safeSubstitute(type, Variance.INVARIANT),
    parameterTypes = parameterTypes
      .mapValues { substitutor.safeSubstitute(it.value, Variance.INVARIANT) },
    typeArguments = typeArguments
      .mapValues { substitutor.safeSubstitute(it.value, Variance.INVARIANT)
                 },
    scopeComponentType = scopeComponentType?.let { substitutor.safeSubstitute(it, Variance.INVARIANT) },
    injectNParameters = injectNParameters.map { it.substitute(substitutor) }
  )
}

fun CallableRef.makeProvide(): CallableRef = if (isProvide) this else copy(isProvide = true)

fun CallableDescriptor.toCallableRef(@Inject ctx: Context): CallableRef =
  trace()!!.getOrPut(InjektWritableSlices.CALLABLE_REF, this) {
    val info = callableInfo()
    CallableRef(
      callable = this,
      type = info.type,
      originalType = info.type,
      typeParameters = typeParameters,
      parameterTypes = info.parameterTypes,
      scopeComponentType = info.scopeComponentType,
      isEager = info.isEager,
      typeArguments = typeParameters
        .map { it to it.defaultType }
        .toMap(),
      isProvide = isProvide(),
      import = null,
      injectNParameters = info.injectNParameters
    )
  }
