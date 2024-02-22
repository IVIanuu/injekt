/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

/**
 * Stores information about a callable which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
data class CallableInfo(
  val type: KotlinType,
  val parameterTypes: Map<Int, KotlinType>,
  val injectParameters: Set<Int>
)

fun CallableDescriptor.callableInfo(ctx: Context): CallableInfo =
  if (this is PropertyAccessorDescriptor) correspondingProperty.callableInfo(ctx)
  else ctx.cached("callable_info", this) {
    if (isDeserializedDeclaration()) {
      val info = annotations
        .findAnnotation(InjektFqNames.DeclarationInfo)
        ?.readChunkedValue()
        ?.decode<PersistedCallableInfo>()
        ?.toCallableInfo(ctx)

      if (info != null) {
        val finalInfo = if (this !is CallableMemberDescriptor ||
          kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
          info
        } else {
          val rootOverriddenCallable = overriddenTreeUniqueAsSequence(false).last()
          val rootClassifier = rootOverriddenCallable.containingDeclaration
            .cast<ClassDescriptor>()
          val classifierInfo = containingDeclaration
            .cast<ClassDescriptor>()
          val superType = classifierInfo.defaultType.firstSuperTypeOrNull {
            it.constructor.declarationDescriptor == rootClassifier
          }!!
          val substitutor = NewTypeSubstitutorByConstructorMap(
            buildMap {
              for ((index, typeParameter) in rootClassifier.declaredTypeParameters.withIndex())
                this[typeParameter.typeConstructor] = superType.arguments[index].type.unwrap()

              for ((index, typeParameter) in rootOverriddenCallable.typeParameters.withIndex())
                this[typeParameter.typeConstructor] = typeParameters[index].defaultType
            }
          )
          info.copy(
            type = info.type.substitute(substitutor),
            parameterTypes = info.parameterTypes.mapValues {
              it.value.substitute(substitutor)
            }
          )
        }

        return@cached finalInfo
      }
    }

    val type = run {
      val tags = if (this is ConstructorDescriptor)
        buildList {
          addAll(constructedClass.classifierInfo(ctx).tags)
          for (tagAnnotation in getTags())
            add(tagAnnotation.type)
        }
      else emptyList()
      tags.wrapTags(returnType ?: ctx.nullableAnyType)
    }

    val parameterTypes = buildMap {
      for (parameter in allParameters)
        this[parameter.injektIndex()] = parameter.type
    }

    val injectParameters = valueParameters
      .filter {
        it.findPsi().safeAs<KtParameter>()?.defaultValue?.text ==
            InjektFqNames.inject.shortName().asString()
      }
      .mapTo(mutableSetOf()) { it.index }

    val info = CallableInfo(type, parameterTypes, injectParameters)

    // important to cache the info before persisting it
    ctx.trace!!.record(sliceOf("callable_info"), this, info)

    persistInfoIfNeeded(info, ctx)

    return info
  }

private fun CallableDescriptor.persistInfoIfNeeded(info: CallableInfo, ctx: Context) {
  if (isExternalDeclaration(ctx) || isDeserializedDeclaration()) return

  if (!visibility.shouldPersistInfo() &&
      safeAs<ConstructorDescriptor>()?.visibility?.shouldPersistInfo() != true)
        return

  if (hasAnnotation(InjektFqNames.DeclarationInfo)) return

  val shouldPersistInfo = info.injectParameters.isNotEmpty() ||
      info.type.shouldBePersisted() ||
      info.parameterTypes.any { (_, parameterType) -> parameterType.shouldBePersisted() }

  if (!shouldPersistInfo) return

  val serializedInfo = info.toPersistedCallableInfo(ctx).encode()

  updateAnnotation(
    AnnotationDescriptorImpl(
      ctx.module.findClassAcrossModuleDependencies(
        ClassId.topLevel(InjektFqNames.DeclarationInfo)
      )?.defaultType ?: return,
      mapOf("values".asNameId() to serializedInfo.toChunkedArrayValue()),
      SourceElement.NO_SOURCE
    )
  )
}

@Serializable data class PersistedCallableInfo(
  val type: PersistedKotlinType,
  val parameterTypes: Map<Int, PersistedKotlinType>,
  val injectParameters: Set<Int>
)

fun CallableInfo.toPersistedCallableInfo(ctx: Context) = PersistedCallableInfo(
  type = type.toPersistedKotlinType(ctx),
  parameterTypes = parameterTypes
    .mapValues { it.value.toPersistedKotlinType(ctx) },
  injectParameters = injectParameters
)

fun PersistedCallableInfo.toCallableInfo(ctx: Context) = CallableInfo(
  type = type.toKotlinType(ctx),
  parameterTypes = parameterTypes.mapValues { it.value.toKotlinType(ctx) },
  injectParameters = injectParameters
)

/**
 * Stores information about a classifier which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
class ClassifierInfo(val tags: List<KotlinType>, val lazySuperTypes: Lazy<List<KotlinType>>) {
  val superTypes by lazySuperTypes
}

fun ClassifierDescriptor.classifierInfo(ctx: Context): ClassifierInfo =
  ctx.cached("classifier_info", this) {
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
          ?.toClassifierInfo(ctx)
      } else {
        annotations
          .findAnnotation(InjektFqNames.DeclarationInfo)
          ?.readChunkedValue()
          ?.cast<String>()
          ?.decode<PersistedClassifierInfo>()
          ?.toClassifierInfo(ctx)
      })?.let {
        return@cached it
      }
    }

    val expandedType = (original as? TypeAliasDescriptor)?.underlyingType

    val isTag = hasAnnotation(InjektFqNames.Tag)

    val lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      when {
        expandedType != null -> listOf(expandedType)
        isTag -> listOf(ctx.anyType)
        else -> typeConstructor.supertypes.toList()
      }
    }

    val tags = getTags().map { it.type }

    if (isDeserializedDeclaration() || fqNameSafe.asString() == "java.io.Serializable") {
      ClassifierInfo(
        tags = tags,
        lazySuperTypes = lazySuperTypes
      )
    } else {
      val info = if (this is TypeParameterDescriptor)
        ClassifierInfo(tags = emptyList(), lazySuperTypes = lazySuperTypes)
      else ClassifierInfo(tags = tags, lazySuperTypes = lazySuperTypes)

      // important to cache the info before persisting it
      ctx.trace!!.record(sliceOf("classifier_info"), this, info)

      persistInfoIfNeeded(info, ctx)

      // no accident
      return info
    }
  }

@Serializable data class PersistedClassifierInfo(
  val tags: List<PersistedKotlinType>,
  val superTypes: List<PersistedKotlinType>
)

fun PersistedClassifierInfo.toClassifierInfo(ctx: Context) = ClassifierInfo(
  tags = tags.map { it.toKotlinType(ctx) },
  lazySuperTypes = lazyOf(superTypes.map { it.toKotlinType(ctx) })
)

fun ClassifierInfo.toPersistedClassifierInfo(ctx: Context) = PersistedClassifierInfo(
  tags = tags.map { it.toPersistedKotlinType(ctx) },
  superTypes = superTypes.map { it.toPersistedKotlinType(ctx) }
)

private fun ClassifierDescriptor.persistInfoIfNeeded(info: ClassifierInfo, ctx: Context) {
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
      val serializedInfo = info.toPersistedClassifierInfo(ctx).encode()
      // load again if the annotation has changed
      val finalTypeParameterInfos =
        if (container.annotations.findAnnotation(InjektFqNames.TypeParameterInfo) !=
          initialInfosAnnotation) loadTypeParameterInfos()
      else initialTypeParameterInfos
      finalTypeParameterInfos[index] = serializedInfo
      container.updateAnnotation(
        AnnotationDescriptorImpl(
          ctx.module.findClassAcrossModuleDependencies(
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
      info.superTypes.none { it.shouldBePersisted() }) return

    if (hasAnnotation(InjektFqNames.DeclarationInfo)) return

    val serializedInfo = info.toPersistedClassifierInfo(ctx).encode()

    updateAnnotation(
      AnnotationDescriptorImpl(
        ctx.module.findClassAcrossModuleDependencies(
          ClassId.topLevel(InjektFqNames.DeclarationInfo)
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

private fun KotlinType.shouldBePersisted(): Boolean = anyType {
  it.constructor.declarationDescriptor?.hasAnnotation(InjektFqNames.Tag) == true
}

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
