package com.ivianuu.injekt.compiler

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
import org.jetbrains.kotlin.resolve.lazy.descriptors.*
import org.jetbrains.kotlin.utils.addToStdlib.*

/**
 * Stores information about a callable which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
data class CallableInfo(
  val type: TypeRef,
  val parameterTypes: Map<String, TypeRef> = emptyMap(),
  val injectParameters: Set<String> = emptySet(),
  val defaultOnAllErrorsParameters: Set<String> = emptySet()
)

fun CallableDescriptor.callableInfo(
  context: InjektContext,
  trace: BindingTrace
): CallableInfo {
  trace[InjektWritableSlices.CALLABLE_INFO, this]?.let { return it }

  annotations
    .findAnnotation(InjektFqNames.CallableInfo)
    ?.readChunkedValue()
    ?.decode<PersistedCallableInfo>()
    ?.toCallableInfo(context, trace)
    ?.let {
      trace.record(InjektWritableSlices.CALLABLE_INFO, this, it)
      return it
    }

  val type = run {
    val qualifiers = if (this is ConstructorDescriptor)
      getAnnotatedAnnotations(InjektFqNames.Qualifier)
        .map { it.type.toTypeRef(context, trace) }
    else emptyList()
    qualifiers.wrap(returnType!!.toTypeRef(context, trace))
  }

  val parameterTypes = (if (this is ConstructorDescriptor) valueParameters else allParameters)
    .map { it.injektName() to it.type.toTypeRef(context, trace) }
    .toMap()

  val injectParameters = (if (this is ConstructorDescriptor) valueParameters else allParameters)
    .filter {
      it.hasAnnotation(InjektFqNames.Inject) ||
          (this is FunctionInvokeDescriptor ||
              (this is InjectFunctionDescriptor &&
                  underlyingDescriptor is FunctionInvokeDescriptor) &&
              it.type.hasAnnotation(InjektFqNames.Inject))
    }
    .mapTo(mutableSetOf()) { it.injektName() }

  val defaultOnAllErrorsParameters = valueParameters
    .asSequence()
    .filter { it.annotations.hasAnnotation(InjektFqNames.DefaultOnAllErrors) }
    .mapTo(mutableSetOf()) { it.injektName() }

  val info = CallableInfo(
    type = type,
    parameterTypes = parameterTypes,
    injectParameters = injectParameters,
    defaultOnAllErrorsParameters = defaultOnAllErrorsParameters
  )

  trace.record(InjektWritableSlices.CALLABLE_INFO, this, info)

  persistInfoIfNeeded(info, context, trace)

  return info
}

private fun CallableDescriptor.persistInfoIfNeeded(
  info: CallableInfo,
  context: InjektContext,
  trace: BindingTrace
) {
  if (isExternalDeclaration(context) || isDeserializedDeclaration()) return

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
          primaryConstructorPropertyValueParameter(context, trace)
            ?.isProvide(context, trace) == true) ||
      safeAs<FunctionDescriptor>()
        ?.valueParameters
        ?.any { it.hasAnnotation(InjektFqNames.Inject) } == true ||
      info.type.shouldBePersisted() ||
      info.parameterTypes.any { (_, parameterType) ->
        parameterType.shouldBePersisted()
      }

  if (!shouldPersistInfo) return

  val serializedInfo = info.toPersistedCallableInfo(context).encode()

  updateAnnotation(
    AnnotationDescriptorImpl(
      context.module.findClassAcrossModuleDependencies(
        ClassId.topLevel(
          InjektFqNames.CallableInfo
        )
      )?.defaultType ?: return,
      serializedInfo.toChunkedAnnotationArguments(),
      SourceElement.NO_SOURCE
    )
  )
}

@Serializable
data class PersistedCallableInfo(
  @SerialName("0") val type: PersistedTypeRef,
  @SerialName("1") val parameterTypes: Map<String, PersistedTypeRef> = emptyMap(),
  @SerialName("2") val injectParameters: Set<String> = emptySet(),
  @SerialName("3") val defaultOnAllErrorsParameters: Set<String> = emptySet()
)

fun CallableInfo.toPersistedCallableInfo(context: InjektContext) = PersistedCallableInfo(
  type = type.toPersistedTypeRef(context),
  parameterTypes = parameterTypes
    .mapValues { it.value.toPersistedTypeRef(context) },
  injectParameters = injectParameters,
  defaultOnAllErrorsParameters = defaultOnAllErrorsParameters
)

fun PersistedCallableInfo.toCallableInfo(
  context: InjektContext,
  trace: BindingTrace
) = CallableInfo(
  type = type.toTypeRef(context, trace),
  parameterTypes = parameterTypes
    .mapValues { it.value.toTypeRef(context, trace) },
  injectParameters = injectParameters,
  defaultOnAllErrorsParameters = defaultOnAllErrorsParameters
)

/**
 * Stores information about a classifier which is NOT stored by the kotlin compiler
 * but is critical to injekt
 */
class ClassifierInfo(
  val qualifiers: List<TypeRef> = emptyList(),
  val lazySuperTypes: Lazy<List<TypeRef>> = unsafeLazy { emptyList() },
  val primaryConstructorPropertyParameters: List<String> = emptyList(),
  val isSpread: Boolean,
  val isForTypeKey: Boolean,
  val isSingletonInjectable: Boolean = false
) {
  val superTypes by lazySuperTypes
}

fun ClassifierDescriptor.classifierInfo(
  context: InjektContext,
  trace: BindingTrace
): ClassifierInfo {
  trace[InjektWritableSlices.CLASSIFIER_INFO, this]?.let { return it }

  (if (this is TypeParameterDescriptor) {
    containingDeclaration
      .annotations
      .findAnnotation(InjektFqNames.TypeParameterInfos)
      ?.readChunkedValue()
      ?.split("=:=")
      ?.get(cast<TypeParameterDescriptor>().index)
      ?.takeIf { it.isNotEmpty() }
      ?.decode<PersistedClassifierInfo>()
      ?.toClassifierInfo(context, trace)
  } else {
    annotations
      .findAnnotation(InjektFqNames.ClassifierInfo)
      ?.readChunkedValue()
      ?.cast<String>()
      ?.decode<PersistedClassifierInfo>()
      ?.toClassifierInfo(context, trace)
  })?.let {
    trace.record(InjektWritableSlices.CLASSIFIER_INFO, this, it)
    return it
  }

  val expandedType = (original as? TypeAliasDescriptor)?.underlyingType
    ?.toTypeRef(context, trace)

  val isQualifier = hasAnnotation(InjektFqNames.Qualifier)

  val lazySuperTypes = unsafeLazy {
    when {
      expandedType != null -> listOf(expandedType)
      isQualifier -> listOf(context.anyType)
      else -> typeConstructor.supertypes.map { it.toTypeRef(context, trace) }
    }
  }

  val primaryConstructorPropertyParameters = safeAs<ClassDescriptor>()
    ?.unsubstitutedPrimaryConstructor
    ?.valueParameters
    ?.asSequence()
    ?.filter { it.findPsi()?.safeAs<KtParameter>()?.isPropertyParameter() == true }
    ?.map { it.name.asString() }
    ?.toList()
    ?: emptyList()

  val isSpread = hasAnnotation(InjektFqNames.Spread) ||
      findPsi()?.safeAs<KtTypeParameter>()?.hasAnnotation(InjektFqNames.Spread) == true

  val isForTypeKey = hasAnnotation(InjektFqNames.ForTypeKey) ||
      findPsi()?.safeAs<KtTypeParameter>()?.hasAnnotation(InjektFqNames.ForTypeKey) == true

  val isSingletonInjectable = !isDeserializedDeclaration() &&
      this is ClassDescriptor &&
      kind == ClassKind.CLASS &&
      constructors
        .filter {
          it.hasAnnotation(InjektFqNames.Provide) ||
              (it.isPrimary && hasAnnotation(InjektFqNames.Provide))
        }
        .any { it.valueParameters.isEmpty() } &&
      declaredTypeParameters.none {
        it.classifierInfo(context, trace)
          .isForTypeKey
      } && unsubstitutedMemberScope.getContributedDescriptors()
    .none {
      (it is ClassDescriptor &&
          it.isInner) ||
          (it is PropertyDescriptor &&
              it.hasBackingField(trace.bindingContext))
    }

  val info = ClassifierInfo(
    qualifiers = getAnnotatedAnnotations(InjektFqNames.Qualifier)
      .map { it.type.toTypeRef(context, trace) },
    lazySuperTypes = lazySuperTypes,
    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
    isSpread = isSpread,
    isForTypeKey = isForTypeKey,
    isSingletonInjectable = isSingletonInjectable
  )

  trace.record(InjektWritableSlices.CLASSIFIER_INFO, this, info)

  persistInfoIfNeeded(info, context)

  return info
}

@Serializable
data class PersistedClassifierInfo(
  @SerialName("0") val qualifiers: List<PersistedTypeRef> = emptyList(),
  @SerialName("1") val superTypes: List<PersistedTypeRef> = emptyList(),
  @SerialName("2") val primaryConstructorPropertyParameters: List<String> = emptyList(),
  @SerialName("3") val isSpread: Boolean = false,
  @SerialName("4") val isForTypeKey: Boolean = false,
  @SerialName("5") val isSingletonInjectable: Boolean = false
)

fun PersistedClassifierInfo.toClassifierInfo(
  context: InjektContext,
  trace: BindingTrace
): ClassifierInfo = ClassifierInfo(
  qualifiers = qualifiers.map { it.toTypeRef(context, trace) },
  lazySuperTypes = unsafeLazy { superTypes.map { it.toTypeRef(context, trace) } },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread,
  isForTypeKey = isForTypeKey,
  isSingletonInjectable = isSingletonInjectable
)

fun ClassifierInfo.toPersistedClassifierInfo(
  context: InjektContext
): PersistedClassifierInfo = PersistedClassifierInfo(
  qualifiers = qualifiers.map { it.toPersistedTypeRef(context) },
  superTypes = superTypes.map { it.toPersistedTypeRef(context) },
  primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
  isSpread = isSpread,
  isForTypeKey = isForTypeKey,
  isSingletonInjectable = isSingletonInjectable
)

private fun ClassifierDescriptor.persistInfoIfNeeded(info: ClassifierInfo, context: InjektContext) {
  if (isExternalDeclaration(context) || isDeserializedDeclaration()) return

  if (this is TypeParameterDescriptor) {
    val container = containingDeclaration
    if (container is TypeAliasDescriptor) return

    if (!info.isSpread &&
      !info.isForTypeKey &&
      info.superTypes.none { it.shouldBePersisted() }
    ) return

    val typeParameterInfos = (container.annotations
      .findAnnotation(InjektFqNames.TypeParameterInfos)
      ?.readChunkedValue()
      ?.split("=:=")
      ?: run {
        when (container) {
          is CallableDescriptor -> container.typeParameters
          is ClassifierDescriptorWithTypeParameters -> container.declaredTypeParameters
          else -> return
        }.map { "" }
      }).toMutableList()
    if (typeParameterInfos[index].isEmpty()) {
      val serializedInfo = info.toPersistedClassifierInfo(context).encode()
      typeParameterInfos[index] = serializedInfo
      container.updateAnnotation(
        AnnotationDescriptorImpl(
          context.module.findClassAcrossModuleDependencies(
            ClassId.topLevel(InjektFqNames.TypeParameterInfos)
          )?.defaultType ?: return,
          typeParameterInfos.joinToString("=:=").toChunkedAnnotationArguments(),
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
        context.module.findClassAcrossModuleDependencies(
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
