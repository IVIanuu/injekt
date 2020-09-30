package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.translate.utils.refineType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.upperIfFlexible

fun <D : DeclarationDescriptor> KtDeclaration.descriptor(
    bindingContext: BindingContext,
) = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? D

fun KotlinType.prepare(): KotlinType {
    var tmp = refineType()
    if (constructor is IntersectionTypeConstructor) {
        tmp = CommonSupertypes.commonSupertype(constructor.supertypes)
    }
    tmp = tmp.upperIfFlexible()
    return tmp
}

fun DeclarationDescriptor.hasAnnotationWithPropertyAndClass(
    fqName: FqName
): Boolean = hasAnnotation(fqName) ||
    (this is PropertyAccessorDescriptor && correspondingProperty.hasAnnotation(fqName)) ||
    (this is ConstructorDescriptor && constructedClass.hasAnnotation(fqName))

fun DeclarationDescriptor.hasAnnotatedAnnotationsWithPropertyAndClass(
    fqName: FqName
): Boolean = hasAnnotatedAnnotations(fqName) ||
    (
        this is PropertyAccessorDescriptor && correspondingProperty.hasAnnotatedAnnotations(
            fqName
        )
        ) ||
    (this is ConstructorDescriptor && constructedClass.hasAnnotatedAnnotations(fqName))

fun ClassDescriptor.getGivenConstructor(): ConstructorDescriptor? {
    constructors
        .firstOrNull { it.hasAnnotation(InjektFqNames.Given) }?.let { return it }
    if (!hasAnnotation(InjektFqNames.Given)) return null
    return unsubstitutedPrimaryConstructor
}

fun String.asNameId() = Name.identifier(this)

fun FqName.toFactoryImplFqName() =
    FqName("${asString()}Impl")

fun <T> unsafeLazy(init: () -> T) = lazy(LazyThreadSafetyMode.NONE, init)

fun joinedNameOf(
    packageFqName: FqName,
    fqName: FqName,
): Name {
    val joinedSegments = fqName.asString()
        .removePrefix(packageFqName.asString() + ".")
        .split(".")
    return joinedSegments.joinToString("_").asNameId()
}

fun String.removeIllegalChars() =
    replace(".", "")
        .replace("<", "")
        .replace(">", "")
        .replace(" ", "")
        .replace("[", "")
        .replace("]", "")
        .replace("@", "")
        .replace(",", "")
        .replace(" ", "")
        .replace("-", "")

fun FqName.toMemberScope(module: ModuleDescriptor): MemberScope? {
    val pkg = module.getPackage(this)

    if (isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope

    val parentMemberScope = parent().toMemberScope(module) ?: return null

    val classDescriptor =
        parentMemberScope.getContributedClassifier(
            shortName(),
            NoLookupLocation.FROM_BACKEND
        ) as? ClassDescriptor ?: return null

    return classDescriptor.unsubstitutedMemberScope
}

fun FqName.asClassDescriptor(module: ModuleDescriptor): ClassDescriptor {
    return parent().toMemberScope(module)!!.getContributedClassifier(
        shortName(), NoLookupLocation.FROM_BACKEND
    ) as ClassDescriptor
}

fun TypeRef.getAllCallables(module: ModuleDescriptor): List<Callable> {
    val callables = mutableListOf<Callable>()

    fun TypeRef.collect(typeArguments: List<TypeRef>) {
        val substitutionMap = classifier.typeParameters
            .zip(typeArguments)
            .toMap()

        callables += classifier.fqName.asClassDescriptor(module)
            .unsubstitutedMemberScope
            .getContributedDescriptors(DescriptorKindFilter.CALLABLES)
            .filterIsInstance<CallableDescriptor>()
            .filter { it.dispatchReceiverParameter?.type?.isAnyOrNullableAny() != true }
            .mapNotNull {
                when (it) {
                    is FunctionDescriptor -> it.toCallableRef()
                    is PropertyDescriptor -> it.getter!!.toCallableRef()
                    else -> null
                }
            }
            .map { callable ->
                callable.copy(
                    type = callable.type.substitute(substitutionMap),
                    valueParameters = callable.valueParameters.map {
                        it.copy(type = it.type.substitute(substitutionMap))
                    }
                )
            }

        superTypes
            .map { it.substitute(substitutionMap) }
            .forEach { it.collect(it.typeArguments) }
    }

    collect(typeArguments)

    return callables
}

fun getFactoryForType(type: TypeRef): FactoryDescriptor {
    return FactoryDescriptor(type)
}

fun getModuleForType(
    type: TypeRef,
    module: ModuleDescriptor,
): com.ivianuu.injekt.compiler.generator.ModuleDescriptor {
    val descriptor = type.classifier.fqName.asClassDescriptor(module)
    val substitutionMap = type.classifier.typeParameters
        .zip(type.typeArguments)
        .toMap()
    return ModuleDescriptor(
        type = type,
        callables = descriptor.unsubstitutedMemberScope.getContributedDescriptors(
            DescriptorKindFilter.CALLABLES
        ).filter {
            it.hasAnnotationWithPropertyAndClass(
                InjektFqNames.Given
            ) || it.hasAnnotationWithPropertyAndClass(InjektFqNames.GivenSetElements) ||
                it.hasAnnotationWithPropertyAndClass(InjektFqNames.GivenMapEntries) ||
                it.hasAnnotationWithPropertyAndClass(InjektFqNames.Module)
        }
            .mapNotNull {
                when (it) {
                    is PropertyDescriptor -> it.getter!!
                    is FunctionDescriptor -> it
                    else -> null
                }
            }
            .map { it.toCallableRef() }
    )
}

fun getFunctionForAlias(aliasType: TypeRef, module: ModuleDescriptor): Callable {
    return aliasType.classifier.fqName.parent().toMemberScope(module)!!
        .getContributedFunctions(
            aliasType.classifier.fqName.shortName(),
            NoLookupLocation.FROM_BACKEND
        )
        .single()
        .toCallableRef()
}
