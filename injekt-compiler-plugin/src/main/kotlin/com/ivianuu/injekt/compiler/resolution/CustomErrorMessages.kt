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
import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeAsSequence
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class CustomErrorMessages(val notFoundMessage: String?, val ambiguousMessage: String?)

@OptIn(ExperimentalStdlibApi::class)
fun CallableRef.customErrorMessages(@Inject ctx: Context): CustomErrorMessages? {
  val typeParametersForErrorMessages = when (callable) {
    is CallableMemberDescriptor -> typeParameters +
        (callable.containingDeclaration
          .safeAs<ClassDescriptor>()?.toClassifierRef()?.typeParameters ?: emptyList())
    is ParameterDescriptor -> {
      (callable.containingDeclaration.safeAs<CallableMemberDescriptor>()
        ?.toCallableRef()
        ?.typeParameters ?: emptyList()) +
          (callable.containingDeclaration.containingDeclaration
            ?.safeAs<ClassDescriptor>()
            ?.toClassifierRef()
            ?.typeParameters ?: emptyList())
    }
    else -> emptyList()
  }

  return (if (callable is CallableMemberDescriptor &&
    callable.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) null
  else (callable.annotations + (callable.safeAs<ConstructorDescriptor>()
    ?.constructedClass?.annotations?.toList() ?: emptyList()))
    .customErrorMessages(typeParametersForErrorMessages, emptyMap()))
    ?: callable.safeAs<CallableMemberDescriptor>()
      ?.overriddenTreeAsSequence(false)
      ?.drop(1)
      ?.firstOrNull()
      ?.original
      ?.toCallableRef()
      ?.let { superCallable ->
        val superErrorMessages = superCallable.customErrorMessages() ?: return@let null

        val substitutions = buildMap<ClassifierRef, TypeRef> {
          val superDispatchReceiver = superCallable.parameterTypes[DISPATCH_RECEIVER_INDEX]!!

          superCallable.typeParameters.forEachIndexed { index, superTypeParameter ->
            val argument = typeArguments[typeParameters[index]]!!
            this[superTypeParameter] = argument
          }

          superDispatchReceiver
            .buildContext(parameterTypes[DISPATCH_RECEIVER_INDEX]!!
              .subtypeView(superDispatchReceiver.classifier)!!, emptyList())
            .fixedTypeVariables
            .forEach { this[it.key] = it.value }
        }

        superErrorMessages.format(substitutions)
      }
    ?: callable.safeAs<ParameterDescriptor>()
      ?.containingDeclaration
      ?.safeAs<CallableMemberDescriptor>()
      ?.toCallableRef()
      ?.let { callable ->
        callable.callable.cast<CallableMemberDescriptor>()
          .overriddenTreeAsSequence(false)
          .drop(1)
          .firstOrNull()
          ?.original
          ?.toCallableRef()
          ?.let { superCallable ->
            val superErrorMessages = superCallable.callable.cast<CallableDescriptor>()
              .valueParameters
              .first { it.index == this.callable.cast<ValueParameterDescriptor>().index }
              .toCallableRef()
              .customErrorMessages()
              ?: return@let null

            val substitutions = buildMap<ClassifierRef, TypeRef> {
              val superDispatchReceiver = superCallable.parameterTypes[DISPATCH_RECEIVER_INDEX]!!

              superCallable.typeParameters.forEachIndexed { index, superTypeParameter ->
                val argument = callable.typeArguments[callable.typeParameters[index]]!!
                this[superTypeParameter] = argument
              }

              superDispatchReceiver
                .buildContext(callable.parameterTypes[DISPATCH_RECEIVER_INDEX]!!
                  .subtypeView(superDispatchReceiver.classifier)!!, emptyList())
                .fixedTypeVariables
                .forEach { this[it.key] = it.value }
            }

            superErrorMessages.format(substitutions)
          }
      }
}

fun ClassifierRef.customErrorMessages(@Inject ctx: Context): CustomErrorMessages? =
  descriptor?.annotations?.customErrorMessages(typeParameters, emptyMap())
    ?: superTypes.firstNotNullOfOrNull {
      it.classifier.customErrorMessages()
        ?.format(it.classifier.typeParameters.zip(it.arguments).toMap())
    }

fun Iterable<AnnotationDescriptor>.customErrorMessages(
  typeParameters: List<ClassifierRef>,
  substitutionMap: Map<ClassifierRef, TypeRef>,
  @Inject ctx: Context
): CustomErrorMessages? {
  fun AnnotationDescriptor.extractMessage(): String =
    allValueArguments["message".asNameId()]!!
      .value
      .cast<String>()
      .format(typeParameters.toSubstitutions())

  val notFoundMessage = (firstOrNull { it.fqName == injektFqNames().injectableNotFound })
    ?.extractMessage()
    ?.format(substitutionMap)
  val ambiguousMessage = (firstOrNull { it.fqName == injektFqNames().ambiguousInjectable })
    ?.extractMessage()
    ?.format(substitutionMap)

  return if (notFoundMessage == null && ambiguousMessage == null) null
  else CustomErrorMessages(notFoundMessage, ambiguousMessage)
}

fun CustomErrorMessages.format(substitutionMap: Map<ClassifierRef, TypeRef>): CustomErrorMessages =
  format(substitutionMap.toSubstitutions())

fun CustomErrorMessages.format(substitutions: List<Pair<String, String>>): CustomErrorMessages =
  CustomErrorMessages(
    notFoundMessage = notFoundMessage?.format(substitutions),
    ambiguousMessage = ambiguousMessage?.format(substitutions)
  )

private fun String.format(substitutions: List<Pair<String, String>>): String =
  substitutions.toList().fold(this) { acc, nextReplacement ->
    acc.replace(nextReplacement.first, nextReplacement.second)
  }

private fun Map<ClassifierRef, TypeRef>.toSubstitutions() =
  map {
    "[${it.key.fqName.asString()}]" to
        if (it.value.classifier.isTypeParameter) "[${it.value.classifier.fqName}]"
        else it.value.renderToString()
  }

private fun Collection<ClassifierRef>.toSubstitutions() =
  map { "[${it.fqName.shortName()}]" to "[${it.fqName}]" }
