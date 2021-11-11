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
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.resolution.wrap
import com.ivianuu.shaded_injekt.Inject
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
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
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
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.resolve.FunctionImportedFromObject
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
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
  val parameterTypes: Map<Int, TypeRef>,
  val injectParameterIndex: Int?,
  val scopeComponentType: TypeRef?,
  val isEager: Boolean
)

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
            getAnnotatedAnnotations(injektFqNames().tag)
              .map { it.type.toTypeRef() }
      else emptyList()
      tags.wrap(returnType?.toTypeRef() ?: ctx.nullableAnyType)
    }

    val parameterTypes = allParameters
      .map { it.injektIndex() to it.type.toTypeRef() }
      .toMap()

    val injectParameterIndex = if (hasAnnotation(injektFqNames().provide))
      dispatchReceiverParameter?.let { DISPATCH_RECEIVER_INDEX }
        ?: extensionReceiverParameter?.let { EXTENSION_RECEIVER_INDEX }
        ?: 0
    else
      valueParameters
        .firstOrNull {
          it.hasAnnotation(injektFqNames().inject) ||
              ((this is FunctionInvokeDescriptor ||
                  (this is InjectFunctionDescriptor &&
                      underlyingDescriptor is FunctionInvokeDescriptor)) &&
                  it.type.hasAnnotation(injektFqNames().inject))
        }
        ?.injektIndex()

    val scopeAnnotation = annotations.findAnnotation(injektFqNames().scoped) ?:
    safeAs<ConstructorDescriptor>()?.constructedClass?.annotations?.findAnnotation(injektFqNames().scoped)

    val scopeComponentType = scopeAnnotation?.type?.arguments?.single()?.type?.toTypeRef()
    val isEager = scopeAnnotation?.allValueArguments?.values?.singleOrNull()?.value == true

    val info = CallableInfo(
      type = type,
      parameterTypes = parameterTypes,
      injectParameterIndex = injectParameterIndex,
      scopeComponentType = scopeComponentType,
      isEager = isEager
    )

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

  val shouldPersistInfo = hasAnnotation(injektFqNames().provide) ||
      containingDeclaration.hasAnnotation(injektFqNames().component) ||
      containingDeclaration.hasAnnotation(injektFqNames().entryPoint) ||
      safeAs<ConstructorDescriptor>()?.constructedClass
        ?.hasAnnotation(injektFqNames().provide) == true ||
      safeAs<PropertyDescriptor>()?.primaryConstructorPropertyValueParameter()
        ?.isProvide() == true
      valueParameters.any { it.hasAnnotation(injektFqNames().inject) } ||
      info.type.shouldBePersisted() ||
      info.parameterTypes.any { (_, parameterType) ->
        parameterType.shouldBePersisted()
      } ||
      info.scopeComponentType != null

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
  val parameterTypes: Map<Int, PersistedTypeRef>,
  val injectParameterIndex: Int?,
  val scopeComponentType: PersistedTypeRef?,
  val isEager: Boolean
)

fun CallableInfo.toPersistedCallableInfo(@Inject ctx: Context) = PersistedCallableInfo(
  type = type.toPersistedTypeRef(),
  parameterTypes = parameterTypes
    .mapValues { it.value.toPersistedTypeRef() },
  injectParameterIndex = injectParameterIndex,
  scopeComponentType = scopeComponentType?.toPersistedTypeRef(),
  isEager = isEager
)

fun PersistedCallableInfo.toCallableInfo(@Inject ctx: Context) = CallableInfo(
  type = type.toTypeRef(),
  parameterTypes = parameterTypes
    .mapValues { it.value.toTypeRef() },
  injectParameterIndex = injectParameterIndex,
  scopeComponentType = scopeComponentType?.toTypeRef(),
  isEager = isEager
)

/**
 * Stores information about a classifier which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
class ClassifierInfo(
  val tags: List<TypeRef>,
  val scopeComponentType: TypeRef?,
  val isEager: Boolean,
  val entryPointComponentType: TypeRef?,
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
          ?.toClassifierInfo(this)
      } else {
        annotations
          .findAnnotation(injektFqNames().classifierInfo)
          ?.readChunkedValue()
          ?.cast<String>()
          ?.decode<PersistedClassifierInfo>()
          ?.toClassifierInfo(this)
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

    if (isDeserializedDeclaration() || fqNameSafe.asString() == "java.io.Serializable") {
      ClassifierInfo(
        tags = emptyList(),
        scopeComponentType = null,
        isEager = false,
        entryPointComponentType = null,
        lazySuperTypes = lazySuperTypes,
        primaryConstructorPropertyParameters = emptyList(),
        isSpread = false,
        lazyDeclaresInjectables = lazyOf(false)
      )
    } else {
      findPsi()?.safeAs<KtDeclaration>()
        ?.let { declaration ->
          annotations.forEach {
            fixTypes(it.type, declaration)
          }
        }

      val info = if (this is TypeParameterDescriptor) {
        val isSpread = hasAnnotation(injektFqNames().spread) ||
            findPsi()?.safeAs<KtTypeParameter>()
              ?.hasAnnotation(injektFqNames().spread) == true

        ClassifierInfo(
          tags = emptyList(),
          scopeComponentType = null,
          isEager = false,
          entryPointComponentType = null,
          lazySuperTypes = lazySuperTypes,
          primaryConstructorPropertyParameters = emptyList(),
          isSpread = isSpread,
          lazyDeclaresInjectables = lazyOf(false)
        )
      } else {
        val tags = getAnnotatedAnnotations(injektFqNames().tag)
          .map { it.type.toTypeRef() }

        val scopeAnnotation = annotations.findAnnotation(injektFqNames().scoped)
        val scopeComponentType = scopeAnnotation?.type?.arguments?.single()?.type?.toTypeRef()
        val isEager = scopeAnnotation?.allValueArguments?.values?.singleOrNull()?.value == true

        val entryPointComponentType = annotations.findAnnotation(injektFqNames().entryPoint)
          ?.type?.arguments?.single()?.type?.toTypeRef()

        val primaryConstructorPropertyParameters = safeAs<ClassDescriptor>()
          ?.unsubstitutedPrimaryConstructor
          ?.valueParameters
          ?.filter { it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true }
          ?: emptyList()

        ClassifierInfo(
          tags = tags,
          scopeComponentType = scopeComponentType,
          isEager = isEager,
          entryPointComponentType = entryPointComponentType,
          lazySuperTypes = lazySuperTypes,
          primaryConstructorPropertyParameters = primaryConstructorPropertyParameters
            .map { it.name.asString() },
          isSpread = false,
          lazyDeclaresInjectables = lazy {
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
  val scopeComponentType: PersistedTypeRef?,
  val isEager: Boolean,
  val entryPointComponentType: PersistedTypeRef?,
  val superTypes: List<PersistedTypeRef>,
  val primaryConstructorPropertyParameters: List<String>,
  val isSpread: Boolean,
  val declaresInjectables: Boolean
)

fun PersistedClassifierInfo.toClassifierInfo(
  descriptor: ClassifierDescriptor,
  @Inject ctx: Context
) = ClassifierInfo(
  tags = tags.map { it.toTypeRef() },
  scopeComponentType = scopeComponentType?.toTypeRef(),
  isEager = isEager,
  entryPointComponentType = entryPointComponentType?.toTypeRef(),
  lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { superTypes.map { it.toTypeRef() } },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread,
  lazyDeclaresInjectables = lazyOf(declaresInjectables)
)

fun ClassifierInfo.toPersistedClassifierInfo(@Inject ctx: Context) = PersistedClassifierInfo(
  tags = tags.map { it.toPersistedTypeRef() },
  scopeComponentType = scopeComponentType?.toPersistedTypeRef(),
  isEager = isEager,
  entryPointComponentType = entryPointComponentType?.toPersistedTypeRef(),
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

    if (info.tags.isEmpty() &&
      info.primaryConstructorPropertyParameters.isEmpty() &&
      !hasAnnotation(injektFqNames().provide) &&
      !hasAnnotation(injektFqNames().component) &&
      (this !is ClassDescriptor ||
          constructors.none { it.hasAnnotation(injektFqNames().provide) }) &&
      info.superTypes.none { it.shouldBePersisted() } &&
      info.entryPointComponentType == null &&
      info.scopeComponentType == null &&
      !info.declaresInjectables
    ) return

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
  (it.classifier.isTag && it.classifier.typeParameters.size > 1) ||
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

fun DescriptorVisibility.shouldPersistInfo() = this ==
    DescriptorVisibilities.PUBLIC ||
    this == DescriptorVisibilities.INTERNAL ||
    this == DescriptorVisibilities.PROTECTED

fun fixTypes(type: KotlinType, declaration: KtDeclaration, @Inject ctx: Context) {
  val typeParameters = when (val descriptor = declaration.descriptor<DeclarationDescriptor>()) {
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
            trace()!!.record(
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
