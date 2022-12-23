/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.analysis.ContextFunctionDescriptor
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
import kotlinx.serialization.decodeFromString
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
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.resolve.FunctionImportedFromObject
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Stores information about a callable which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
data class CallableInfo(val type: TypeRef, val parameterTypes: Map<Int, TypeRef>)

fun CallableDescriptor.callableInfo(ctx: Context): CallableInfo =
  if (this is PropertyAccessorDescriptor) correspondingProperty.callableInfo(ctx)
  else ctx.trace!!.getOrPut(InjektWritableSlices.CALLABLE_INFO, this) {
    if (isDeserializedDeclaration()) {
      val info = annotations
        .findAnnotation(InjektFqNames.CallableInfo)
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
            .toClassifierRef(ctx)
          val classifierInfo = containingDeclaration
            .cast<ClassDescriptor>()
            .toClassifierRef(ctx)
          val superType = classifierInfo.defaultType.firstSuperTypeOrNull {
            it.classifier == rootClassifier
          }!!
          val substitutionMap = buildMap<ClassifierRef, TypeRef> {
            for ((index, typeParameter) in rootClassifier.typeParameters.withIndex())
              this[typeParameter] = superType.arguments[index]

            for ((index, typeParameter) in rootOverriddenCallable.typeParameters.withIndex())
              this[typeParameter.toClassifierRef(ctx)] = typeParameters[index].defaultType.toTypeRef(ctx)
          }
          info.copy(
            type = info.type.substitute(substitutionMap),
            parameterTypes = info.parameterTypes.mapValues {
              it.value.substitute(substitutionMap)
            }
          )
        }

        return@getOrPut finalInfo
      }
    }

    val type = run {
      val tags = if (this is ConstructorDescriptor)
        buildList {
          addAll(constructedClass.classifierInfo(ctx).tags)
          for (tagAnnotation in getTags())
            add(tagAnnotation.type.toTypeRef(ctx))
        }
      else emptyList()
      tags.wrap(returnType?.toTypeRef(ctx) ?: ctx.nullableAnyType)
    }

    val parameterTypes = buildMap<Int, TypeRef> {
      for (parameter in allParametersWithContext)
        this[parameter.injektIndex(ctx)] = parameter.type.toTypeRef(ctx)
    }

    val info = CallableInfo(type, parameterTypes)

    // important to cache the info before persisting it
    ctx.trace.record(InjektWritableSlices.CALLABLE_INFO, this, info)

    persistInfoIfNeeded(info, ctx)

    return info
  }

private fun CallableDescriptor.persistInfoIfNeeded(info: CallableInfo, ctx: Context) {
  if (isExternalDeclaration(ctx) || isDeserializedDeclaration()) return

  if (!visibility.shouldPersistInfo() &&
      safeAs<ConstructorDescriptor>()?.visibility?.shouldPersistInfo() != true)
        return

  if (hasAnnotation(InjektFqNames.CallableInfo))
    return

  val shouldPersistInfo = info.type.shouldBePersisted() ||
      info.parameterTypes.any { (_, parameterType) -> parameterType.shouldBePersisted() }

  if (!shouldPersistInfo) return

  val serializedInfo = info.toPersistedCallableInfo(ctx).encode()

  updateAnnotation(
    AnnotationDescriptorImpl(
      ctx.module.findClassAcrossModuleDependencies(
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

fun CallableInfo.toPersistedCallableInfo(ctx: Context) = PersistedCallableInfo(
  type = type.toPersistedTypeRef(ctx),
  parameterTypes = parameterTypes
    .mapValues { it.value.toPersistedTypeRef(ctx) }
)

fun PersistedCallableInfo.toCallableInfo(ctx: Context) = CallableInfo(
  type = type.toTypeRef(ctx),
  parameterTypes = parameterTypes
    .mapValues { it.value.toTypeRef(ctx) }
)

/**
 * Stores information about a classifier which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
class ClassifierInfo(
  val tags: List<TypeRef>,
  val lazySuperTypes: Lazy<List<TypeRef>>,
  val primaryConstructorPropertyParameters: List<String>,
  val lazyDeclaresProviders: Lazy<Boolean>
) {
  val superTypes by lazySuperTypes
  val declaresProviders by lazyDeclaresProviders
}

fun ClassifierDescriptor.classifierInfo(ctx: Context): ClassifierInfo =
  ctx.trace!!.getOrPut(InjektWritableSlices.CLASSIFIER_INFO, this) {
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
          .findAnnotation(InjektFqNames.ClassifierInfo)
          ?.readChunkedValue()
          ?.cast<String>()
          ?.decode<PersistedClassifierInfo>()
          ?.toClassifierInfo(ctx)
      })?.let {
        return@getOrPut it
      }
    }

    val expandedType = (original as? TypeAliasDescriptor)?.underlyingType
      ?.toTypeRef(ctx)

    val isTag = hasAnnotation(InjektFqNames.Tag)

    val lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      when {
        expandedType != null -> listOf(expandedType)
        isTag -> listOf(ctx.anyType)
        else -> typeConstructor.supertypes.map { it.toTypeRef(ctx) }
      }
    }

    val tags = getTags()
      .map { it.type.toTypeRef(ctx) }

    if (isDeserializedDeclaration() || fqNameSafe.asString() == "java.io.Serializable") {
      ClassifierInfo(
        tags = tags,
        lazySuperTypes = lazySuperTypes,
        primaryConstructorPropertyParameters = emptyList(),
        lazyDeclaresProviders = lazyOf(false)
      )
    } else {
      val info = if (this is TypeParameterDescriptor) {
        ClassifierInfo(
          tags = emptyList(),
          lazySuperTypes = lazySuperTypes,
          primaryConstructorPropertyParameters = emptyList(),
          lazyDeclaresProviders = lazyOf(false)
        )
      } else {
        val primaryConstructorPropertyParameters = safeAs<ClassDescriptor>()
          ?.unsubstitutedPrimaryConstructor
          ?.valueParameters
          ?.transform {
            if (it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true)
              add(it.name.asString())
          }
          ?: emptyList()

        ClassifierInfo(
          tags = tags,
          lazySuperTypes = lazySuperTypes,
          primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
          lazyDeclaresProviders = lazy(LazyThreadSafetyMode.NONE) {
            defaultType
              .memberScope
              .getContributedDescriptors()
              .any { it.isProvide(ctx) }
          }
        )
      }

      // important to cache the info before persisting it
      ctx.trace.record(InjektWritableSlices.CLASSIFIER_INFO, this, info)

      persistInfoIfNeeded(info, ctx)

      // no accident
      return info
    }
  }

@Serializable data class PersistedClassifierInfo(
  val tags: List<PersistedTypeRef>,
  val superTypes: List<PersistedTypeRef>,
  val primaryConstructorPropertyParameters: List<String>,
  val declaresProviders: Boolean
)

fun PersistedClassifierInfo.toClassifierInfo(ctx: Context) = ClassifierInfo(
  tags = tags.map { it.toTypeRef(ctx) },
  lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { superTypes.map { it.toTypeRef(ctx) } },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  lazyDeclaresProviders = lazyOf(declaresProviders)
)

fun ClassifierInfo.toPersistedClassifierInfo(ctx: Context) = PersistedClassifierInfo(
  tags = tags.map { it.toPersistedTypeRef(ctx) },
  superTypes = superTypes.map { it.toPersistedTypeRef(ctx) },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  declaresProviders = declaresProviders
)

private fun ClassifierDescriptor.persistInfoIfNeeded(
  info: ClassifierInfo,
  ctx: Context
) {
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
      info.primaryConstructorPropertyParameters.isEmpty() &&
      info.superTypes.none { it.shouldBePersisted() } &&
      !info.declaresProviders) return

    if (hasAnnotation(InjektFqNames.ClassifierInfo)) return

    val serializedInfo = info.toPersistedClassifierInfo(ctx).encode()

    updateAnnotation(
      AnnotationDescriptorImpl(
        ctx.module.findClassAcrossModuleDependencies(
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
    is ContextFunctionDescriptor -> underlyingDescriptor.updateAnnotation(annotation)
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
