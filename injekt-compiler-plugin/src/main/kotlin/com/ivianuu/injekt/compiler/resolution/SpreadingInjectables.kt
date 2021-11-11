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
import com.ivianuu.injekt.compiler.PersistedTypeRef
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.callableForUniqueKey
import com.ivianuu.injekt.compiler.decode
import com.ivianuu.injekt.compiler.encode
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.injectablesLookupName
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.isDeserializedDeclaration
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.module
import com.ivianuu.injekt.compiler.readChunkedValue
import com.ivianuu.injekt.compiler.shouldPersistInfo
import com.ivianuu.injekt.compiler.toChunkedArrayValue
import com.ivianuu.injekt.compiler.toPersistedTypeRef
import com.ivianuu.injekt.compiler.toTypeRef
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.injekt.compiler.updateAnnotation
import com.ivianuu.shaded_injekt.Inject
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.cast

fun CallableDescriptor.spreadingInjectables(
  @Inject ctx: Context
): List<CallableRef> = trace()!!.getOrPut(InjektWritableSlices.SPREADING_INJECTABLES, this) {
  if (isDeserializedDeclaration()) {
    return@getOrPut annotations
      .findAnnotation(injektFqNames().spreadingInfo)
      ?.readChunkedValue()
      ?.decode<List<PersistedSpreadingInjectable>>()
      ?.map { spreadingInjectable ->
        callableForUniqueKey(FqName(spreadingInjectable.fqName), spreadingInjectable.uniqueKey)!!
          .toCallableRef()
          .let {
            it.substitute(
              it.typeParameters.zip(
                spreadingInjectable.substitutions
                  .map { it.toTypeRef() }
              )
                .toMap()
            )
          }
      } ?: emptyList()
  }

  val callable = toCallableRef()
  if (callable.typeParameters.any { it.isSpread })
    return@getOrPut emptyList()

  val results = callable.spreadInjectables()

  if (visibility.shouldPersistInfo()) {
    val serializedInfo = results
      .map {
        PersistedSpreadingInjectable(
          it.callable.fqNameSafe.asString(),
          it.callable.uniqueKey(),
          it.typeArguments.values.map { it.toPersistedTypeRef() }
        )
      }
      .encode()

    updateAnnotation(
      AnnotationDescriptorImpl(
        module().findClassAcrossModuleDependencies(
          ClassId.topLevel(injektFqNames().spreadingInfo)
        )?.defaultType ?: return@getOrPut emptyList(),
        mapOf("values".asNameId() to serializedInfo.toChunkedArrayValue()),
        SourceElement.NO_SOURCE
      )
    )
  }

  results
}

private fun CallableRef.spreadInjectables(
  @Inject ctx: Context
): List<CallableRef> {
  val lookedUpPackages = mutableSetOf<FqName>()
  val spreadingInjectables = type.collectTypeScopeInjectables()
    .let {
      lookedUpPackages += it.lookedUpPackages

      it.injectables
        .filter { it.typeParameters.any { it.isSpread } }
    }

  val lookupLocation = KotlinLookupLocation(callable.findPsi()!!.cast())

  fun recordLookups() {
    lookedUpPackages
      .map { memberScopeForFqName(it, lookupLocation)!! }
      .forEach { it.recordLookup(injectablesLookupName, lookupLocation) }
  }

  if (spreadingInjectables.isEmpty()) {
    recordLookups()
    return emptyList()
  }

  val result = mutableListOf<CallableRef>()

  fun spreadInjectablesForCandidate(candidate: CallableRef) {
    val (context, substitutionMap) = buildContextForSpreadingInjectable(
      spreadingInjectable.constraintType,
      candidateType,
      allStaticTypeParameters
    )
    if (!context.isOk) return

    val newInjectableType = spreadingInjectable.callable.type
      .substitute(substitutionMap)
      .copy(frameworkKey = 0)
    val newInjectable = spreadingInjectable.callable
      .copy(
        type = newInjectableType,
        originalType = newInjectableType,
        parameterTypes = spreadingInjectable.callable.parameterTypes
          .mapValues { it.value.substitute(substitutionMap) },
        typeArguments = spreadingInjectable.callable
          .typeArguments
          .mapValues { it.value.substitute(substitutionMap) }
      )
  }

  spreadInjectablesForCandidate(this)

  recordLookups()

  return result
}

private data class SpreadingInjectable(
  val callable: CallableRef,
  val constraintType: TypeRef = callable.typeParameters.single {
    it.isSpread
  }.defaultType.substitute(callable.typeArguments),
  val processedCandidateTypes: MutableSet<TypeRef> = mutableSetOf(),
  val resultingFrameworkKeys: MutableSet<Int> = mutableSetOf()
)

@Serializable private data class PersistedSpreadingInjectable(
  val fqName: String,
  val uniqueKey: String,
  val substitutions: List<PersistedTypeRef>
)
