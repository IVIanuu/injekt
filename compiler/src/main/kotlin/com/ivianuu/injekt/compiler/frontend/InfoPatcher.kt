/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.frontend

import com.ivianuu.injekt.compiler.*
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
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InfoPatcher(private val baseCtx: Context) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    val ctx = baseCtx.withTrace(context.trace)

    // requesting infos triggers saving them
    when (descriptor) {
      is ClassDescriptor -> if (descriptor.visibility.shouldPersistInfo()) {
        descriptor.classifierInfo(ctx)
        descriptor.declaredTypeParameters
          .forEach { it.classifierInfo(ctx) }
        descriptor.constructors
          .forEach { it.callableInfo(ctx) }
      }
      is CallableDescriptor -> if (descriptor.visibility.shouldPersistInfo()) {
        descriptor.callableInfo(ctx)
        descriptor.typeParameters
          .forEach { it.classifierInfo(ctx) }
      }
      is TypeAliasDescriptor -> if (descriptor.visibility.shouldPersistInfo()) {
        descriptor.classifierInfo(ctx)
        descriptor.declaredTypeParameters
          .forEach { it.classifierInfo(ctx) }
      }
    }
  }
}

/**
 * Stores information about a callable which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
data class CallableInfo(
  val type: InjektType,
  val parameterTypes: Map<Int, InjektType>,
  val injectParameters: Set<Int>
)

fun CallableDescriptor.callableInfo(ctx: Context): CallableInfo =
  if (this is PropertyAccessorDescriptor) correspondingProperty.callableInfo(ctx)
  else ctx.cached("callable_info", this) {
    if (isDeserializedDeclaration()) {
      val info = annotations
        .findAnnotation(InjektFqNames.DeclarationInfo.asSingleFqName())
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
            .toInjektClassifier(ctx)
          val classifierInfo = containingDeclaration
            .cast<ClassDescriptor>()
            .toInjektClassifier(ctx)
          val superType = classifierInfo.defaultType.firstSuperTypeOrNull {
            it.classifier == rootClassifier
          }!!
          val substitutionMap = buildMap<InjektClassifier, InjektType> {
            for ((index, typeParameter) in rootClassifier.typeParameters.withIndex())
              this[typeParameter] = superType.arguments[index]

            for ((index, typeParameter) in rootOverriddenCallable.typeParameters.withIndex())
              this[typeParameter.toInjektClassifier(ctx)] = typeParameters[index].defaultType.toInjektType(ctx)
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
          addAll(constructedClass.classifierInfo(ctx).tags)
          for (tagAnnotation in getTags())
            add(tagAnnotation.type.toInjektType(ctx))
        }
      else emptyList()
      tags.wrap(returnType?.toInjektType(ctx) ?: ctx.nullableAnyType)
    }

    val parameterTypes = buildMap {
      for (parameter in allParameters)
        this[parameter.injektIndex()] = parameter.type.toInjektType(ctx)
    }

    val injectParameters = valueParameters
      .filter {
        it.findPsi().safeAs<KtParameter>()?.defaultValue?.text ==
            InjektFqNames.inject.callableName.asString()
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
        InjektFqNames.DeclarationInfo
      )?.defaultType ?: return,
      mapOf("values".asNameId() to serializedInfo.toChunkedArrayValue()),
      SourceElement.NO_SOURCE
    )
  )
}

@Serializable data class PersistedCallableInfo(
  val type: PersistedInjektType,
  val parameterTypes: Map<Int, PersistedInjektType>,
  val injectParameters: Set<Int>
)

fun CallableInfo.toPersistedCallableInfo(ctx: Context) = PersistedCallableInfo(
  type = type.toPersistedInjektType(ctx),
  parameterTypes = parameterTypes
    .mapValues { it.value.toPersistedInjektType(ctx) },
  injectParameters = injectParameters
)

fun PersistedCallableInfo.toCallableInfo(ctx: Context) = CallableInfo(
  type = type.toInjektType(ctx),
  parameterTypes = parameterTypes
    .mapValues { it.value.toInjektType(ctx) },
  injectParameters = injectParameters
)

/**
 * Stores information about a classifier which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
class ClassifierInfo(val tags: List<InjektType>, val lazySuperTypes: Lazy<List<InjektType>>) {
  val superTypes by lazySuperTypes
}

fun ClassifierDescriptor.classifierInfo(ctx: Context): ClassifierInfo =
  ctx.cached("classifier_info", this) {
    if (isDeserializedDeclaration()) {
      (if (this is TypeParameterDescriptor) {
        containingDeclaration
          .annotations
          .findAnnotation(InjektFqNames.TypeParameterInfo.asSingleFqName())
          ?.readChunkedValue()
          ?.split("=:=")
          ?.get(cast<TypeParameterDescriptor>().index)
          ?.takeIf { it.isNotEmpty() }
          ?.decode<PersistedClassifierInfo>()
          ?.toClassifierInfo(ctx)
      } else {
        annotations
          .findAnnotation(InjektFqNames.DeclarationInfo.asSingleFqName())
          ?.readChunkedValue()
          ?.cast<String>()
          ?.decode<PersistedClassifierInfo>()
          ?.toClassifierInfo(ctx)
      })?.let {
        return@cached it
      }
    }

    val expandedType = (original as? TypeAliasDescriptor)?.underlyingType
      ?.toInjektType(ctx)

    val isTag = hasAnnotation(InjektFqNames.Tag)

    val lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      when {
        expandedType != null -> listOf(expandedType)
        isTag -> listOf(ctx.anyType)
        else -> typeConstructor.supertypes.map { it.toInjektType(ctx) }
      }
    }

    val tags = getTags()
      .map { it.type.toInjektType(ctx) }

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
  val tags: List<PersistedInjektType>,
  val superTypes: List<PersistedInjektType>
)

fun PersistedClassifierInfo.toClassifierInfo(ctx: Context) = ClassifierInfo(
  tags = tags.map { it.toInjektType(ctx) },
  lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { superTypes.map { it.toInjektType(ctx) } }
)

fun ClassifierInfo.toPersistedClassifierInfo(ctx: Context) = PersistedClassifierInfo(
  tags = tags.map { it.toPersistedInjektType(ctx) },
  superTypes = superTypes.map { it.toPersistedInjektType(ctx) }
)

private fun ClassifierDescriptor.persistInfoIfNeeded(info: ClassifierInfo, ctx: Context) {
  if (this is TypeParameterDescriptor) {
    val container = containingDeclaration
    if (container is TypeAliasDescriptor) return

    if (info.superTypes.none { it.shouldBePersisted() }) return

    fun loadTypeParameterInfos() = (container.annotations
      .findAnnotation(InjektFqNames.TypeParameterInfo.asSingleFqName())
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
      .findAnnotation(InjektFqNames.TypeParameterInfo.asSingleFqName())
    val initialTypeParameterInfos = loadTypeParameterInfos()
    if (initialTypeParameterInfos[index].isEmpty()) {
      val serializedInfo = info.toPersistedClassifierInfo(ctx).encode()
      // load again if the annotation has changed
      val finalTypeParameterInfos =
        if (container.annotations.findAnnotation(InjektFqNames.TypeParameterInfo.asSingleFqName()) !=
          initialInfosAnnotation) loadTypeParameterInfos()
        else initialTypeParameterInfos
      finalTypeParameterInfos[index] = serializedInfo
      container.updateAnnotation(
        AnnotationDescriptorImpl(
          ctx.module.findClassAcrossModuleDependencies(
            ClassId.topLevel(InjektFqNames.TypeParameterInfo.asSingleFqName())
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
          ClassId.topLevel(InjektFqNames.DeclarationInfo.asSingleFqName())
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

private fun InjektType.shouldBePersisted(): Boolean = anyType { it.classifier.isTag }

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

@Serializable data class PersistedInjektType(
  val classifierKey: String,
  val arguments: List<PersistedInjektType> = emptyList(),
  val isStarProjection: Boolean,
  val variance: TypeVariance,
  val isMarkedNullable: Boolean,
  val isProvide: Boolean
)

fun InjektType.toPersistedInjektType(ctx: Context): PersistedInjektType =
  PersistedInjektType(
    classifierKey = classifier.key,
    arguments = arguments.map { it.toPersistedInjektType(ctx) },
    isStarProjection = isStarProjection,
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide
  )

fun PersistedInjektType.toInjektType(ctx: Context): InjektType {
  if (isStarProjection) return STAR_PROJECTION_TYPE
  val classifier = classifierDescriptorForKey(classifierKey, ctx)
    .toInjektClassifier(ctx)
  val arguments = if (classifier.isTag) {
    arguments
      .map { it.toInjektType(ctx) } +
        listOfNotNull(
          if (arguments.size < classifier.typeParameters.size)
            ctx.nullableAnyType
          else null
        )
  } else arguments.map { it.toInjektType(ctx) }
  return classifier.untaggedType.copy(
    arguments = arguments,
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide
  )
}
