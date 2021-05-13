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
    val type: TypeRef,
    val parameterTypes: Map<String, TypeRef> = emptyMap(),
    val givenParameters: Set<String> = emptySet(),
    val defaultOnAllErrorsParameters: Set<String> = emptySet()
)

fun CallableDescriptor.callableInfo(
    context: InjektContext,
    trace: BindingTrace
): CallableInfo {
    trace[InjektWritableSlices.CALLABLE_INFO, this]?.let { return it }

    annotations
        .findAnnotation(InjektFqNames.CallableInfo)
        ?.allValueArguments
        ?.get("value".asNameId())
        ?.value
        ?.cast<String>()
        ?.decode<PersistedCallableInfo>()
        ?.toCallableInfo(context, trace)
        ?.let {
            trace.record(InjektWritableSlices.CALLABLE_INFO, this, it)
            return it
        }

    val type = run {
        val psi = findPsi()
        if (psi is KtProperty && psi.initializer != null) {
            trace.get(InjektWritableSlices.EXPECTED_TYPE, psi.initializer)
        } else if (psi is KtFunction && psi.bodyExpression != null) {
            trace.get(InjektWritableSlices.EXPECTED_TYPE, psi.bodyExpression)
        } else null
    } ?: run {
        val qualifiers = if (this is ConstructorDescriptor)
            getAnnotatedAnnotations(InjektFqNames.Qualifier)
                .map { it.type.toTypeRef(context, trace) }
        else emptyList()
        qualifiers.wrap(returnType!!.toTypeRef(context, trace))
    }

    val parameterTypes = (if (this is ConstructorDescriptor) valueParameters else allParameters)
        .map { it.injektName() to it.type.toTypeRef(context, trace) }
        .toMap()

    val givenParameters = (if (this is ConstructorDescriptor) valueParameters else allParameters)
        .filter {
            it.hasAnnotation(InjektFqNames.Given) ||
                    (this is FunctionInvokeDescriptor ||
                            (this is GivenFunctionDescriptor &&
                                    underlyingDescriptor is FunctionInvokeDescriptor) &&
                            it.type.hasAnnotation(InjektFqNames.Given))
        }
        .mapTo(mutableSetOf()) { it.injektName() }

    val defaultOnAllErrorParameters = valueParameters
        .asSequence()
        .filter { it.annotations.hasAnnotation(InjektFqNames.DefaultOnAllErrors) }
        .mapTo(mutableSetOf()) { it.injektName() }

    val info = CallableInfo(
        type = type,
        parameterTypes = parameterTypes,
        givenParameters = givenParameters,
        defaultOnAllErrorsParameters = defaultOnAllErrorParameters
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
                !constructedClass.visibility.shouldPersistInfo())) return

    if (hasAnnotation(InjektFqNames.CallableInfo))
        return

    val shouldPersistInfo = hasAnnotation(InjektFqNames.Given) ||
            (this is ConstructorDescriptor &&
                    constructedClass.hasAnnotation(InjektFqNames.Given)) ||
            (this is PropertyDescriptor &&
                    overriddenTreeUniqueAsSequence(false)
                        .map { it.containingDeclaration }
                        .filterIsInstance<ClassDescriptor>()
                        .flatMap { clazz ->
                            val clazzClassifier = clazz.toClassifierRef(context, trace)
                            clazz.unsubstitutedPrimaryConstructor
                                ?.valueParameters
                                ?.filter {
                                    it.name == name &&
                                            it.name in clazzClassifier.primaryConstructorPropertyParameters &&
                                            it.isGiven(context, trace)
                                }
                                ?: emptyList()
                        }
                        .any())
            safeAs<FunctionDescriptor>()
                ?.valueParameters
                ?.any { it.hasAnnotation(InjektFqNames.Given) } == true ||
            info.type.anyType {
                it.classifier.isQualifier ||
                        (it.classifier.isTypeAlias &&
                                it.fullyExpandedType.isSuspendFunctionType)
            } ||
            info.parameterTypes.any { (_, parameterType) ->
                parameterType.anyType {
                    it.classifier.isQualifier ||
                            (it.classifier.isTypeAlias &&
                                    it.fullyExpandedType.isSuspendFunctionType)
                }
            }

    if (!shouldPersistInfo) return

    val serializedInfo = info.toPersistedCallableInfo(context).encode()

    updateAnnotation(
        AnnotationDescriptorImpl(
            context.module.findClassAcrossModuleDependencies(
                ClassId.topLevel(
                    InjektFqNames.CallableInfo
                ))!!.defaultType,
            mapOf("value".asNameId() to StringValue(serializedInfo)),
            SourceElement.NO_SOURCE
        )
    )
}

@Serializable
data class PersistedCallableInfo(
    @SerialName("0") val type: PersistedTypeRef,
    @SerialName("1") val parameterTypes: Map<String, PersistedTypeRef> = emptyMap(),
    @SerialName("2") val givenParameters: Set<String> = emptySet(),
    @SerialName("3") val defaultOnAllErrorsParameters: Set<String> = emptySet()
)

fun CallableInfo.toPersistedCallableInfo(context: InjektContext) = PersistedCallableInfo(
    type = type.toPersistedTypeRef(context),
    parameterTypes = parameterTypes
        .mapValues { it.value.toPersistedTypeRef(context) },
    givenParameters = givenParameters,
    defaultOnAllErrorsParameters = defaultOnAllErrorsParameters
)

fun PersistedCallableInfo.toCallableInfo(
    context: InjektContext,
    trace: BindingTrace
) = CallableInfo(
    type = type.toTypeRef(context, trace),
    parameterTypes = parameterTypes
        .mapValues { it.value.toTypeRef(context, trace) },
    givenParameters = givenParameters,
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
    val isGivenConstraint: Boolean,
    val isForTypeKey: Boolean,
    val isSingletonGiven: Boolean = false
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
            ?.allValueArguments
            ?.values
            ?.single()
            ?.cast<ArrayValue>()
            ?.value
            ?.get(cast<TypeParameterDescriptor>().index)
            ?.value
            ?.cast<String>()
            ?.takeIf { it.isNotEmpty() }
            ?.also {
                println()
            }
            ?.decode<PersistedClassifierInfo>()
            ?.toClassifierInfo(context, trace)
    } else {
        annotations
            .findAnnotation(InjektFqNames.ClassifierInfo)
            ?.allValueArguments
            ?.get("value".asNameId())
            ?.value
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

    val isGivenConstraint = hasAnnotation(InjektFqNames.Given) ||
            findPsi()?.safeAs<KtTypeParameter>()?.hasAnnotation(InjektFqNames.Given) == true

    val isForTypeKey = hasAnnotation(InjektFqNames.ForTypeKey) ||
            findPsi()?.safeAs<KtTypeParameter>()?.hasAnnotation(InjektFqNames.ForTypeKey) == true

    val isSingletonGiven = this is ClassDescriptor &&
            kind == ClassKind.CLASS &&
            constructors
                .filter {
                    it.hasAnnotation(InjektFqNames.Given) ||
                            (hasAnnotation(InjektFqNames.Given) && it.isPrimary)
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
        isGivenConstraint = isGivenConstraint,
        isForTypeKey = isForTypeKey,
        isSingletonGiven = isSingletonGiven
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
    @SerialName("3") val isGivenConstraint: Boolean = false,
    @SerialName("4") val isForTypeKey: Boolean = false,
    @SerialName("5") val isSingletonGiven: Boolean = false
)

fun PersistedClassifierInfo.toClassifierInfo(
    context: InjektContext,
    trace: BindingTrace
): ClassifierInfo = ClassifierInfo(
    qualifiers = qualifiers.map { it.toTypeRef(context, trace) },
    lazySuperTypes = unsafeLazy { superTypes.map { it.toTypeRef(context, trace) } },
    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
    isGivenConstraint = isGivenConstraint,
    isForTypeKey = isForTypeKey,
    isSingletonGiven = isSingletonGiven
)

fun ClassifierInfo.toPersistedClassifierInfo(
    context: InjektContext
): PersistedClassifierInfo = PersistedClassifierInfo(
    qualifiers = qualifiers.map { it.toPersistedTypeRef(context) },
    superTypes = superTypes.map { it.toPersistedTypeRef(context) },
    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
    isGivenConstraint = isGivenConstraint,
    isForTypeKey = isForTypeKey,
    isSingletonGiven = isSingletonGiven
)

private fun ClassifierDescriptor.persistInfoIfNeeded(info: ClassifierInfo, context: InjektContext) {
    if (isExternalDeclaration(context) || isDeserializedDeclaration()) return

    if (this is TypeParameterDescriptor) {
        val container = containingDeclaration
        if (container is TypeAliasDescriptor) return
        val typeParameterInfos = (container.annotations
            .findAnnotation(InjektFqNames.TypeParameterInfos)
            ?.allValueArguments
            ?.values
            ?.single()
            ?.cast<ArrayValue>()
            ?.value
            ?: run {
                when (container) {
                    is CallableDescriptor -> container.typeParameters
                    is ClassifierDescriptorWithTypeParameters -> container.declaredTypeParameters
                    else -> return
                }.map {
                    StringValue("")
                }
            }).toMutableList()
        if (typeParameterInfos[index].value.cast<String>().isEmpty()) {
            val serializedInfo = info.toPersistedClassifierInfo(context).encode()
            typeParameterInfos[index] = StringValue(serializedInfo)
            container.updateAnnotation(
                AnnotationDescriptorImpl(
                    context.module.findClassAcrossModuleDependencies(
                        ClassId.topLevel(
                            InjektFqNames.TypeParameterInfos
                        ))!!.defaultType,
                    mapOf("values".asNameId() to ArrayValue(typeParameterInfos) {
                        it.builtIns.array.defaultType.replace(
                            newArguments = listOf(it.builtIns.stringType.asTypeProjection())
                        )
                    }),
                    SourceElement.NO_SOURCE
                )
            )
        }
    } else if (this is DeclarationDescriptorWithVisibility) {
        if (!visibility.shouldPersistInfo()) return
        if (hasAnnotation(InjektFqNames.ClassifierInfo)) return

        val serializedInfo = info.toPersistedClassifierInfo(context).encode()

        updateAnnotation(
            AnnotationDescriptorImpl(
                context.module.findClassAcrossModuleDependencies(
                    ClassId.topLevel(
                        InjektFqNames.ClassifierInfo
                    ))!!.defaultType,
                mapOf("value".asNameId() to StringValue(serializedInfo)),
                SourceElement.NO_SOURCE
            )
        )
    }
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
        is GivenFunctionDescriptor -> underlyingDescriptor.updatePrivateFinalField<Annotations>(
            AnnotatedImpl::class,
            "annotations"
        ) { newAnnotations }
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
