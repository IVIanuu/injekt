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
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.shaded_injekt.Inject
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.cast

@Serializable
data class CustomErrorMessages(val notFoundMessage: String?, val ambiguousMessage: String?)

fun Iterable<AnnotationDescriptor>.customErrorMessages(
  typeParameters: List<ClassifierRef>,
  @Inject ctx: Context
): CustomErrorMessages? {
  val substitutions = typeParameters.toSubstitutions()

  fun AnnotationDescriptor.extractMessage(): String =
    allValueArguments["message".asNameId()]!!
      .value
      .cast<String>()
      .format(substitutions)

  val notFoundMessage = (firstOrNull { it.fqName == injektFqNames().injectableNotFound })
    ?.extractMessage()
  val ambiguousMessage = (firstOrNull { it.fqName == injektFqNames().ambiguousInjectable })
    ?.extractMessage()

  return if (notFoundMessage == null && ambiguousMessage == null) null
  else CustomErrorMessages(notFoundMessage, ambiguousMessage)
}

fun CustomErrorMessages.format(substitutionMap: Map<ClassifierRef, TypeRef>): CustomErrorMessages =
  format(substitutionMap.map { it.key.fqName.asString() to it.value.renderToString() })

fun CustomErrorMessages.format(substitutions: List<Pair<String, String>>): CustomErrorMessages =
  CustomErrorMessages(
    notFoundMessage = notFoundMessage?.format(substitutions),
    ambiguousMessage = ambiguousMessage?.format(substitutions)
  )

private fun String.format(substitutions: List<Pair<String, String>>): String =
  substitutions.toList().fold(this) { acc, nextReplacement ->
    acc.replace(nextReplacement.first, nextReplacement.second)
  }

private fun List<ClassifierRef>.toSubstitutions() =
  map { "[${it.fqName.shortName()}]" to "[${it.fqName}]" }
