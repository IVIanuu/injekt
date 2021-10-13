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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.compiler.analysis.AnalysisContext
import com.ivianuu.injekt.compiler.analysis.InjectFunctionDescriptor
import com.ivianuu.injekt.compiler.resolution.STAR_PROJECTION_TYPE
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.anyType
import com.ivianuu.injekt.compiler.resolution.firstSuperTypeOrNull
import com.ivianuu.injekt.compiler.resolution.isProvide
import com.ivianuu.injekt.compiler.resolution.isSuspendFunctionType
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.resolution.wrap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotatedImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.resolve.FunctionImportedFromObject
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Stores information about a callable which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
data class CallableInfo(
  val type: TypeRef,
  val parameterTypes: Map<Int, TypeRef> = emptyMap(),
  val injectParameters: Set<Int> = emptySet(),
  val defaultOnAllErrorsParameters: Set<Int> = emptySet()
) {
  companion object {
    val Empty = CallableInfo(STAR_PROJECTION_TYPE, emptyMap(), emptySet(), emptySet())
  }
}

fun CallableDescriptor.callableInfo(@Inject context: AnalysisContext): CallableInfo =
  context.trace.getOrPut(InjektWritableSlices.CALLABLE_INFO, this) {
    if (isDeserializedDeclaration()) {
      val info = annotations
        .findAnnotation(InjektFqNames.CallableInfo)
        ?.readChunkedValue()
        ?.decode<PersistedCallableInfo>()
        ?.toCallableInfo()

      if (info != null) {
        val finalInfo = if (this !is CallableMemberDescriptor ||
          kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
          info
        } else {
          val rootOverriddenCallable = overriddenTreeUniqueAsSequence(false).last()
          val rootClassifier = rootOverriddenCallable.containingDeclaration
            .cast<ClassDescriptor>()
            .toClassifierRef()
          val classifierInfo = containingDeclaration
            .cast<ClassDescriptor>()
            .toClassifierRef()
          val superType = classifierInfo.defaultType.firstSuperTypeOrNull {
            it.classifier == rootClassifier
          }!!
          val substitutionMap = rootClassifier.typeParameters
            .toMap(superType.arguments) + rootOverriddenCallable
            .typeParameters
            .map { it.toClassifierRef() }
            .toMap(typeParameters.map { it.defaultType.toTypeRef() })
          info.copy(
            type = info.type.substitute(substitutionMap),
            parameterTypes = info.parameterTypes.mapValues {
              it.value.substitute(substitutionMap)
            }
          )
        }

        return@getOrPut finalInfo
      }

      // if this is a deserialized declaration and no info was persisted
      // we can return a dummy object because this callable is not relevant for injekt
      return@getOrPut CallableInfo.Empty
    }

    val type = run {
      val tags = if (this is ConstructorDescriptor)
        getAnnotatedAnnotations(InjektFqNames.Tag)
          .map { it.type.toTypeRef() }
      else emptyList()
      tags.wrap(returnType!!.toTypeRef())
    }

    val parameterTypes = (if (this is ConstructorDescriptor) valueParameters else allParameters)
      .map { it.injektIndex() to it.type.toTypeRef() }
      .toMap()

    val injectParameters = (if (this is ConstructorDescriptor) valueParameters else allParameters)
      .filter {
        it.hasAnnotation(InjektFqNames.Inject) ||
            ((this is FunctionInvokeDescriptor ||
                (this is InjectFunctionDescriptor &&
                    underlyingDescriptor is FunctionInvokeDescriptor)) &&
                it.type.hasAnnotation(InjektFqNames.Inject))
      }
      .mapTo(mutableSetOf()) { it.injektIndex() }

    val defaultOnAllErrorsParameters = valueParameters
      .filter { it.annotations.hasAnnotation(InjektFqNames.DefaultOnAllErrors) }
      .mapTo(mutableSetOf()) { it.injektIndex() }

    val info = CallableInfo(
      type = type,
      parameterTypes = parameterTypes,
      injectParameters = injectParameters,
      defaultOnAllErrorsParameters = defaultOnAllErrorsParameters
    )

    // important to cache the info before persisting it
    context.trace?.record(InjektWritableSlices.CALLABLE_INFO, this, info)

    persistInfoIfNeeded(info)

    return info
  }

private fun CallableDescriptor.persistInfoIfNeeded(
  info: CallableInfo,
  @Inject context: AnalysisContext
) {
  if (isExternalDeclaration() || isDeserializedDeclaration()) return

  if ((this !is ConstructorDescriptor &&
        !visibility.shouldPersistInfo()) ||
    (this is ConstructorDescriptor &&
        !constructedClass.visibility.shouldPersistInfo())
  ) return

  if (hasAnnotation(InjektFqNames.CallableInfo))
    return

  val shouldPersistInfo = hasAnnotation(InjektFqNames.Provide) ||
      (this is ConstructorDescriptor &&
          constructedClass.hasAnnotation(InjektFqNames.Provide)) ||
      (this is PropertyDescriptor &&
          primaryConstructorPropertyValueParameter()?.isProvide() == true) ||
      safeAs<FunctionDescriptor>()
        ?.valueParameters
        ?.any { it.hasAnnotation(InjektFqNames.Inject) } == true ||
      info.type.shouldBePersisted() ||
      info.parameterTypes.any { (_, parameterType) ->
        parameterType.shouldBePersisted()
      }

  if (!shouldPersistInfo) return

  val serializedInfo = info.toPersistedCallableInfo().encode()

  updateAnnotation(
    AnnotationDescriptorImpl(
      context.injektContext.module.findClassAcrossModuleDependencies(
        ClassId.topLevel(
          InjektFqNames.CallableInfo
        )
      )?.defaultType ?: return,
      serializedInfo.toChunkedAnnotationArguments(),
      SourceElement.NO_SOURCE
    )
  )
}

@Serializable data class PersistedCallableInfo(
  @SerialName("0") val type: PersistedTypeRef,
  @SerialName("1") val parameterTypes: Map<Int, PersistedTypeRef> = emptyMap(),
  @SerialName("2") val injectParameters: Set<Int> = emptySet(),
  @SerialName("3") val defaultOnAllErrorsParameters: Set<Int> = emptySet()
)

fun CallableInfo.toPersistedCallableInfo(@Inject context: AnalysisContext) = PersistedCallableInfo(
  type = type.toPersistedTypeRef(),
  parameterTypes = parameterTypes
    .mapValues { it.value.toPersistedTypeRef() },
  injectParameters = injectParameters,
  defaultOnAllErrorsParameters = defaultOnAllErrorsParameters
)

fun PersistedCallableInfo.toCallableInfo(@Inject context: AnalysisContext) = CallableInfo(
  type = type.toTypeRef(),
  parameterTypes = parameterTypes
    .mapValues { it.value.toTypeRef() },
  injectParameters = injectParameters,
  defaultOnAllErrorsParameters = defaultOnAllErrorsParameters
)

/**
 * Stores information about a classifier which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
class ClassifierInfo(
  val tags: List<TypeRef> = emptyList(),
  val lazySuperTypes: Lazy<List<TypeRef>> = lazy { emptyList() },
  val primaryConstructorPropertyParameters: List<String> = emptyList(),
  val isSpread: Boolean = false
) {
  val superTypes by lazySuperTypes
}

fun ClassifierDescriptor.classifierInfo(@Inject context: AnalysisContext): ClassifierInfo =
  context.trace.getOrPut(InjektWritableSlices.CLASSIFIER_INFO, this) {
    if (isDeserializedDeclaration()) {
      (if (this is TypeParameterDescriptor) {
        containingDeclaration
          .annotations
          .findAnnotation(InjektFqNames.TypeParameterInfos)
          ?.readChunkedValue()
          ?.split("=:=")
          ?.get(cast<TypeParameterDescriptor>().index)
          ?.takeIf { it.isNotEmpty() }
          ?.decode<PersistedClassifierInfo>()
          ?.toClassifierInfo()
      } else {
        annotations
          .findAnnotation(InjektFqNames.ClassifierInfo)
          ?.readChunkedValue()
          ?.cast<String>()
          ?.decode<PersistedClassifierInfo>()
          ?.toClassifierInfo()
      })?.let {
        return@getOrPut it
      }
    }

    val expandedType = (original as? TypeAliasDescriptor)?.underlyingType
      ?.toTypeRef()

    val isTag = hasAnnotation(InjektFqNames.Tag)

    val lazySuperTypes = lazy {
      when {
        expandedType != null -> listOf(expandedType)
        isTag -> listOf(context.injektContext.anyType)
        else -> typeConstructor.supertypes.map { it.toTypeRef() }
      }
    }

    val isDeserialized = isDeserializedDeclaration()

    val tags = getAnnotatedAnnotations(InjektFqNames.Tag)
      .map { it.type.toTypeRef() }

    val primaryConstructorPropertyParameters = if (isDeserialized) emptyList()
    else safeAs<ClassDescriptor>()
      ?.unsubstitutedPrimaryConstructor
      ?.valueParameters
      ?.filter { it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true }
      ?.map { it.name.asString() }
      ?: emptyList()

    val isSpread = if (isDeserialized) false
    else hasAnnotation(InjektFqNames.Spread) ||
        findPsi()?.safeAs<KtTypeParameter>()?.hasAnnotation(InjektFqNames.Spread) == true

    val info = ClassifierInfo(
      tags = tags,
      lazySuperTypes = lazySuperTypes,
      primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
      isSpread = isSpread
    )

    // important to cache the info before persisting it
    context.trace?.record(InjektWritableSlices.CLASSIFIER_INFO, this, info)

    persistInfoIfNeeded(info)

    return info
  }

@Serializable data class PersistedClassifierInfo(
  @SerialName("0") val tags: List<PersistedTypeRef> = emptyList(),
  @SerialName("1") val superTypes: List<PersistedTypeRef> = emptyList(),
  @SerialName("2") val primaryConstructorPropertyParameters: List<String> = emptyList(),
  @SerialName("3") val isSpread: Boolean = false
)

fun PersistedClassifierInfo.toClassifierInfo(
  @Inject context: AnalysisContext
): ClassifierInfo = ClassifierInfo(
  tags = tags.map { it.toTypeRef() },
  lazySuperTypes = lazy { superTypes.map { it.toTypeRef() } },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread
)

fun ClassifierInfo.toPersistedClassifierInfo(
  @Inject context: AnalysisContext
): PersistedClassifierInfo = PersistedClassifierInfo(
  tags = tags.map { it.toPersistedTypeRef() },
  superTypes = superTypes.map { it.toPersistedTypeRef() },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread
)

private fun ClassifierDescriptor.persistInfoIfNeeded(
  info: ClassifierInfo,
  @Inject context: AnalysisContext
) {
  if (isExternalDeclaration() || isDeserializedDeclaration()) return

  if (this is TypeParameterDescriptor) {
    val container = containingDeclaration
    if (container is TypeAliasDescriptor) return

    if (!info.isSpread && info.superTypes.none { it.shouldBePersisted() }) return

    fun loadTypeParameterInfos() = (container.annotations
      .findAnnotation(InjektFqNames.TypeParameterInfos)
      ?.readChunkedValue()
      ?.split("=:=")
      ?: run {
        when (container) {
          is CallableDescriptor -> container.typeParameters
          is ClassifierDescriptorWithTypeParameters -> container.declaredTypeParameters
          else -> throw AssertionError()
        }.map { "" }
      }).toMutableList()

    val initialInfosAnnotation = container.annotations
      .findAnnotation(InjektFqNames.TypeParameterInfos)
    val initialTypeParameterInfos = loadTypeParameterInfos()
    if (initialTypeParameterInfos[index].isEmpty()) {
      val serializedInfo = info.toPersistedClassifierInfo().encode()
      // load again if the annotation has changed
      val finalTypeParameterInfos =
        if (container.annotations.findAnnotation(InjektFqNames.TypeParameterInfos) !=
          initialInfosAnnotation) loadTypeParameterInfos()
      else initialTypeParameterInfos
      finalTypeParameterInfos[index] = serializedInfo
      container.updateAnnotation(
        AnnotationDescriptorImpl(
          context.injektContext.module.findClassAcrossModuleDependencies(
            ClassId.topLevel(InjektFqNames.TypeParameterInfos)
          )?.defaultType ?: return,
          finalTypeParameterInfos.joinToString("=:=").toChunkedAnnotationArguments(),
          SourceElement.NO_SOURCE
        )
      )
    }
  } else if (this is DeclarationDescriptorWithVisibility) {
    if (!visibility.shouldPersistInfo()) return
    if (hasAnnotation(InjektFqNames.ClassifierInfo)) return

    if (info.tags.isEmpty() &&
      info.primaryConstructorPropertyParameters.isEmpty() &&
      !hasAnnotation(InjektFqNames.Provide) &&
      (this !is ClassDescriptor ||
          constructors.none { it.hasAnnotation(InjektFqNames.Provide) }) &&
      info.superTypes.none { it.shouldBePersisted() }
    ) return

    val serializedInfo = info.toPersistedClassifierInfo().encode()

    updateAnnotation(
      AnnotationDescriptorImpl(
        context.injektContext.module.findClassAcrossModuleDependencies(
          ClassId.topLevel(
            InjektFqNames.ClassifierInfo
          )
        )?.defaultType ?: return,
        serializedInfo.toChunkedAnnotationArguments(),
        SourceElement.NO_SOURCE
      )
    )
  }
}

private fun AnnotationDescriptor.readChunkedValue() = allValueArguments
  .toList()
  .sortedBy {
    it.first.asString()
      .removePrefix("value")
      .toInt()
  }
  .joinToString(separator = "") { it.second.value as String }

private fun String.toChunkedAnnotationArguments() = chunked(65535 / 2)
  .mapIndexed { index, chunk -> "value$index".asNameId() to StringValue(chunk) }
  .toMap()

private fun TypeRef.shouldBePersisted() = anyType {
  (it.classifier.isTag && it.classifier.typeParameters.size > 1) ||
      (it.classifier.isTypeAlias && it.isSuspendFunctionType)
}

private fun Annotated.updateAnnotation(annotation: AnnotationDescriptor) {
  val newAnnotations = Annotations.create(
    annotations
      .filter { it.type != annotation.type } + annotation
  )
  when (this) {
    is AnnotatedImpl -> updatePrivateFinalField<Annotations>(
      AnnotatedImpl::class,
      "annotations"
    ) { newAnnotations }
    is LazyClassDescriptor -> updatePrivateFinalField<Annotations>(
      LazyClassDescriptor::class,
      "annotations"
    ) { newAnnotations }
    is InjectFunctionDescriptor -> underlyingDescriptor.updateAnnotation(annotation)
    is FunctionImportedFromObject -> callableFromObject.updateAnnotation(annotation)
    else -> {
      //throw AssertionError("Cannot add annotation to $this $javaClass")
    }
  }
}

val json = Json { ignoreUnknownKeys = true }
inline fun <reified T> T.encode(): String = json.encodeToString(this)
inline fun <reified T> String.decode(): T = json.decodeFromString(this)

private fun DescriptorVisibility.shouldPersistInfo() = this ==
    DescriptorVisibilities.PUBLIC ||
    this == DescriptorVisibilities.INTERNAL ||
    this == DescriptorVisibilities.PROTECTED
