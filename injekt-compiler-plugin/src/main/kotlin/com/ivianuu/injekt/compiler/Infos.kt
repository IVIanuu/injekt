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
import com.ivianuu.injekt.compiler.analysis.InjectNParameterDescriptor
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.anyType
import com.ivianuu.injekt.compiler.resolution.firstSuperTypeOrNull
import com.ivianuu.injekt.compiler.resolution.injectNParameters
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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
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
import org.jetbrains.kotlin.descriptors.annotations.TargetedAnnotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.resolve.FunctionImportedFromObject
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.types.ErrorType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
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
  val scopeComponentType: TypeRef? = null,
  val injectNParameters: List<InjectNParameterDescriptor>
)

@WithInjektContext fun CallableDescriptor.callableInfo(): CallableInfo =
  trace!!.getOrPut(InjektWritableSlices.CALLABLE_INFO, this) {
    if (isDeserializedDeclaration()) {
      val info = annotations
        .findAnnotation(injektFqNames.callableInfo)
        ?.readChunkedValue()
        ?.decode<PersistedCallableInfo>()
        ?.toCallableInfo(this)

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

    findPsi()?.safeAs<KtDeclaration>()
      ?.let { declaration ->
        annotations.forEach {
          fixTypes(it.type, declaration)
        }
      }

    val type = run {
      val tags = if (this is ConstructorDescriptor)
        constructedClass.classifierInfo().tags +
            getAnnotatedAnnotations(injektFqNames.tag)
              .map { it.type.toTypeRef() }
      else emptyList()
      tags.wrap(returnType?.toTypeRef() ?: context.nullableAnyType)
    }

    val injectNParameters = injectNParameters()

    val allParameters = (if (this is ConstructorDescriptor) valueParameters else allParameters) +
        injectNParameters

    val parameterTypes = allParameters
      .map {
        it.injektIndex() to if (it is InjectNParameterDescriptor) it.typeRef else it.type.toTypeRef()
      }
      .toMap()

    val injectParameters = allParameters
      .filter {
        it.hasAnnotation(injektFqNames.inject) ||
            ((this is FunctionInvokeDescriptor ||
                (this is InjectFunctionDescriptor &&
                    underlyingDescriptor is FunctionInvokeDescriptor)) &&
                it.type.hasAnnotation(injektFqNames.inject))
      }
      .mapTo(mutableSetOf()) { it.injektIndex() }

    val scopeComponentType = (annotations.findAnnotation(injektFqNames.scoped) ?:
      safeAs<ConstructorDescriptor>()?.constructedClass?.annotations?.findAnnotation(injektFqNames.scoped))
      ?.type?.arguments?.single()?.type?.toTypeRef()

    val info = CallableInfo(
      type = type,
      parameterTypes = parameterTypes,
      injectParameters = injectParameters,
      scopeComponentType = scopeComponentType,
      injectNParameters = injectNParameters
    )

    // important to cache the info before persisting it
    trace!!.record(InjektWritableSlices.CALLABLE_INFO, this, info)

    persistInfoIfNeeded(info)

    addInjectNInfo()

    return info
  }

@WithInjektContext private fun CallableDescriptor.persistInfoIfNeeded(info: CallableInfo) {
  if (isExternalDeclaration() || isDeserializedDeclaration()) return

  if ((this !is ConstructorDescriptor && !visibility.shouldPersistInfo()) ||
    (this is ConstructorDescriptor && !constructedClass.visibility.shouldPersistInfo()))
      return

  if (hasAnnotation(injektFqNames.callableInfo))
    return

  val shouldPersistInfo = hasAnnotation(injektFqNames.provide) ||
      containingDeclaration.hasAnnotation(injektFqNames.component) ||
      containingDeclaration.hasAnnotation(injektFqNames.entryPoint) ||
      (this is ConstructorDescriptor &&
          constructedClass.hasAnnotation(injektFqNames.provide)) ||
      (this is PropertyDescriptor &&
          primaryConstructorPropertyValueParameter()?.isProvide() == true) ||
      safeAs<FunctionDescriptor>()
        ?.valueParameters
        ?.any { it.hasAnnotation(injektFqNames.inject) } == true ||
      info.type.shouldBePersisted() ||
      info.parameterTypes.any { (_, parameterType) ->
        parameterType.shouldBePersisted()
      } ||
      info.scopeComponentType != null ||
      info.injectNParameters.isNotEmpty()

  if (!shouldPersistInfo) return

  val serializedInfo = info.toPersistedCallableInfo().encode()

  updateAnnotation(
    AnnotationDescriptorImpl(
      module.findClassAcrossModuleDependencies(
        ClassId.topLevel(injektFqNames.callableInfo)
      )?.defaultType ?: return,
      mapOf("values".asNameId() to serializedInfo.toChunkedArrayValue()),
      SourceElement.NO_SOURCE
    )
  )
}

@Serializable data class PersistedCallableInfo(
  @SerialName("0") val type: PersistedTypeRef,
  @SerialName("1") val parameterTypes: Map<Int, PersistedTypeRef> = emptyMap(),
  @SerialName("2") val injectParameters: Set<Int> = emptySet(),
  @SerialName("3") val scopeComponentType: PersistedTypeRef? = null,
  @SerialName("4") val injectNParameters: List<PersistedTypeRef>
)

@WithInjektContext fun CallableInfo.toPersistedCallableInfo() = PersistedCallableInfo(
  type = type.toPersistedTypeRef(),
  parameterTypes = parameterTypes
    .mapValues { it.value.toPersistedTypeRef() },
  injectParameters = injectParameters,
  scopeComponentType = scopeComponentType?.toPersistedTypeRef(),
  injectNParameters = injectNParameters.map { it.typeRef.toPersistedTypeRef() }
)

@WithInjektContext fun PersistedCallableInfo.toCallableInfo(callable: CallableDescriptor) =
  CallableInfo(
    type = type.toTypeRef(),
    parameterTypes = parameterTypes
      .mapValues { it.value.toTypeRef() },
    injectParameters = injectParameters,
    scopeComponentType = scopeComponentType?.toTypeRef(),
    injectNParameters = injectNParameters.mapIndexed { index, type ->
      InjectNParameterDescriptor(
        callable,
        callable.valueParameters.size + index,
        type.toTypeRef()
      )
    }
  )

/**
 * Stores information about a classifier which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
class ClassifierInfo(
  val tags: List<TypeRef> = emptyList(),
  val scopeComponentType: TypeRef? = null,
  val entryPointComponentType: TypeRef? = null,
  val lazySuperTypes: Lazy<List<TypeRef>> = lazy(LazyThreadSafetyMode.NONE) { emptyList() },
  val primaryConstructorPropertyParameters: List<String> = emptyList(),
  val isSpread: Boolean = false,
  val injectNParameters: List<InjectNParameterDescriptor>
) {
  val superTypes by lazySuperTypes
}

@WithInjektContext fun ClassifierDescriptor.classifierInfo(): ClassifierInfo =
  trace!!.getOrPut(InjektWritableSlices.CLASSIFIER_INFO, this) {
    if (isDeserializedDeclaration()) {
      (if (this is TypeParameterDescriptor) {
        containingDeclaration
          .annotations
          .findAnnotation(injektFqNames.typeParameterInfos)
          ?.readChunkedValue()
          ?.split("=:=")
          ?.get(cast<TypeParameterDescriptor>().index)
          ?.takeIf { it.isNotEmpty() }
          ?.decode<PersistedClassifierInfo>()
          ?.toClassifierInfo(this)
      } else {
        annotations
          .findAnnotation(injektFqNames.classifierInfo)
          ?.readChunkedValue()
          ?.cast<String>()
          ?.decode<PersistedClassifierInfo>()
          ?.toClassifierInfo(this)
      })?.let {
        return@getOrPut it
      }
    }

    findPsi()?.safeAs<KtDeclaration>()
      ?.let { declaration ->
        annotations.forEach {
          fixTypes(it.type, declaration)
        }
      }

    val expandedType = (original as? TypeAliasDescriptor)?.underlyingType
      ?.toTypeRef()

    val isTag = hasAnnotation(injektFqNames.tag)

    val lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      when {
        expandedType != null -> listOf(expandedType)
        isTag -> listOf(context.anyType)
        else -> typeConstructor.supertypes.map { it.toTypeRef() }
      }
    }

    val isDeserialized = isDeserializedDeclaration()

    val tags = getAnnotatedAnnotations(injektFqNames.tag)
      .map { it.type.toTypeRef() }

    val scopeComponentType = annotations.findAnnotation(injektFqNames.scoped)
      ?.type?.arguments?.single()?.type?.toTypeRef()

    val entryPointComponentType = annotations.findAnnotation(injektFqNames.entryPoint)
      ?.type?.arguments?.single()?.type?.toTypeRef()

    val primaryConstructorPropertyParameters = if (isDeserialized) emptyList()
    else safeAs<ClassDescriptor>()
      ?.unsubstitutedPrimaryConstructor
      ?.valueParameters
      ?.filter { it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true }
      ?.map { it.name.asString() }
      ?: emptyList()

    val isSpread = if (isDeserialized) false
    else hasAnnotation(injektFqNames.spread) ||
        findPsi()?.safeAs<KtTypeParameter>()
          ?.hasAnnotation(injektFqNames.spread) == true

    val injectNParameters = injectNTypes()
      .mapIndexed { index, parameterType ->
        InjectNParameterDescriptor(
          this,
          index,
          parameterType
        )
      }

    val info = ClassifierInfo(
      tags = tags,
      scopeComponentType = scopeComponentType,
      entryPointComponentType = entryPointComponentType,
      lazySuperTypes = lazySuperTypes,
      primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
      isSpread = isSpread,
      injectNParameters = injectNParameters
    )

    // important to cache the info before persisting it
    trace!!.record(InjektWritableSlices.CLASSIFIER_INFO, this, info)

    persistInfoIfNeeded(info)

    addInjectNInfo()

    return info
  }

@Serializable data class PersistedClassifierInfo(
  @SerialName("0") val tags: List<PersistedTypeRef>,
  @SerialName("1") val scopeComponentType: PersistedTypeRef?,
  @SerialName("2") val entryPointComponentType: PersistedTypeRef?,
  @SerialName("3") val superTypes: List<PersistedTypeRef>,
  @SerialName("4") val primaryConstructorPropertyParameters: List<String>,
  @SerialName("5") val isSpread: Boolean,
  @SerialName("6") val injectNParameters: List<PersistedTypeRef>
)

@WithInjektContext fun PersistedClassifierInfo.toClassifierInfo(
  descriptor: ClassifierDescriptor
) = ClassifierInfo(
  tags = tags.map { it.toTypeRef() },
  scopeComponentType = scopeComponentType?.toTypeRef(),
  entryPointComponentType = entryPointComponentType?.toTypeRef(),
  lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { superTypes.map { it.toTypeRef() } },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread,
  injectNParameters = injectNParameters.map { type ->
    InjectNParameterDescriptor(
      descriptor,
      0,
      type.toTypeRef()
    )
  }
)

@WithInjektContext fun ClassifierInfo.toPersistedClassifierInfo() = PersistedClassifierInfo(
  tags = tags.map { it.toPersistedTypeRef() },
  scopeComponentType = scopeComponentType?.toPersistedTypeRef(),
  entryPointComponentType = entryPointComponentType?.toPersistedTypeRef(),
  superTypes = superTypes.map { it.toPersistedTypeRef() },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread,
  injectNParameters = injectNParameters.map { it.typeRef.toPersistedTypeRef() }
)

@WithInjektContext private fun ClassifierDescriptor.persistInfoIfNeeded(info: ClassifierInfo) {
  if (isExternalDeclaration() || isDeserializedDeclaration()) return

  if (this is TypeParameterDescriptor) {
    val container = containingDeclaration
    if (container is TypeAliasDescriptor) return

    if (!info.isSpread && info.superTypes.none { it.shouldBePersisted() }) return

    fun loadTypeParameterInfos() = (container.annotations
      .findAnnotation(injektFqNames.typeParameterInfos)
      ?.readChunkedValue()
      ?.split("=:=")
      ?: run {
        when (container) {
          is CallableDescriptor -> container.typeParameters
          is ClassifierDescriptorWithTypeParameters -> container.declaredTypeParameters
          else -> throw AssertionError("Unexpected container $container")
        }.map { "" }
      }).toMutableList()

    val initialInfosAnnotation = container.annotations
      .findAnnotation(injektFqNames.typeParameterInfos)
    val initialTypeParameterInfos = loadTypeParameterInfos()
    if (initialTypeParameterInfos[index].isEmpty()) {
      val serializedInfo = info.toPersistedClassifierInfo().encode()
      // load again if the annotation has changed
      val finalTypeParameterInfos =
        if (container.annotations.findAnnotation(injektFqNames.typeParameterInfos) !=
          initialInfosAnnotation) loadTypeParameterInfos()
      else initialTypeParameterInfos
      finalTypeParameterInfos[index] = serializedInfo
      container.updateAnnotation(
        AnnotationDescriptorImpl(
          module.findClassAcrossModuleDependencies(
            ClassId.topLevel(injektFqNames.typeParameterInfos)
          )?.defaultType ?: return,
          mapOf("values".asNameId() to finalTypeParameterInfos.joinToString("=:=").toChunkedArrayValue()),
          SourceElement.NO_SOURCE
        )
      )
    }
  } else if (this is DeclarationDescriptorWithVisibility) {
    if (!visibility.shouldPersistInfo()) return
    if (hasAnnotation(injektFqNames.classifierInfo)) return

    if (info.tags.isEmpty() &&
      info.primaryConstructorPropertyParameters.isEmpty() &&
      !hasAnnotation(injektFqNames.provide) &&
      !hasAnnotation(injektFqNames.component) &&
      (this !is ClassDescriptor ||
          constructors.none { it.hasAnnotation(injektFqNames.provide) }) &&
      info.superTypes.none { it.shouldBePersisted() } &&
      info.entryPointComponentType == null &&
      info.scopeComponentType == null &&
      info.injectNParameters.isEmpty()
    ) return

    val serializedInfo = info.toPersistedClassifierInfo().encode()

    updateAnnotation(
      AnnotationDescriptorImpl(
        module.findClassAcrossModuleDependencies(
          ClassId.topLevel(injektFqNames.classifierInfo)
        )?.defaultType ?: return,
        mapOf("values".asNameId() to serializedInfo.toChunkedArrayValue()),
        SourceElement.NO_SOURCE
      )
    )
  }
}

private fun AnnotationDescriptor.readChunkedValue() = allValueArguments
  .values
  .single()
  .cast<ArrayValue>()
  .value
  .map { it.value.toString() }
  .joinToString("")

private fun String.toChunkedArrayValue() = ArrayValue(
  chunked((65535 * 0.8f).toInt()).map { StringValue(it) }
) { it.builtIns.array.defaultType.replace(listOf(it.builtIns.stringType.asTypeProjection())) }

private fun TypeRef.shouldBePersisted(): Boolean = anyType {
  (it.classifier.isTag && it.classifier.typeParameters.size > 1) ||
      (it.classifier.isTypeAlias && it.isSuspendFunctionType) ||
      it.injectNTypes.isNotEmpty() ||
      it.scopeComponentType != null
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

@WithInjektContext fun DeclarationDescriptor.addInjectNInfo() {
  if (isDeserializedDeclaration()) return

  findPsi().safeAs<KtDeclaration>()?.let { declaration ->
    annotations.forEach {
      fixTypes(it.type, declaration)
    }
  }

  annotations.forEach { it.type.addInjectNInfo() }

  if (this is CallableDescriptor) {
    (this as Annotated).addInjectNInfo()

    returnType?.addInjectNInfo()
    dispatchReceiverParameter?.type?.addInjectNInfo()
    extensionReceiverParameter?.type?.addInjectNInfo()
    valueParameters.forEach {
      it.type.addInjectNInfo()
      it.varargElementType?.addInjectNInfo()
    }
  }
}

@WithInjektContext fun Annotated.addInjectNInfo() {
  if (hasAnnotation(injektFqNames.inject2) &&
    !hasAnnotation(injektFqNames.injectNInfo)) {
    val injectNTypes = injectNTypes()
      .map { it.toPersistedTypeRef() }

    val transform: List<AnnotationDescriptor>.() -> List<AnnotationDescriptor> = {
      this + AnnotationDescriptorImpl(
        module.findClassAcrossModuleDependencies(
          ClassId.topLevel(injektFqNames.injectNInfo)
        )?.defaultType!!,
        mapOf("values".asNameId() to ArrayValue(
          injectNTypes.map { StringValue(it.encode()) }
        ) { it.builtIns.array.defaultType.replace(listOf(it.builtIns.stringType.asTypeProjection())) }),
        SourceElement.NO_SOURCE
      )
    }

    when {
      annotations is TargetedAnnotations -> annotations.updatePrivateFinalField(
        TargetedAnnotations::class,
        "standardAnnotations",
        transform
      )
      annotations.javaClass.name == "org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl" ->
        annotations.updatePrivateFinalField(
          annotations.javaClass.kotlin,
          "annotations",
          transform
        )
      else -> throw AssertionError("Unexpected annotations $annotations $this")
    }
  }
}

@WithInjektContext fun KotlinType.addInjectNInfo() {
  val seen = mutableSetOf<KotlinType>()
  fun KotlinType.visit() {
    if (this in seen) return
    seen += this
    (this as Annotated).addInjectNInfo()
    arguments.forEach { it.type.visit() }
  }

  visit()
}

@WithInjektContext fun fixTypes(type: KotlinType, declaration: KtDeclaration) {
  val descriptor = declaration.descriptor<DeclarationDescriptor>()

  val typeParameters = when (descriptor) {
    is ClassDescriptor -> descriptor.declaredTypeParameters
    is CallableDescriptor -> descriptor.typeParameters
    else -> emptyList()
  }

  if (typeParameters.isNotEmpty()) {
    fun fixUnresolved(type: KotlinType) {
      val arguments = type.arguments as? MutableList<TypeProjection> ?: return
      val replacements = mutableMapOf<Int, TypeProjection>()
      for ((i, argument) in type.arguments.withIndex()) {
        val argumentType = argument.type
        if (argumentType is ErrorType) {
          val typeParameter = typeParameters.singleOrNull {
            it.name.asString() == argumentType.presentableName
          }
          if (typeParameter != null) {
            trace!!.record(
              InjektWritableSlices.FIXED_TYPE,
              argumentType.presentableName,
              Unit
            )
            replacements[i] = typeParameter.defaultType.asTypeProjection()
          }
        } else {
          fixUnresolved(argumentType)
        }
      }

      replacements.forEach {
        arguments[it.key] = it.value
      }
    }
    fixUnresolved(type)
  }
}
