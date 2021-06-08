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

import com.ivianuu.injekt.*
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.resolution.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.builtins.functions.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.*
import org.jetbrains.kotlin.utils.addToStdlib.*

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

fun CallableDescriptor.callableInfo(@Inject context: AnalysisContext): CallableInfo {
  context.injektContext.callableInfos[this]?.let { return it }

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

      context.injektContext.callableInfos[this] = finalInfo
      return finalInfo
    }

    // if this is a deserialized declaration and no info was persisted
    // we can return a dummy object because this callable is not relevant for injekt
    context.injektContext.callableInfos[this] = CallableInfo.Empty
    return CallableInfo.Empty
  }

  val type = run {
    val qualifiers = if (this is ConstructorDescriptor)
      getAnnotatedAnnotations(InjektFqNames.Qualifier)
        .map { it.type.toTypeRef() }
    else emptyList()
    qualifiers.wrap(returnType!!.toTypeRef())
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
    .asSequence()
    .filter { it.annotations.hasAnnotation(InjektFqNames.DefaultOnAllErrors) }
    .mapTo(mutableSetOf()) { it.injektIndex() }

  val info = CallableInfo(
    type = type,
    parameterTypes = parameterTypes,
    injectParameters = injectParameters,
    defaultOnAllErrorsParameters = defaultOnAllErrorsParameters
  )

  // import to cache the info before persisting it
  context.injektContext.callableInfos[this] = info

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
  val qualifiers: List<TypeRef> = emptyList(),
  val lazySuperTypes: Lazy<List<TypeRef>> = lazy { emptyList() },
  val primaryConstructorPropertyParameters: List<String> = emptyList(),
  val isSpread: Boolean = false,
  val isSingletonInjectable: Boolean = false
) {
  val superTypes by lazySuperTypes
}

fun ClassifierDescriptor.classifierInfo(@Inject context: AnalysisContext): ClassifierInfo {
  context.injektContext.classifierInfos[this]?.let { return it }

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
      // import to cache the info before persisting it
      context.injektContext.classifierInfos[this] = it
      return it
    }
  }

  val expandedType = (original as? TypeAliasDescriptor)?.underlyingType
    ?.toTypeRef()

  val isQualifier = hasAnnotation(InjektFqNames.Qualifier)

  val lazySuperTypes = lazy {
    when {
      expandedType != null -> listOf(expandedType)
      isQualifier -> listOf(context.injektContext.anyType)
      else -> typeConstructor.supertypes.map { it.toTypeRef() }
    }
  }

  val isDeserialized = isDeserializedDeclaration()

  val qualifiers = getAnnotatedAnnotations(InjektFqNames.Qualifier)
    .map { it.type.toTypeRef() }

  val primaryConstructorPropertyParameters = if (isDeserialized) emptyList()
  else safeAs<ClassDescriptor>()
    ?.unsubstitutedPrimaryConstructor
    ?.valueParameters
    ?.asSequence()
    ?.filter { it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true }
    ?.map { it.name.asString() }
    ?.toList()
    ?: emptyList()

  val isSpread = if (isDeserialized) false
  else hasAnnotation(InjektFqNames.Spread) ||
      findPsi()?.safeAs<KtTypeParameter>()?.hasAnnotation(InjektFqNames.Spread) == true

  val isSingletonInjectable = !isDeserialized &&
      this is ClassDescriptor &&
      kind == ClassKind.CLASS &&
      constructors
        .filter {
          it.hasAnnotation(InjektFqNames.Provide) ||
              (it.isPrimary && hasAnnotation(InjektFqNames.Provide))
        }
        .any { it.valueParameters.isEmpty() } &&
      unsubstitutedMemberScope.getContributedDescriptors()
        .none {
          (it is ClassDescriptor &&
              it.isInner) ||
              (it is PropertyDescriptor &&
                  it.hasBackingField(context.trace?.bindingContext))
        }

  val info = ClassifierInfo(
    qualifiers = qualifiers,
    lazySuperTypes = lazySuperTypes,
    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
    isSpread = isSpread,
    isSingletonInjectable = isSingletonInjectable
  )

  // import to cache the info before persisting it
  context.injektContext.classifierInfos[this] = info

  persistInfoIfNeeded(info)

  return info
}

@Serializable data class PersistedClassifierInfo(
  @SerialName("0") val qualifiers: List<PersistedTypeRef> = emptyList(),
  @SerialName("1") val superTypes: List<PersistedTypeRef> = emptyList(),
  @SerialName("2") val primaryConstructorPropertyParameters: List<String> = emptyList(),
  @SerialName("3") val isSpread: Boolean = false,
  @SerialName("5") val isSingletonInjectable: Boolean = false
)

fun PersistedClassifierInfo.toClassifierInfo(
  @Inject context: AnalysisContext
): ClassifierInfo = ClassifierInfo(
  qualifiers = qualifiers.map { it.toTypeRef() },
  lazySuperTypes = lazy { superTypes.map { it.toTypeRef() } },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread,
  isSingletonInjectable = isSingletonInjectable
)

fun ClassifierInfo.toPersistedClassifierInfo(
  @Inject context: AnalysisContext
): PersistedClassifierInfo = PersistedClassifierInfo(
  qualifiers = qualifiers.map { it.toPersistedTypeRef() },
  superTypes = superTypes.map { it.toPersistedTypeRef() },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread,
  isSingletonInjectable = isSingletonInjectable
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
      val serializedInfo = info.toPersistedClassifierInfo(context).encode()
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

    if (!info.isSingletonInjectable &&
      info.qualifiers.isEmpty() &&
      info.primaryConstructorPropertyParameters.isEmpty() &&
      !hasAnnotation(InjektFqNames.Provide) &&
      (this !is ClassDescriptor ||
          constructors.none { it.hasAnnotation(InjektFqNames.Provide) }) &&
      info.superTypes.none { it.shouldBePersisted() }
    ) return

    val serializedInfo = info.toPersistedClassifierInfo(context).encode()

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
  (it.classifier.isQualifier && it.classifier.typeParameters.size > 1) ||
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

inline fun <reified T> T.encode(): String = Json.encodeToString(this)
inline fun <reified T> String.decode(): T = Json.decodeFromString(this)

private fun DescriptorVisibility.shouldPersistInfo() = this ==
    DescriptorVisibilities.PUBLIC ||
    this == DescriptorVisibilities.INTERNAL ||
    this == DescriptorVisibilities.PROTECTED
