package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun ClassDescriptor.extractGivensOfDeclaration(
    declarationStore: DeclarationStore,
): List<CallableDescriptor> =
    unsubstitutedMemberScope.extractGivenCallables(defaultType, declarationStore)

fun MemberScope.extractGivenCallables(
    type: KotlinType,
    declarationStore: DeclarationStore,
): List<CallableDescriptor> {
    val primaryConstructorGivensNames = (type.constructor.declarationDescriptor
        ?.safeAs<ClassDescriptor>()
        ?.unsubstitutedPrimaryConstructor
        ?.let { primaryConstructor ->
            val info = declarationStore.givenInfoFor(primaryConstructor)
            primaryConstructor.valueParameters
                .filter {
                    it.hasAnnotation(InjektFqNames.Given) ||
                            it.name in info.allGivens
                }
                .map { it.name }
        }
        ?: emptyList())
    return getContributedDescriptors()
        .flatMap { declaration ->
            when (declaration) {
                is ClassDescriptor -> declaration.getGivenConstructors()
                    .flatMap { constructor ->
                        declaration.allGivenTypes()
                            .map { constructor.overrideType(it) }
                    }
                is PropertyDescriptor -> if (declaration.hasAnnotation(InjektFqNames.Given) ||
                    declaration.name in primaryConstructorGivensNames
                )
                    listOf(declaration) else emptyList()
                is FunctionDescriptor -> if (declaration.hasAnnotation(InjektFqNames.Given))
                    listOf(declaration) else emptyList()
                else -> emptyList()
            }
        }
}

fun ClassDescriptor.extractGivenSetElementsOfDeclaration(): List<CallableDescriptor> =
    unsubstitutedMemberScope.extractGivenSetElements(defaultType)

fun MemberScope.extractGivenSetElements(type: KotlinType): List<CallableDescriptor> {
    val primaryConstructorGivensNames = (type.constructor.declarationDescriptor
        ?.safeAs<ClassDescriptor>()
        ?.unsubstitutedPrimaryConstructor
        ?.let { primaryConstructor ->
            primaryConstructor.valueParameters
                .filter { it.hasAnnotation(InjektFqNames.GivenSetElement) }
                .map { it.name }
        }
        ?: emptyList())
    return getContributedDescriptors()
        .flatMap { declaration ->
            when (declaration) {
                is PropertyDescriptor -> if (declaration.hasAnnotation(InjektFqNames.GivenSetElement) ||
                    declaration.name in primaryConstructorGivensNames
                )
                    listOf(declaration) else emptyList()
                is FunctionDescriptor -> if (declaration.hasAnnotation(InjektFqNames.GivenSetElement))
                    listOf(declaration) else emptyList()
                else -> emptyList()
            }
        }
}

fun ConstructorDescriptor.allGivenTypes(): List<KotlinType> =
    constructedClass.allGivenTypes()

fun ClassDescriptor.allGivenTypes(): List<KotlinType> = buildList<KotlinType> {
    this += defaultType
    this += defaultType.constructor.supertypes
        .filter { it.hasAnnotation(InjektFqNames.Given) }
}

fun CallableDescriptor.extractGivensOfCallable(
    declarationStore: DeclarationStore,
): List<CallableDescriptor> {
    val info = declarationStore.givenInfoFor(this)
    val userData = getUserData(DslMarkerUtils.FunctionTypeAnnotationsKey)
    fun ParameterDescriptor.isGiven(): Boolean {
        return hasAnnotation(InjektFqNames.Given) ||
                type.hasAnnotation(InjektFqNames.Given) ||
                userData?.hasAnnotation(InjektFqNames.Given) == true ||
                name in info.allGivens
    }

    val allGivens = mutableListOf<CallableDescriptor>()

    allGivens += allParameters
        .filter { it.isGiven() }

    allGivens += extensionReceiverParameter?.type?.memberScope?.extractGivenCallables(
        extensionReceiverParameter!!.type, declarationStore
    ) ?: emptyList()

    return allGivens
}

fun CallableDescriptor.extractGivenSetElementsOfCallable(): List<CallableDescriptor> =
    allParameters
        .filter {
            it.hasAnnotation(InjektFqNames.GivenSetElement) ||
                    it.type.hasAnnotation(InjektFqNames.GivenSetElement)
        }

fun ClassConstructorDescriptor.overrideType(type: KotlinType): ClassConstructorDescriptor {
    return object : ClassConstructorDescriptor by this {
        override fun getReturnType(): KotlinType = type
        override fun getOriginal(): ClassConstructorDescriptor = this@overrideType
    }
}

fun ClassDescriptor.getGivenConstructors(): List<ClassConstructorDescriptor> {
    return constructors
        .filter {
            (it.isPrimary && hasAnnotation(InjektFqNames.Given)) ||
                    it.hasAnnotation(InjektFqNames.Given)
        }
}
