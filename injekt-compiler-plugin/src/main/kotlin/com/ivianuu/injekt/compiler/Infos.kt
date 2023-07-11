/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.analysis.InjectFunctionDescriptor
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.anyType
import com.ivianuu.injekt.compiler.resolution.firstSuperTypeOrNull
import com.ivianuu.injekt.compiler.resolution.isProvide
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.resolution.wrap
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotatedImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.FunctionImportedFromObject
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Stores information about a callable which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
data class CallableInfo(val type: TypeRef, val parameterTypes: Map<Int, TypeRef>)

context(Context) fun CallableDescriptor.callableInfo(): CallableInfo =
  if (this is PropertyAccessorDescriptor) correspondingProperty.callableInfo()
  else cached("callable_info", this) {
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
          val substitutionMap = buildMap<ClassifierRef, TypeRef> {
            for ((index, typeParameter) in rootClassifier.typeParameters.withIndex())
              this[typeParameter] = superType.arguments[index]

            for ((index, typeParameter) in rootOverriddenCallable.typeParameters.withIndex())
              this[typeParameter.toClassifierRef()] = typeParameters[index].defaultType.toTypeRef()
          }
          info.copy(
            type = info.type.substitute(substitutionMap),
            parameterTypes = info.parameterTypes.mapValues {
              it.value.substitute(substitutionMap)
            }
          )
        }

        return@cached finalInfo
      }
    }

    val type = run {
      val tags = if (this is ConstructorDescriptor)
        buildList {
          addAll(constructedClass.classifierInfo().tags)
          for (tagAnnotation in getTags())
            add(tagAnnotation.type.toTypeRef())
        }
      else emptyList()
      tags.wrap(returnType?.toTypeRef() ?: nullableAnyType)
    }

    val parameterTypes = buildMap {
      for (parameter in allParametersWithContext)
        this[parameter.injektIndex()] = parameter.type.toTypeRef()
    }

    val info = CallableInfo(type, parameterTypes)

    // important to cache the info before persisting it
    trace!!.record(sliceOf("callable_info"), this, info)

    persistInfoIfNeeded(info)

    return info
  }

context(Context) private fun CallableDescriptor.persistInfoIfNeeded(info: CallableInfo) {
  if (isExternalDeclaration() || isDeserializedDeclaration()) return

  if (!visibility.shouldPersistInfo() &&
      safeAs<ConstructorDescriptor>()?.visibility?.shouldPersistInfo() != true)
        return

  if (hasAnnotation(InjektFqNames.CallableInfo))
    return

  val shouldPersistInfo = info.type.shouldBePersisted() ||
      info.parameterTypes.any { (_, parameterType) -> parameterType.shouldBePersisted() }

  if (!shouldPersistInfo) return

  val serializedInfo = info.toPersistedCallableInfo().encode()

  updateAnnotation(
    AnnotationDescriptorImpl(
      module.findClassAcrossModuleDependencies(
        ClassId.topLevel(InjektFqNames.CallableInfo)
      )?.defaultType ?: return,
      mapOf("values".asNameId() to serializedInfo.toChunkedArrayValue()),
      SourceElement.NO_SOURCE
    )
  )
}

@Serializable data class PersistedCallableInfo(
  val type: PersistedTypeRef,
  val parameterTypes: Map<Int, PersistedTypeRef>
)

context(Context) fun CallableInfo.toPersistedCallableInfo() = PersistedCallableInfo(
  type = type.toPersistedTypeRef(),
  parameterTypes = parameterTypes
    .mapValues { it.value.toPersistedTypeRef() }
)

context(Context) fun PersistedCallableInfo.toCallableInfo() = CallableInfo(
  type = type.toTypeRef(),
  parameterTypes = parameterTypes
    .mapValues { it.value.toTypeRef() }
)

/**
 * Stores information about a classifier which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
class ClassifierInfo(
  val tags: List<TypeRef>,
  val lazySuperTypes: Lazy<List<TypeRef>>,
  val lazyDeclaresInjectables: Lazy<Boolean>
) {
  val superTypes by lazySuperTypes
  val declaresInjectables by lazyDeclaresInjectables
}

context(Context) fun ClassifierDescriptor.classifierInfo(): ClassifierInfo =
  cached("classifier_info", this) {
    if (isDeserializedDeclaration()) {
      (if (this is TypeParameterDescriptor) {
        containingDeclaration
          .annotations
          .findAnnotation(InjektFqNames.TypeParameterInfo)
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
        return@cached it
      }
    }

    val expandedType = (original as? TypeAliasDescriptor)?.underlyingType?.toTypeRef()

    val isTag = hasAnnotation(InjektFqNames.Tag)

    val lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      when {
        expandedType != null -> listOf(expandedType)
        isTag -> listOf(anyType)
        else -> typeConstructor.supertypes.map { it.toTypeRef() }
      }
    }

    val tags = getTags()
      .map { it.type.toTypeRef() }

    if (isDeserializedDeclaration() || fqNameSafe.asString() == "java.io.Serializable") {
      ClassifierInfo(
        tags = tags,
        lazySuperTypes = lazySuperTypes,
        lazyDeclaresInjectables = lazyOf(false)
      )
    } else {
      val info = if (this is TypeParameterDescriptor) {
        ClassifierInfo(
          tags = emptyList(),
          lazySuperTypes = lazySuperTypes,
          lazyDeclaresInjectables = lazyOf(false)
        )
      } else {
        ClassifierInfo(
          tags = tags,
          lazySuperTypes = lazySuperTypes,
          lazyDeclaresInjectables = lazy(LazyThreadSafetyMode.NONE) {
            defaultType
              .memberScope
              .getContributedDescriptors()
              .any { it.isProvide() }
          }
        )
      }

      // important to cache the info before persisting it
      trace!!.record(sliceOf("classifier_info"), this, info)

      persistInfoIfNeeded(info)

      // no accident
      return info
    }
  }

@Serializable data class PersistedClassifierInfo(
  val tags: List<PersistedTypeRef>,
  val superTypes: List<PersistedTypeRef>,
  val declaresInjectables: Boolean
)

context(Context) fun PersistedClassifierInfo.toClassifierInfo() = ClassifierInfo(
  tags = tags.map { it.toTypeRef() },
  lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { superTypes.map { it.toTypeRef() } },
  lazyDeclaresInjectables = lazyOf(declaresInjectables)
)

context(Context) fun ClassifierInfo.toPersistedClassifierInfo() = PersistedClassifierInfo(
  tags = tags.map { it.toPersistedTypeRef() },
  superTypes = superTypes.map { it.toPersistedTypeRef() },
  declaresInjectables = declaresInjectables
)

context(Context) private fun ClassifierDescriptor.persistInfoIfNeeded(info: ClassifierInfo) {
  if (this is TypeParameterDescriptor) {
    val container = containingDeclaration
    if (container is TypeAliasDescriptor) return

    if (info.superTypes.none { it.shouldBePersisted() }) return

    fun loadTypeParameterInfos() = (container.annotations
      .findAnnotation(InjektFqNames.TypeParameterInfo)
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
      .findAnnotation(InjektFqNames.TypeParameterInfo)
    val initialTypeParameterInfos = loadTypeParameterInfos()
    if (initialTypeParameterInfos[index].isEmpty()) {
      val serializedInfo = info.toPersistedClassifierInfo().encode()
      // load again if the annotation has changed
      val finalTypeParameterInfos =
        if (container.annotations.findAnnotation(InjektFqNames.TypeParameterInfo) !=
          initialInfosAnnotation) loadTypeParameterInfos()
      else initialTypeParameterInfos
      finalTypeParameterInfos[index] = serializedInfo
      container.updateAnnotation(
        AnnotationDescriptorImpl(
          module.findClassAcrossModuleDependencies(
            ClassId.topLevel(InjektFqNames.TypeParameterInfo)
          )?.defaultType ?: return,
          mapOf("values".asNameId() to finalTypeParameterInfos.joinToString("=:=").toChunkedArrayValue()),
          SourceElement.NO_SOURCE
        )
      )
    }
  } else if (this is DeclarationDescriptorWithVisibility) {
    if (!visibility.shouldPersistInfo()) return

    if (info.tags.none { it.shouldBePersisted() } &&
      info.superTypes.none { it.shouldBePersisted() } &&
      !info.declaresInjectables) return

    if (hasAnnotation(InjektFqNames.ClassifierInfo)) return

    val serializedInfo = info.toPersistedClassifierInfo().encode()

    updateAnnotation(
      AnnotationDescriptorImpl(
        module.findClassAcrossModuleDependencies(
          ClassId.topLevel(InjektFqNames.ClassifierInfo)
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
  .value.joinToString("") { it.value.toString() }

private fun String.toChunkedArrayValue() = ArrayValue(
  chunked((65535 * 0.8f).toInt()).map { StringValue(it) }
) { it.builtIns.array.defaultType.replace(listOf(it.builtIns.stringType.asTypeProjection())) }

private fun TypeRef.shouldBePersisted(): Boolean = anyType { it.classifier.isTag }

private fun Annotated.updateAnnotation(annotation: AnnotationDescriptor) {
  val newAnnotations = Annotations.create(
    buildList {
      for (existing in annotations)
        if (existing.type != annotation.type)
          add(existing)
      add(annotation)
    }
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

fun DescriptorVisibility.shouldPersistInfo() = this == DescriptorVisibilities.PUBLIC ||
    this == DescriptorVisibilities.INTERNAL ||
    this == DescriptorVisibilities.PROTECTED
