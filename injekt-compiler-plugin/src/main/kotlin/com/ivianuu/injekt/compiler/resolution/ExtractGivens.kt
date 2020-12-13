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
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils
import org.jetbrains.kotlin.types.KotlinType

fun ClassDescriptor.extractGivensOfDeclaration(
    bindingContext: BindingContext,
    declarationStore: DeclarationStore,
): List<CallableDescriptor> {
    val primaryConstructorGivens = (unsubstitutedPrimaryConstructor
        ?.let { primaryConstructor ->
            val info = declarationStore.givenInfoFor(primaryConstructor)
            primaryConstructor.valueParameters
                .filter {
                    it.hasAnnotation(InjektFqNames.Given) ||
                            it.name in info.allGivens
                }
                .mapNotNull { bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, it] }
                .map { it }
        }
        ?: emptyList())

    val memberGivens = unsubstitutedMemberScope.getContributedDescriptors()
        .flatMap { declaration ->
            when (declaration) {
                is ClassDescriptor -> declaration.getGivenConstructors()
                    .flatMap { constructor ->
                        declaration.allGivenTypes()
                            .map { constructor.overrideType(it) }
                    }
                is PropertyDescriptor -> if (declaration.hasAnnotation(InjektFqNames.Given))
                    listOf(declaration) else emptyList()
                is FunctionDescriptor -> if (declaration.hasAnnotation(InjektFqNames.Given))
                    listOf(declaration) else emptyList()
                else -> emptyList()
            }
        }

    return primaryConstructorGivens + memberGivens
}

fun ClassDescriptor.extractGivenCollectionElementsOfDeclaration(bindingContext: BindingContext): List<CallableDescriptor> {
    val primaryConstructorGivens = (unsubstitutedPrimaryConstructor
        ?.let { primaryConstructor ->
            primaryConstructor.valueParameters
                .filter {
                    it.hasAnnotation(InjektFqNames.GivenMap) ||
                            it.hasAnnotation(InjektFqNames.GivenSet)
                }
                .mapNotNull { bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, it] }
                .map { it }
        }
        ?: emptyList())

    val memberGivens = unsubstitutedMemberScope.getContributedDescriptors()
        .flatMap { declaration ->
            when (declaration) {
                is PropertyDescriptor -> if (declaration.hasAnnotation(InjektFqNames.GivenMap) ||
                    declaration.hasAnnotation(InjektFqNames.GivenSet)
                )
                    listOf(declaration) else emptyList()
                is FunctionDescriptor -> if (declaration.hasAnnotation(InjektFqNames.GivenMap) ||
                    declaration.hasAnnotation(InjektFqNames.GivenSet)
                ) listOf(declaration) else emptyList()
                else -> emptyList()
            }
        }

    return primaryConstructorGivens + memberGivens
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
    return allParameters
        .filter {
            it.hasAnnotation(InjektFqNames.Given) ||
                    it.type.hasAnnotation(InjektFqNames.Given) ||
                    userData?.hasAnnotation(InjektFqNames.Given) == true ||
                    it.name in info.allGivens
        }
}

fun CallableDescriptor.extractGivenCollectionElementsOfCallable(): List<CallableDescriptor> =
    allParameters
        .filter {
            it.hasAnnotation(InjektFqNames.GivenMap) ||
                    it.type.hasAnnotation(InjektFqNames.GivenMap) ||
                    it.hasAnnotation(InjektFqNames.GivenSet) ||
                    it.type.hasAnnotation(InjektFqNames.GivenSet)
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
