/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.shaded_injekt.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.kotlin.backend.common.descriptors.*
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
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

/**
 * Stores information about a callable which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
data class CallableInfo(val type: TypeRef, val parameterTypes: Map<Int, TypeRef>)

@OptIn(ExperimentalStdlibApi::class)
fun CallableDescriptor.callableInfo(@Inject ctx: Context): CallableInfo =
  if (this is PropertyAccessorDescriptor) correspondingProperty.callableInfo()
  else trace()!!.getOrPut(InjektWritableSlices.CALLABLE_INFO, this) {
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

        return@getOrPut finalInfo
      }
    }

    val type = run {
      val tags = if (this is ConstructorDescriptor)
        buildList {
          addAll(constructedClass.classifierInfo().tags)
          for (tagAnnotation in getTags(injektFqNames()))
            add(tagAnnotation.type.toTypeRef())
        }
      else emptyList()
      tags.wrap(returnType?.toTypeRef() ?: ctx.nullableAnyType)
    }

    val parameterTypes = buildMap<Int, TypeRef> {
      for (parameter in allParameters)
        this[parameter.injektIndex()] = parameter.type.toTypeRef()
    }

    val info = CallableInfo(type, parameterTypes)

    // important to cache the info before persisting it
    trace()!!.record(InjektWritableSlices.CALLABLE_INFO, this, info)

    persistInfoIfNeeded(info)

    return info
  }

private fun CallableDescriptor.persistInfoIfNeeded(info: CallableInfo, @Inject ctx: Context) {
  if (isExternalDeclaration() || isDeserializedDeclaration()) return

  if (!visibility.shouldPersistInfo() &&
      safeAs<ConstructorDescriptor>()?.visibility?.shouldPersistInfo() != true)
        return

  if (hasAnnotation(injektFqNames().callableInfo))
    return

  val shouldPersistInfo = info.type.shouldBePersisted() ||
      info.parameterTypes.any { (_, parameterType) -> parameterType.shouldBePersisted() }

  if (!shouldPersistInfo) return

  val serializedInfo = info.toPersistedCallableInfo().encode()

  updateAnnotation(
    AnnotationDescriptorImpl(
      module().findClassAcrossModuleDependencies(
        ClassId.topLevel(injektFqNames().callableInfo)
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

fun CallableInfo.toPersistedCallableInfo(@Inject ctx: Context) = PersistedCallableInfo(
  type = type.toPersistedTypeRef(),
  parameterTypes = parameterTypes
    .mapValues { it.value.toPersistedTypeRef() }
)

fun PersistedCallableInfo.toCallableInfo(@Inject ctx: Context) = CallableInfo(
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
  val primaryConstructorPropertyParameters: List<String>,
  val isSpread: Boolean,
  val lazyDeclaresInjectables: Lazy<Boolean>
) {
  val superTypes by lazySuperTypes
  val declaresInjectables by lazyDeclaresInjectables
}

fun ClassifierDescriptor.classifierInfo(@Inject ctx: Context): ClassifierInfo =
  trace()!!.getOrPut(InjektWritableSlices.CLASSIFIER_INFO, this) {
    if (isDeserializedDeclaration()) {
      (if (this is TypeParameterDescriptor) {
        containingDeclaration
          .annotations
          .findAnnotation(injektFqNames().typeParameterInfo)
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
        isTag -> listOf(ctx.anyType)
        else -> typeConstructor.supertypes.map { it.toTypeRef() }
      }
    }

    val tags = getTags(injektFqNames())
      .map { it.type.toTypeRef() }

    if (isDeserializedDeclaration() || fqNameSafe.asString() == "java.io.Serializable") {
      ClassifierInfo(
        tags = tags,
        lazySuperTypes = lazySuperTypes,
        primaryConstructorPropertyParameters = emptyList(),
        isSpread = false,
        lazyDeclaresInjectables = lazyOf(false)
      )
    } else {
      val info = if (this is TypeParameterDescriptor) {
        val isSpread = hasAnnotation(injektFqNames().spread) ||
            findPsi()?.safeAs<KtTypeParameter>()
              ?.hasAnnotation(injektFqNames().spread) == true

        ClassifierInfo(
          tags = emptyList(),
          lazySuperTypes = lazySuperTypes,
          primaryConstructorPropertyParameters = emptyList(),
          isSpread = isSpread,
          lazyDeclaresInjectables = lazyOf(false)
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
          isSpread = false,
          lazyDeclaresInjectables = lazy(LazyThreadSafetyMode.NONE) {
            defaultType
              .memberScope
              .getContributedDescriptors()
              .any { it.isProvide() }
          }
        )
      }

      // important to cache the info before persisting it
      trace()!!.record(InjektWritableSlices.CLASSIFIER_INFO, this, info)

      persistInfoIfNeeded(info)

      // no accident
      return info
    }
  }

@Serializable data class PersistedClassifierInfo(
  val tags: List<PersistedTypeRef>,
  val superTypes: List<PersistedTypeRef>,
  val primaryConstructorPropertyParameters: List<String>,
  val isSpread: Boolean,
  val declaresInjectables: Boolean
)

fun PersistedClassifierInfo.toClassifierInfo(@Inject ctx: Context) = ClassifierInfo(
  tags = tags.map { it.toTypeRef() },
  lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { superTypes.map { it.toTypeRef() } },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread,
  lazyDeclaresInjectables = lazyOf(declaresInjectables)
)

fun ClassifierInfo.toPersistedClassifierInfo(@Inject ctx: Context) = PersistedClassifierInfo(
  tags = tags.map { it.toPersistedTypeRef() },
  superTypes = superTypes.map { it.toPersistedTypeRef() },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread,
  declaresInjectables = declaresInjectables
)

private fun ClassifierDescriptor.persistInfoIfNeeded(
  info: ClassifierInfo,
  @Inject ctx: Context
) {
  if (this is TypeParameterDescriptor) {
    val container = containingDeclaration
    if (container is TypeAliasDescriptor) return

    if (!info.isSpread && info.superTypes.none { it.shouldBePersisted() }) return

    fun loadTypeParameterInfos() = (container.annotations
      .findAnnotation(injektFqNames().typeParameterInfo)
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
      .findAnnotation(injektFqNames().typeParameterInfo)
    val initialTypeParameterInfos = loadTypeParameterInfos()
    if (initialTypeParameterInfos[index].isEmpty()) {
      val serializedInfo = info.toPersistedClassifierInfo().encode()
      // load again if the annotation has changed
      val finalTypeParameterInfos =
        if (container.annotations.findAnnotation(injektFqNames().typeParameterInfo) !=
          initialInfosAnnotation) loadTypeParameterInfos()
      else initialTypeParameterInfos
      finalTypeParameterInfos[index] = serializedInfo
      container.updateAnnotation(
        AnnotationDescriptorImpl(
          module().findClassAcrossModuleDependencies(
            ClassId.topLevel(injektFqNames().typeParameterInfo)
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
      !info.declaresInjectables) return

    if (hasAnnotation(injektFqNames().classifierInfo)) return

    val serializedInfo = info.toPersistedClassifierInfo().encode()

    updateAnnotation(
      AnnotationDescriptorImpl(
        module().findClassAcrossModuleDependencies(
          ClassId.topLevel(injektFqNames().classifierInfo)
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

private fun TypeRef.shouldBePersisted(): Boolean = anyType {
  it.classifier.isTag && it.classifier.typeParameters.size > 1
}

@OptIn(ExperimentalStdlibApi::class)
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

fun DescriptorVisibility.shouldPersistInfo() = this ==
    DescriptorVisibilities.PUBLIC ||
    this == DescriptorVisibilities.INTERNAL ||
    this == DescriptorVisibilities.PROTECTED
