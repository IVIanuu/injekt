package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.getGivenFunctionType
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.generator.merge.MergeDeclarations
import com.ivianuu.injekt.compiler.generator.merge.MergeEntryPointDescriptor
import com.ivianuu.injekt.compiler.generator.merge.MergeModuleDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny

@Given
class DeclarationStore(private val module: ModuleDescriptor) {

    val generatedFactories: List<FactoryDescriptor> by ::_generatedFactories
    private val _generatedFactories = mutableListOf<FactoryDescriptor>()

    fun addGeneratedFactory(descriptor: FactoryDescriptor) {
        _generatedFactories += descriptor
    }

    val generatedModules: List<com.ivianuu.injekt.compiler.generator.ModuleDescriptor> by ::_generatedModules
    private val _generatedModules = mutableListOf<com.ivianuu.injekt.compiler.generator.ModuleDescriptor>()

    fun addGeneratedModule(module: com.ivianuu.injekt.compiler.generator.ModuleDescriptor) {
        _generatedModules += module
    }

    fun getMergeDeclarations(): MergeDeclarations {
        val mergeIndexPackage = memberScopeForFqName(InjektFqNames.MergeIndexPackage)!!
        val entryPoints = mutableListOf<MergeEntryPointDescriptor>()
        val modules = mutableListOf<MergeModuleDescriptor>()
        val mergeFactories = mutableListOf<TypeRef>()
        mergeIndexPackage.getContributedDescriptors(DescriptorKindFilter.VARIABLES)
            .filterIsInstance<PropertyDescriptor>()
            .map {
                val fqName = FqName(it.name.asString().replace("_", "."))
                val classifier = classifierDescriptorByFqName(fqName)
                when {
                    classifier.hasAnnotation(InjektFqNames.MergeFactory) -> {
                        mergeFactories += classifier.defaultType.toTypeRef()
                    }
                    classifier.hasAnnotation(InjektFqNames.Module) -> {
                        val component = classifier.annotations.findAnnotation(InjektFqNames.Module)!!
                            .allValueArguments
                            .get("installIn".asNameId())
                            .let { it as KClassValue }
                            .getArgumentType(module)
                            .toTypeRef()
                        modules += MergeModuleDescriptor(
                            component = component,
                            module = classifier.defaultType.toTypeRef()
                        )
                    }
                    classifier.hasAnnotation(InjektFqNames.EntryPoint) -> {
                        val component = classifier.annotations.findAnnotation(InjektFqNames.EntryPoint)!!
                            .allValueArguments
                            .get("installIn".asNameId())
                            .let { it as KClassValue }
                            .getArgumentType(module)
                            .toTypeRef()
                        entryPoints += MergeEntryPointDescriptor(
                            component = component,
                            entryPoint = classifier.defaultType.toTypeRef()
                        )
                    }

                }
            }

        return MergeDeclarations(entryPoints, modules, mergeFactories)
    }

    private val callablesForType = mutableMapOf<TypeRef, List<Callable>>()
    fun allCallablesForType(type: TypeRef): List<Callable> {
        return callablesForType.getOrPut(type) {
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
            ) as ClassDescriptor
        }
    }

    private val classifierDescriptorByFqName = mutableMapOf<FqName, ClassifierDescriptor>()
    fun classifierDescriptorByFqName(fqName: FqName): ClassifierDescriptor {
        return classifierDescriptorByFqName.getOrPut(fqName) {
            memberScopeForFqName(fqName.parent())!!.getContributedClassifier(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            )!!
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

            return classDescriptor.unsubstitutedMemberScope
        }
    }

    private val callablesByDescriptor = mutableMapOf<CallableDescriptor, Callable>()
    fun callableForDescriptor(descriptor: FunctionDescriptor): Callable = callablesByDescriptor.getOrPut(descriptor) {
        val owner = when (descriptor) {
            is ConstructorDescriptor -> descriptor.constructedClass
            is PropertyAccessorDescriptor -> descriptor.correspondingProperty
            else -> descriptor
        }
        return Callable(
            name = owner.name,
            packageFqName = descriptor.findPackage().fqName,
            fqName = owner.fqNameSafe,
            type = (
                    if (descriptor.allParameters.any { it.hasAnnotation(InjektFqNames.Assisted) })
                        descriptor.getGivenFunctionType() else descriptor.returnType!!
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
            typeParameters = descriptor.typeParameters.map { it.toClassifierRef() },
            valueParameters = listOfNotNull(
                descriptor.extensionReceiverParameter?.let {
                    ValueParameterRef(
                        type = it.type.toTypeRef(),
                        isExtensionReceiver = true,
                        isAssisted = it.hasAnnotation(InjektFqNames.Assisted),
                        name = "receiver".asNameId()
                    )
                }
            ) + descriptor.valueParameters.map {
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

    fun factoryForType(type: TypeRef): FactoryDescriptor = FactoryDescriptor(type)

    private val moduleByType = mutableMapOf<TypeRef, com.ivianuu.injekt.compiler.generator.ModuleDescriptor>()
    fun moduleForType(type: TypeRef): com.ivianuu.injekt.compiler.generator.ModuleDescriptor {
        return moduleByType.getOrPut(type) {
            _generatedModules.firstOrNull {
                it.type == type
            } ?: run {
                val descriptor = classDescriptorForFqName(type.classifier.fqName)
                ModuleDescriptor(
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
                        .map { callableForDescriptor(it) }
                )
            }
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
