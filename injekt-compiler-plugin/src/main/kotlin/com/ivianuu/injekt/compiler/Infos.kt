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

import com.ivianuu.injekt.compiler.analysis.InjectFunctionDescriptor
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.anyType
import com.ivianuu.injekt.compiler.resolution.firstSuperTypeOrNull
import com.ivianuu.injekt.compiler.resolution.isProvide
import com.ivianuu.injekt.compiler.resolution.isSuspendFunctionType
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.resolution.wrap
import com.ivianuu.injekt_shaded.Inject
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
  val scopeComponentType: TypeRef? = null
)

fun CallableDescriptor.callableInfo(@Inject context: InjektContext): CallableInfo =
  context.trace.getOrPut(InjektWritableSlices.CALLABLE_INFO, this) {
    context.trace!!
    if (isDeserializedDeclaration()) {
      val info = annotations
        .findAnnotation(injektFqNames().callableInfo)
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
            .zip(superType.arguments)
            .toMap() + rootOverriddenCallable
            .typeParameters
            .map { it.toClassifierRef() }
            .zip(typeParameters.map { it.defaultType.toTypeRef() })
          info.copy(
            type = info.type.substitute(substitutionMap),
            parameterTypes = info.parameterTypes.mapValues {
              it.value.substitute(substitutionMap)
            },
            scopeComponentType = info.scopeComponentType?.substitute(substitutionMap)
          )
        }

        return@getOrPut finalInfo
      }
    }

    val type = run {
      val tags = if (this is ConstructorDescriptor)
        getAnnotatedAnnotations(injektFqNames().tag)
          .map { it.type.toTypeRef() }
      else emptyList()
      tags.wrap(returnType!!.toTypeRef())
    }

    val parameterTypes = (if (this is ConstructorDescriptor) valueParameters else allParameters)
      .map { it.injektIndex() to it.type.toTypeRef() }
      .toMap()

    val injectParameters = (if (this is ConstructorDescriptor) valueParameters else allParameters)
      .filter {
        it.hasAnnotation(injektFqNames().inject) ||
            ((this is FunctionInvokeDescriptor ||
                (this is InjectFunctionDescriptor &&
                    underlyingDescriptor is FunctionInvokeDescriptor)) &&
                it.type.hasAnnotation(injektFqNames().inject))
      }
      .mapTo(mutableSetOf()) { it.injektIndex() }

    val scopeComponentType = (returnType!!.annotations.findAnnotation(injektFqNames().scoped)
      ?: safeAs<ConstructorDescriptor>()?.annotations?.findAnnotation(injektFqNames().scoped))
      ?.type?.arguments?.single()?.type?.toTypeRef()

    val info = CallableInfo(
      type = type,
      parameterTypes = parameterTypes,
      injectParameters = injectParameters,
      scopeComponentType = scopeComponentType
    )

    // important to cache the info before persisting it
    context.trace.record(InjektWritableSlices.CALLABLE_INFO, this, info)

    persistInfoIfNeeded(info)

    return info
  }

private fun CallableDescriptor.persistInfoIfNeeded(
  info: CallableInfo,
  @Inject context: InjektContext
) {
  if (isExternalDeclaration() || isDeserializedDeclaration()) return

  if ((this !is ConstructorDescriptor && !visibility.shouldPersistInfo()) ||
    (this is ConstructorDescriptor && !constructedClass.visibility.shouldPersistInfo()))
      return

  if (hasAnnotation(injektFqNames().callableInfo))
    return

  val shouldPersistInfo = hasAnnotation(injektFqNames().provide) ||
      containingDeclaration.hasAnnotation(injektFqNames().component) ||
      containingDeclaration.hasAnnotation(injektFqNames().entryPoint) ||
      (this is ConstructorDescriptor &&
          constructedClass.hasAnnotation(injektFqNames().provide)) ||
      (this is PropertyDescriptor &&
          primaryConstructorPropertyValueParameter()?.isProvide() == true) ||
      safeAs<FunctionDescriptor>()
        ?.valueParameters
        ?.any { it.hasAnnotation(injektFqNames().inject) } == true ||
      info.type.shouldBePersisted() ||
      info.parameterTypes.any { (_, parameterType) ->
        parameterType.shouldBePersisted()
      } ||
      info.scopeComponentType != null

  if (!shouldPersistInfo) return

  val serializedInfo = info.toPersistedCallableInfo().encode()

  updateAnnotation(
    AnnotationDescriptorImpl(
      context.injektContext.module.findClassAcrossModuleDependencies(
        ClassId.topLevel(injektFqNames().callableInfo)
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
  @SerialName("3") val scopeComponentType: PersistedTypeRef? = null
)

fun CallableInfo.toPersistedCallableInfo(@Inject context: InjektContext) = PersistedCallableInfo(
  type = type.toPersistedTypeRef(),
  parameterTypes = parameterTypes
    .mapValues { it.value.toPersistedTypeRef() },
  injectParameters = injectParameters,
  scopeComponentType = scopeComponentType?.toPersistedTypeRef()
)

fun PersistedCallableInfo.toCallableInfo(@Inject context: InjektContext) = CallableInfo(
  type = type.toTypeRef(),
  parameterTypes = parameterTypes
    .mapValues { it.value.toTypeRef() },
  injectParameters = injectParameters,
  scopeComponentType = scopeComponentType?.toTypeRef()
)

/**
 * Stores information about a classifier which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
class ClassifierInfo(
  val tags: List<TypeRef> = emptyList(),
  val scopeComponentType: TypeRef? = null,
  val lazySuperTypes: Lazy<List<TypeRef>> = lazy(LazyThreadSafetyMode.NONE) { emptyList() },
  val primaryConstructorPropertyParameters: List<String> = emptyList(),
  val isSpread: Boolean = false
) {
  val superTypes by lazySuperTypes
}

fun ClassifierDescriptor.classifierInfo(@Inject context: InjektContext): ClassifierInfo =
  context.trace.getOrPut(InjektWritableSlices.CLASSIFIER_INFO, this) {
    context.trace!!
    if (isDeserializedDeclaration()) {
      (if (this is TypeParameterDescriptor) {
        containingDeclaration
          .annotations
          .findAnnotation(injektFqNames().typeParameterInfos)
          ?.readChunkedValue()
          ?.split("=:=")
          ?.get(cast<TypeParameterDescriptor>().index)
          ?.takeIf { it.isNotEmpty() }
          ?.decode<PersistedClassifierInfo>()
          ?.toClassifierInfo()
      } else {
        annotations
          .findAnnotation(injektFqNames().classifierInfo)
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

    val isTag = hasAnnotation(injektFqNames().tag)

    val lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      when {
        expandedType != null -> listOf(expandedType)
        isTag -> listOf(context.injektContext.anyType)
        else -> typeConstructor.supertypes.map { it.toTypeRef() }
      }
    }

    val isDeserialized = isDeserializedDeclaration()

    val tags = getAnnotatedAnnotations(injektFqNames().tag)
      .map { it.type.toTypeRef() }

    val scopeComponentType = annotations.findAnnotation(injektFqNames().scoped)
      ?.type?.arguments?.single()?.type?.toTypeRef()

    val entryPointComponentType = annotations.findAnnotation(injektFqNames().entryPoint)
    ?.type?.arguments?.single()?.type?.toTypeRef()

    val primaryConstructorPropertyParameters = if (isDeserialized) emptyList()
    else safeAs<ClassDescriptor>()
      ?.unsubstitutedPrimaryConstructor
      ?.valueParameters
      ?.filter { it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true }
      ?.map { it.name.asString() }
      ?: emptyList()

    val isSpread = if (isDeserialized) false
    else hasAnnotation(injektFqNames().spread) ||
        findPsi()?.safeAs<KtTypeParameter>()
          ?.hasAnnotation(injektFqNames().spread) == true

    val info = ClassifierInfo(
      tags = tags,
      scopeComponentType = scopeComponentType,
      lazySuperTypes = lazySuperTypes,
      primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
      isSpread = isSpread
    )

    // important to cache the info before persisting it
    context.trace.record(InjektWritableSlices.CLASSIFIER_INFO, this, info)

    persistInfoIfNeeded(info)

    return info
  }

@Serializable data class PersistedClassifierInfo(
  @SerialName("0") val tags: List<PersistedTypeRef> = emptyList(),
  @SerialName("1") val scopeComponentType: PersistedTypeRef? = null,
  @SerialName("2") val entryPointComponentType: PersistedTypeRef? = null,
  @SerialName("3") val superTypes: List<PersistedTypeRef> = emptyList(),
  @SerialName("4") val primaryConstructorPropertyParameters: List<String> = emptyList(),
  @SerialName("5") val isSpread: Boolean = false
)

fun PersistedClassifierInfo.toClassifierInfo(
  @Inject context: InjektContext
): ClassifierInfo = ClassifierInfo(
  tags = tags.map { it.toTypeRef() },
  scopeComponentType = scopeComponentType?.toTypeRef(),
  lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { superTypes.map { it.toTypeRef() } },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread
)

fun ClassifierInfo.toPersistedClassifierInfo(
  @Inject context: InjektContext
): PersistedClassifierInfo = PersistedClassifierInfo(
  tags = tags.map { it.toPersistedTypeRef() },
  scopeComponentType = scopeComponentType?.toPersistedTypeRef(),
  superTypes = superTypes.map { it.toPersistedTypeRef() },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread
)

private fun ClassifierDescriptor.persistInfoIfNeeded(
  info: ClassifierInfo,
  @Inject context: InjektContext
) {
  if (isExternalDeclaration() || isDeserializedDeclaration()) return

  if (this is TypeParameterDescriptor) {
    val container = containingDeclaration
    if (container is TypeAliasDescriptor) return

    if (!info.isSpread && info.superTypes.none { it.shouldBePersisted() }) return

    fun loadTypeParameterInfos() = (container.annotations
      .findAnnotation(injektFqNames().typeParameterInfos)
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
      .findAnnotation(injektFqNames().typeParameterInfos)
    val initialTypeParameterInfos = loadTypeParameterInfos()
    if (initialTypeParameterInfos[index].isEmpty()) {
      val serializedInfo = info.toPersistedClassifierInfo().encode()
      // load again if the annotation has changed
      val finalTypeParameterInfos =
        if (container.annotations.findAnnotation(injektFqNames().typeParameterInfos) !=
          initialInfosAnnotation) loadTypeParameterInfos()
      else initialTypeParameterInfos
      finalTypeParameterInfos[index] = serializedInfo
      container.updateAnnotation(
        AnnotationDescriptorImpl(
          context.injektContext.module.findClassAcrossModuleDependencies(
            ClassId.topLevel(injektFqNames().typeParameterInfos)
          )?.defaultType ?: return,
          finalTypeParameterInfos.joinToString("=:=").toChunkedAnnotationArguments(),
          SourceElement.NO_SOURCE
        )
      )
    }
  } else if (this is DeclarationDescriptorWithVisibility) {
    if (!visibility.shouldPersistInfo()) return
    if (hasAnnotation(injektFqNames().classifierInfo)) return

    if (info.tags.isEmpty() &&
      info.primaryConstructorPropertyParameters.isEmpty() &&
      !hasAnnotation(injektFqNames().provide) &&
      !hasAnnotation(injektFqNames().component) &&
      (this !is ClassDescriptor ||
          constructors.none { it.hasAnnotation(injektFqNames().provide) }) &&
      info.superTypes.none { it.shouldBePersisted() } &&
      info.scopeComponentType == null
    ) return

    val serializedInfo = info.toPersistedClassifierInfo().encode()

    updateAnnotation(
      AnnotationDescriptorImpl(
        context.injektContext.module.findClassAcrossModuleDependencies(
          ClassId.topLevel(injektFqNames().classifierInfo)
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
    else -> throw AssertionError("Cannot add annotation to $this $javaClass")
  }
}

val json = Json { ignoreUnknownKeys = true }
inline fun <reified T> T.encode(): String = json.encodeToString(this)
inline fun <reified T> String.decode(): T = json.decodeFromString(this)

private fun DescriptorVisibility.shouldPersistInfo() = this ==
    DescriptorVisibilities.PUBLIC ||
    this == DescriptorVisibilities.INTERNAL ||
    this == DescriptorVisibilities.PROTECTED
