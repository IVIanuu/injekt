package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.getGivenFunctionType
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
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

data class FactoryDescriptor(val factoryType: TypeRef) {
    val contextType = factoryType.expandedType!!.typeArguments.last()
    val inputTypes = factoryType.expandedType!!.typeArguments.dropLast(1)
}

data class Callable(
    val packageFqName: FqName,
    val fqName: FqName,
    val name: Name,
    val type: TypeRef,
    val typeParameters: List<ClassifierRef>,
    val valueParameters: List<ValueParameterRef>,
    val targetComponent: TypeRef?,
    val givenKind: GivenKind?,
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
        type = (
            if (allParameters.any { it.hasAnnotation(InjektFqNames.Assisted) })
                getGivenFunctionType() else returnType!!
            )
            .toTypeRef(),
        targetComponent = owner.annotations.findAnnotation(InjektFqNames.Given)
            ?.allValueArguments
            ?.get("scopeComponent".asNameId())
            ?.let { it as KClassValue }
            ?.getArgumentType(module)
            ?.toTypeRef(),
        givenKind = when {
            owner.hasAnnotationWithPropertyAndClass(InjektFqNames.Given) -> Callable.GivenKind.GIVEN
            owner.hasAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Effect) -> Callable.GivenKind.GIVEN
            owner.hasAnnotationWithPropertyAndClass(InjektFqNames.GivenMapEntries) -> Callable.GivenKind.GIVEN_MAP_ENTRIES
            owner.hasAnnotationWithPropertyAndClass(InjektFqNames.GivenSetElements) -> Callable.GivenKind.GIVEN_SET_ELEMENTS
            owner.hasAnnotationWithPropertyAndClass(InjektFqNames.Module) -> Callable.GivenKind.MODULE
            else -> null
        },
        typeParameters = typeParameters.map { it.toClassifierRef() },
        valueParameters = listOfNotNull(
            extensionReceiverParameter?.let {
                ValueParameterRef(
                    type = it.type.toTypeRef(),
                    isExtensionReceiver = true,
                    isAssisted = it.hasAnnotation(InjektFqNames.Assisted),
                    name = "receiver".asNameId()
                )
            }
        ) + valueParameters.map {
            ValueParameterRef(
                type = it.type.toTypeRef(),
                isExtensionReceiver = false,
                isAssisted = it.hasAnnotation(InjektFqNames.Assisted),
                name = it.name
            )
        },
        isCall = owner !is PropertyDescriptor &&
            (owner !is ClassDescriptor || owner.kind != ClassKind.OBJECT)
    )
}

data class ValueParameterRef(
    val type: TypeRef,
    val isExtensionReceiver: Boolean = false,
    val isAssisted: Boolean = false,
    val name: Name,
)

data class ModuleDescriptor(
    val type: TypeRef,
    val callables: List<Callable>,
)
