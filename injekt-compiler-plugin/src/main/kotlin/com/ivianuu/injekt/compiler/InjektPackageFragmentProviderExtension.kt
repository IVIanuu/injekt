package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.Printer

class InjektPackageFragmentProviderExtension : PackageFragmentProviderExtension {
    override fun getPackageFragmentProvider(
        project: Project,
        module: ModuleDescriptor,
        storageManager: StorageManager,
        trace: BindingTrace,
        moduleInfo: ModuleInfo?,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider? = InjektPackageFragmentProvider(module, storageManager)
}

private class InjektPackageFragmentProvider(
    private val module: ModuleDescriptor,
    private val storageManager: StorageManager
) : PackageFragmentProvider {

    private val packages = mutableMapOf<FqName, PackageFragmentDescriptor>()

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        message("get fragments for $fqName")
        return listOf(
            packages.getOrPut(fqName) {
                InjektPackageFragmentDescriptor(module, fqName, storageManager)
            }
        )
    }

    override fun getSubPackagesOf(
        fqName: FqName,
        nameFilter: (Name) -> Boolean
    ): Collection<FqName> = packages
        .filter { (k, _) -> !k.isRoot && k.parent() == fqName }
        .map { it.key }
}

private class InjektPackageFragmentDescriptor(
    module: ModuleDescriptor,
    fqName: FqName,
    private val storageManager: StorageManager
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = InjektMemberScope()

    override fun getMemberScope(): MemberScope = memberScope

    private inner class InjektMemberScope : MemberScopeImpl() {

        private val allDescriptors = storageManager.createRecursionTolerantLazyValue(
            { computeDescriptors() },
            listOf()
        )

        private fun computeDescriptors(): List<DeclarationDescriptor> {
            val descriptors = mutableListOf<DeclarationDescriptor>()

            val otherDescriptors = module.getPackage(fqName)
                .memberScope.getContributedDescriptors(
                DescriptorKindFilter.ALL,
                MemberScope.ALL_NAME_FILTER
            )

            descriptors += otherDescriptors
                .filterIsInstance<ClassDescriptor>()
                .filter { it.annotations.hasAnnotation(InjektClassNames.ScopeMarker) }
                .map {
                    ClassDescriptorImpl(
                        it,
                        Name.identifier("Companion"),
                        Modality.FINAL,
                        ClassKind.OBJECT,
                        listOf(
                            module.findClassAcrossModuleDependencies(
                                ClassId.topLevel(InjektClassNames.Scope)
                            )!!.defaultType
                        ),
                        SourceElement.NO_SOURCE,
                        false,
                        storageManager
                    ).apply {
                        initialize(
                            MemberScope.Empty,
                            emptySet(),
                            null
                        )
                    }
                }

            descriptors += otherDescriptors
                .filterIsInstance<SimpleFunctionDescriptor>()
                .filter { it.annotations.hasAnnotation(InjektClassNames.KeyOverload) }
                .map { original -> KeyOverloadFunctionDescriptor(original, storageManager) }
                .also { message("overloaded functions 2 $it") }

            return descriptors
        }

        override fun getClassifierNames(): Set<Name>? {
            if (allDescriptors.isComputing()) return null
            return allDescriptors()
                .filterIsInstance<ClassifierDescriptor>()
                .map { it.name }
                .toSet()
        }

        override fun getContributedClassifier(
            name: Name,
            location: LookupLocation
        ): ClassifierDescriptor? {
            if (!allDescriptors.isComputed()) return null

            return allDescriptors()
                .filterIsInstance<ClassifierDescriptor>()
                .firstOrNull { it.name == name }
        }

        override fun getContributedFunctions(
            name: Name,
            location: LookupLocation
        ): Collection<SimpleFunctionDescriptor> {
            if (allDescriptors.isComputing()) return emptyList()

            return allDescriptors()
                .filterIsInstance<SimpleFunctionDescriptor>()
                .filter { it.name == name }
        }

        override fun getFunctionNames(): Set<Name> {
            if (allDescriptors.isComputing()) return emptySet()

            return allDescriptors()
                .filterIsInstance<FunctionDescriptor>()
                .map { it.name }
                .toSet()
        }

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> {
            if (allDescriptors.isComputing()) return emptyList()

            return allDescriptors().filter {
                nameFilter(it.name) && kindFilter.accepts(it)
            }
        }

        override fun printScopeStructure(p: Printer) {
        }
    }

}

class KeyOverloadFunctionDescriptor(
    val overloadedFunction: SimpleFunctionDescriptor,
    storageManager: StorageManager
) : SimpleFunctionDescriptorImpl(
    overloadedFunction.containingDeclaration,
    null,
    Annotations.EMPTY,
    overloadedFunction.name,
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    overloadedFunction.source
) {
    init {
        val typeParameters = overloadedFunction.typeParameters.map {
            TypeParameterDescriptorImpl.createForFurtherModification(
                this,
                it.annotations,
                it.isReified,
                it.variance,
                it.name,
                it.index,
                it.source,
                storageManager
            ).apply {
                it.upperBounds.forEach { addUpperBound(it) }
                setInitialized()
            }
        }

        val typeMap = overloadedFunction.typeParameters.mapIndexed { i, param ->
            param.defaultType to typeParameters[i].defaultType
        }.toMap() as Map<KotlinType, KotlinType>

        initialize(
            overloadedFunction.extensionReceiverParameter,
            null,
            typeParameters,
            listOf(
                ValueParameterDescriptorImpl(
                    this,
                    null,
                    0,
                    Annotations.EMPTY,
                    Name.identifier("qualifier"),
                    module.findClassAcrossModuleDependencies(
                        ClassId.topLevel(InjektClassNames.Qualifier)
                    )!!.defaultType,
                    true,
                    false,
                    false,
                    null,
                    SourceElement.NO_SOURCE
                )
            ) + overloadedFunction.valueParameters.drop(1)
                .map {
                    ValueParameterDescriptorImpl(
                        this,
                        null,
                        it.index,
                        it.annotations,
                        it.name,
                        it.type.replace2(typeMap),
                        it.declaresDefaultValue(),
                        it.isCrossinline,
                        it.isNoinline,
                        it.varargElementType,
                        it.source
                    )
                },
            overloadedFunction.returnType?.replace2(typeMap),
            overloadedFunction.modality,
            overloadedFunction.visibility,
            null
        )
    }
}

private fun KotlinType.replace2(
    map: Map<KotlinType, KotlinType>
): KotlinType {
    if (map.contains(this)) return map.getValue(this)
    return replace(
        newArguments = arguments.map { it.type.replace2(map).asTypeProjection() }
    )
}
