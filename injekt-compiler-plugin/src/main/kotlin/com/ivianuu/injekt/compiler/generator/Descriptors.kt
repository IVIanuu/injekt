package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.getFunctionType
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.irtransform.asNameId
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

data class ContextFactoryDescriptor(
    val factoryType: TypeRef,
    val contextType: TypeRef,
    val inputTypes: List<TypeRef>
)

data class ContextFactoryImplDescriptor(
    val factoryImplFqName: FqName,
    val factory: ContextFactoryDescriptor
)

data class CallableRef(
    val packageFqName: FqName,
    val fqName: FqName,
    val name: Name,
    val uniqueKey: String,
    val type: TypeRef,
    val staticReceiver: TypeRef?,
    val typeParameters: List<ClassifierRef>,
    val valueParameters: List<ValueParameterRef>,
    val targetContext: TypeRef?,
    val givenKind: GivenKind,
    val isExternal: Boolean,
    val isPropertyAccessor: Boolean,
    val isSuspend: Boolean,
    val isReader: Boolean,
    val isComposable: Boolean,
    val isInline: Boolean
) {
    enum class GivenKind {
        GIVEN, GIVEN_MAP_ENTRIES, GIVEN_SET_ELEMENTS
    }
}

enum class Modifier {
    Inline,
    Vararg,
    Private,
    Internal,
    Protected
}

fun FunctionDescriptor.toCallableRef(): CallableRef {
    val owner = when (this) {
        is ConstructorDescriptor -> constructedClass
        is PropertyAccessorDescriptor -> correspondingProperty
        else -> this
    }
    return CallableRef(
        name = owner.name,
        packageFqName = findPackage().fqName,
        fqName = owner.fqNameSafe,
        type = (if (extensionReceiverParameter != null || valueParameters.isNotEmpty()) getFunctionType() else returnType!!)
            .toTypeRef(),
        staticReceiver = dispatchReceiverParameter?.type?.constructor?.declarationDescriptor
            ?.takeIf { it is ClassDescriptor && it.kind == ClassKind.OBJECT }?.defaultType?.toTypeRef(),
        isExternal = owner is DeserializedDescriptor,
        targetContext = (owner.annotations.findAnnotation(InjektFqNames.Given)
            ?.allValueArguments
            ?.get("scopeContext".asNameId())
            ?: owner.annotations.findAnnotation(InjektFqNames.GivenMapEntries)
                ?.allValueArguments
                ?.get("targetContext".asNameId())
            ?: owner.annotations.findAnnotation(InjektFqNames.GivenSetElements)
                ?.allValueArguments
                ?.get("targetContext".asNameId()))
            ?.let { it as KClassValue }
            ?.getArgumentType(module)
            ?.toTypeRef(),
        givenKind = when {
            hasAnnotationWithPropertyAndClass(InjektFqNames.Given) -> CallableRef.GivenKind.GIVEN
            hasAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Effect) -> CallableRef.GivenKind.GIVEN
            hasAnnotationWithPropertyAndClass(InjektFqNames.GivenMapEntries) -> CallableRef.GivenKind.GIVEN_MAP_ENTRIES
            hasAnnotationWithPropertyAndClass(InjektFqNames.GivenSetElements) -> CallableRef.GivenKind.GIVEN_SET_ELEMENTS
            else -> error("Unexpected callable $this")
        },
        typeParameters = typeParameters.map { it.toClassifierRef() },
        valueParameters = listOfNotNull(
            extensionReceiverParameter?.let {
                ValueParameterRef(
                    name = it.name,
                    isCrossline = false,
                    isNoinline = false,
                    typeRef = it.type.toTypeRef(),
                    isExtensionReceiver = true
                )
            }
        ) + valueParameters.map {
            ValueParameterRef(
                name = it.name,
                isCrossline = false,
                isNoinline = false,
                typeRef = it.type.toTypeRef(),
                isExtensionReceiver = true
            )
        },
        isPropertyAccessor = owner is PropertyDescriptor,
        uniqueKey = owner.uniqueKey(),
        isSuspend = isSuspend,
        isReader = hasAnnotation(InjektFqNames.Reader),

        )
}

data class ValueParameterRef(
    val name: Name,
    val isCrossline: Boolean,
    val isNoinline: Boolean,
    val typeRef: TypeRef,
    val isExtensionReceiver: Boolean = false
)
