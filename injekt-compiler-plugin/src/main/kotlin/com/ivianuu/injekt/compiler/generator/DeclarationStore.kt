package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny

@Binding(GenerationComponent::class)
class DeclarationStore(private val module: ModuleDescriptor) {

    private val internalIndices = mutableListOf<Index>()
    val internalGeneratedIndices: Map<KtFile, List<Index>> get() = _internalGeneratedIndices
    private val _internalGeneratedIndices = mutableMapOf<KtFile, MutableList<Index>>()

    fun addInternalIndex(index: Index) {
        internalIndices += index
    }

    fun addGeneratedInternalIndex(file: KtFile, index: Index) {
        _internalGeneratedIndices.getOrPut(file) { mutableListOf() } += index
    }

    fun constructorForComponent(type: TypeRef): Callable? {
        return classDescriptorForFqName(type.classifier.fqName)
            .unsubstitutedPrimaryConstructor
            ?.let { callableForDescriptor(it) }
    }

    private val allIndices by unsafeLazy {
        internalIndices + (memberScopeForFqName(InjektFqNames.IndexPackage)
            ?.getContributedDescriptors(DescriptorKindFilter.VALUES)
            ?.filterIsInstance<PropertyDescriptor>()
            ?.map {
                val annotation = it.annotations.findAnnotation(InjektFqNames.Index)!!
                val fqName = annotation.allValueArguments["fqName".asNameId()]!!.value as String
                val type = annotation.allValueArguments["type".asNameId()]!!.value as String
                Index(FqName(fqName), type)
            } ?: emptyList())
    }

    private val classIndices by unsafeLazy {
        allIndices
            .filter { it.type == "class" }
            .map { classDescriptorForFqName(it.fqName) }
    }

    private val functionIndices by unsafeLazy {
        allIndices
            .filter { it.type == "function" }
            .flatMap { functionDescriptorForFqName(it.fqName) }
    }

    private val propertyIndices by unsafeLazy {
        allIndices
            .filter { it.type == "property" }
            .flatMap { propertyDescriptorsForFqName(it.fqName) }
    }

    private val allBindings by unsafeLazy {
        classIndices
            .mapNotNull { it.getInjectConstructor() }
            .map { callableForDescriptor(it) } +
                functionIndices
                    .filter { it.hasAnnotation(InjektFqNames.Binding) }
                    .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.Binding) }
                    .map { callableForDescriptor(it.getter!!) }
    }

    private val bindingsByType = mutableMapOf<TypeRef, List<Callable>>()
    fun bindingsForType(type: TypeRef): List<Callable> = bindingsByType.getOrPut(type) {
        (allBindings + generatedBindings)
            .filter { type.isAssignable(it.type) }
    }

    private val generatedBindings = mutableListOf<Callable>()
    fun addGeneratedBinding(callable: Callable) {
        generatedBindings += callable
    }

    private val allMapEntries by unsafeLazy {
        functionIndices
                    .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
                    .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
                    .map { callableForDescriptor(it.getter!!) }
    }
    private val mapEntriesForType = mutableMapOf<TypeRef, List<Callable>>()
    fun mapEntriesByType(type: TypeRef): List<Callable> = mapEntriesForType.getOrPut(type) {
        return allMapEntries
            .filter { type.isAssignable(it.type) }
    }

    private val allSetElements by unsafeLazy {
        functionIndices
            .filter { it.hasAnnotation(InjektFqNames.SetElements) }
            .map { callableForDescriptor(it) } +
                propertyIndices
                    .filter { it.hasAnnotation(InjektFqNames.SetElements) }
                    .map { callableForDescriptor(it.getter!!) }
    }
    private val setElementsForType = mutableMapOf<TypeRef, List<Callable>>()
    fun setElementsByType(type: TypeRef): List<Callable> = setElementsForType.getOrPut(type) {
        return allSetElements
            .filter { type.isAssignable(it.type) }
    }

    val mergeComponents: List<TypeRef> by unsafeLazy {
        classIndices
            .filter { it.hasAnnotation(InjektFqNames.MergeComponent) }
            .map { it.defaultType.toTypeRef() }
    }

    private val allMergeDeclarationsByFqName by unsafeLazy {
        buildMap<FqName, MutableList<TypeRef>> {
            generatedMergeDeclarationsByComponent
                .forEach { (mergeComponent, declarations) ->
                    getOrPut(mergeComponent) { mutableListOf() } += declarations.map { it.type }
                }
            classIndices
                .filter { it.hasAnnotation(InjektFqNames.MergeInto) }
                .groupBy { declaration ->
                    declaration.annotations.findAnnotation(InjektFqNames.MergeInto)!!
                        .allValueArguments["component".asNameId()]!!
                        .let { it as KClassValue }
                        .getArgumentType(module)
                        .constructor
                        .declarationDescriptor!!
                        .fqNameSafe
                }
                .forEach { (mergeComponent, declarations) ->
                    getOrPut(mergeComponent) { mutableListOf() } += declarations.map { it.defaultType.toTypeRef() }
                }
        }
    }

    fun mergeDeclarationsForMergeComponent(component: FqName): List<TypeRef> =
        allMergeDeclarationsByFqName[component] ?: emptyList()

    private val generatedMergeDeclarationsByComponent = mutableMapOf<FqName, MutableList<com.ivianuu.injekt.compiler.generator.ModuleDescriptor>>()
    fun addGeneratedMergeModule(
        mergeComponent: TypeRef,
        moduleDescriptor: com.ivianuu.injekt.compiler.generator.ModuleDescriptor
    ) {
        generatedMergeDeclarationsByComponent.getOrPut(
            mergeComponent.classifier.fqName) { mutableListOf() } += moduleDescriptor
        moduleByType[moduleDescriptor.type] = moduleDescriptor
        callablesByType[moduleDescriptor.type] = moduleDescriptor.callables
    }

    private val callablesByType = mutableMapOf<TypeRef, List<Callable>>()
    fun allCallablesForType(type: TypeRef): List<Callable> {
        return callablesByType.getOrPut(type) {
            val callables = mutableListOf<Callable>()

            fun TypeRef.collect(typeArguments: List<TypeRef>) {
                val substitutionMap = classifier.typeParameters
                    .zip(typeArguments)
                    .toMap()

                callables += classDescriptorForFqName(classifier.fqName)
                    .unsubstitutedMemberScope
                    .getContributedDescriptors(DescriptorKindFilter.CALLABLES)
                    .filterIsInstance<CallableDescriptor>()
                    .filter { it.dispatchReceiverParameter?.type?.isAnyOrNullableAny() != true }
                    .mapNotNull {
                        when (it) {
                            is FunctionDescriptor -> callableForDescriptor(it)
                            is PropertyDescriptor -> callableForDescriptor(it.getter!!)
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

            type.collect(type.typeArguments)

            callables
        }
    }

    private val classDescriptorByFqName = mutableMapOf<FqName, ClassDescriptor>()
    fun classDescriptorForFqName(fqName: FqName): ClassDescriptor {
        return classDescriptorByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedClassifier(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ) as? ClassDescriptor ?: error("Could not get for $fqName")
        }
    }

    private val functionDescriptorsByFqName = mutableMapOf<FqName, List<FunctionDescriptor>>()
    fun functionDescriptorForFqName(fqName: FqName): List<FunctionDescriptor> {
        return functionDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedFunctions(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).toList()
        }
    }

    private val propertyDescriptorsByFqName = mutableMapOf<FqName, List<PropertyDescriptor>>()
    fun propertyDescriptorsForFqName(fqName: FqName): List<PropertyDescriptor> {
        return propertyDescriptorsByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedVariables(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).toList()
        }
    }

    private val memberScopeByFqName = mutableMapOf<FqName, MemberScope?>()
    fun memberScopeForFqName(fqName: FqName): MemberScope? {
        return memberScopeByFqName.getOrPut(fqName) {
            val pkg = module.getPackage(fqName)

            if (fqName.isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope

            val parentMemberScope = memberScopeForFqName(fqName.parent()) ?: return null

            val classDescriptor =
                parentMemberScope.getContributedClassifier(
                    fqName.shortName(),
                    NoLookupLocation.FROM_BACKEND
                ) as? ClassDescriptor ?: return null

            classDescriptor.unsubstitutedMemberScope
        }
    }

    private val callablesByDescriptor = mutableMapOf<CallableDescriptor, Callable>()
    fun callableForDescriptor(descriptor: FunctionDescriptor): Callable = callablesByDescriptor.getOrPut(descriptor) {
        val owner = when (descriptor) {
            is ConstructorDescriptor -> descriptor.constructedClass
            is PropertyAccessorDescriptor -> descriptor.correspondingProperty
            else -> descriptor
        }
        Callable(
            name = owner.name,
            packageFqName = descriptor.findPackage().fqName,
            fqName = owner.fqNameSafe,
            type = (
                    if (descriptor.allParameters.any { it.type.hasAnnotation(InjektFqNames.Assisted) })
                        descriptor.getBindingFunctionType() else descriptor.returnType!!
                    )
                .toTypeRef(),
            targetComponent = owner.annotations.findAnnotation(InjektFqNames.Binding)
                ?.allValueArguments
                ?.get("scopeComponent".asNameId())
                ?.let { it as KClassValue }
                ?.getArgumentType(module)
                ?.toTypeRef(),
            contributionKind = when {
                owner.hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) -> Callable.ContributionKind.BINDING
                owner.hasAnnotationWithPropertyAndClass(InjektFqNames.MapEntries) -> Callable.ContributionKind.MAP_ENTRIES
                owner.hasAnnotationWithPropertyAndClass(InjektFqNames.SetElements) -> Callable.ContributionKind.SET_ELEMENTS
                owner.hasAnnotationWithPropertyAndClass(InjektFqNames.Module) -> Callable.ContributionKind.MODULE
                else -> null
            },
            typeParameters = descriptor.typeParameters.map { it.toClassifierRef() },
            valueParameters = listOfNotNull(
                descriptor.extensionReceiverParameter?.let {
                    ValueParameterRef(
                        type = it.type.toTypeRef(),
                        isExtensionReceiver = true,
                        isAssisted = it.type.hasAnnotation(InjektFqNames.Assisted),
                        name = "receiver".asNameId()
                    )
                }
            ) + descriptor.valueParameters.map {
                ValueParameterRef(
                    type = it.type.toTypeRef(),
                    isExtensionReceiver = false,
                    isAssisted = it.type.hasAnnotation(InjektFqNames.Assisted),
                    name = it.name
                )
            },
            isCall = owner !is PropertyDescriptor &&
                    (owner !is ClassDescriptor || owner.kind != ClassKind.OBJECT),
            isSuspend = (owner is CallableDescriptor && owner.isSuspend),
            isExternal = owner is DeserializedDescriptor
        )
    }

    private val moduleByType = mutableMapOf<TypeRef, com.ivianuu.injekt.compiler.generator.ModuleDescriptor>()
    fun moduleForType(type: TypeRef): com.ivianuu.injekt.compiler.generator.ModuleDescriptor {
        return moduleByType.getOrPut(type) {
            val descriptor = classDescriptorForFqName(type.classifier.fqName)
            val substitutionMap = type.classifier.typeParameters
                .zip(type.typeArguments)
                .toMap()
            ModuleDescriptor(
                type = type,
                callables = descriptor.unsubstitutedMemberScope.getContributedDescriptors().filter {
                    it.hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.SetElements) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.MapEntries) ||
                            it.hasAnnotationWithPropertyAndClass(InjektFqNames.Module)
                }
                    .mapNotNull {
                        when (it) {
                            is PropertyDescriptor -> it.getter!!
                            is FunctionDescriptor -> it
                            else -> null
                        }
                    }
                    .map { callableDescriptor ->
                        val callable = callableForDescriptor(callableDescriptor)
                        callable.copy(
                            type = callable.type.substitute(substitutionMap),
                            valueParameters = callable.valueParameters.map {
                                it.copy(
                                    type = it.type.substitute(substitutionMap)
                                )
                            }
                        )
                    }
            )
        }
    }

    private val callableByFunctionAlias = mutableMapOf<TypeRef, Callable>()
    fun functionForAlias(aliasType: TypeRef): Callable {
        return callableByFunctionAlias.getOrPut(aliasType) {
            callableForDescriptor(
                memberScopeForFqName(aliasType.classifier.fqName.parent())!!
                    .getContributedFunctions(
                        aliasType.classifier.fqName.shortName(),
                        NoLookupLocation.FROM_BACKEND
                    )
                    .single()
            )
        }
    }

}
