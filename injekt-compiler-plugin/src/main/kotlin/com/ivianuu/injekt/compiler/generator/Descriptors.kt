package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.getFunctionType
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

data class FactoryDescriptor(
    val factoryType: Type,
) {
    val contextType = factoryType.typeArguments.last()
    val inputTypes = factoryType.typeArguments.dropLast(1)
}

data class Callable(
    val packageFqName: FqName,
    val fqName: FqName,
    val name: Name,
    val type: Type,
    val objectReceiver: Type?,
    val typeParameters: List<ClassifierRef>,
    val valueParameters: List<ValueParameterRef>,
    val targetComponent: Type?,
    val givenKind: GivenKind,
    val isCall: Boolean,
) {
    enum class GivenKind {
        GIVEN, GIVEN_MAP_ENTRIES, GIVEN_SET_ELEMENTS, MODULE
    }
}

fun FunctionDescriptor.toCallableRef(): Callable {
    val owner = when (this) {
        is ConstructorDescriptor -> constructedClass
        is PropertyAccessorDescriptor -> correspondingProperty
        else -> this
    }
    return Callable(
        name = owner.name,
        packageFqName = findPackage().fqName,
        fqName = owner.fqNameSafe,
        type = (if (extensionReceiverParameter != null || valueParameters.isNotEmpty()) getFunctionType() else returnType!!)
            .toTypeRef(),
        objectReceiver = dispatchReceiverParameter?.type?.constructor?.declarationDescriptor
            ?.takeIf { it is ClassDescriptor && it.kind == ClassKind.OBJECT }?.defaultType?.toTypeRef(),
        targetComponent = (owner.annotations.findAnnotation(InjektFqNames.Given)
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
            hasAnnotationWithPropertyAndClass(InjektFqNames.Given) -> Callable.GivenKind.GIVEN
            hasAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Effect) -> Callable.GivenKind.GIVEN
            hasAnnotationWithPropertyAndClass(InjektFqNames.GivenMapEntries) -> Callable.GivenKind.GIVEN_MAP_ENTRIES
            hasAnnotationWithPropertyAndClass(InjektFqNames.GivenSetElements) -> Callable.GivenKind.GIVEN_SET_ELEMENTS
            else -> error("Unexpected callable $this")
        },
        typeParameters = typeParameters.map { it.toClassifierRef() },
        valueParameters = listOfNotNull(
            extensionReceiverParameter?.type?.let {
                ValueParameterRef(
                    KotlinType(it),
                    true
                )
            }
        ) + valueParameters.map { ValueParameterRef(it.type.toTypeRef()) },
        isCall = owner is PropertyDescriptor || (owner is ClassDescriptor && owner.kind == ClassKind.OBJECT)
    )
}

data class ValueParameterRef(
    val type: Type,
    val isExtensionReceiver: Boolean = false,
    val isAssisted: Boolean = false,
)

data class ModuleDescriptor(
    val type: Type,
    val callables: List<Callable>,
)
