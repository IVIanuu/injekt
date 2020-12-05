package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.ApplicationComponent
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer

@Binding(ApplicationComponent::class)
class InjektPackageFragmentProviderExtension : PackageFragmentProviderExtension {

    private val propertiesByPackage =
        mutableMapOf<FqName, MutableList<(PackageFragmentDescriptor) -> PropertyDescriptor>>()
    fun addGeneratedProperty(
        packageFqName: FqName,
        produceDescriptor: (PackageFragmentDescriptor) -> PropertyDescriptor
    ) {
        propertiesByPackage.getOrPut(packageFqName) { mutableListOf() } += produceDescriptor
    }

    private val typeAliasesByPackage =
        mutableMapOf<FqName, MutableList<(PackageFragmentDescriptor) -> TypeAliasDescriptor>>()
    fun addGeneratedTypeAlias(
        packageFqName: FqName,
        produceDescriptor: (PackageFragmentDescriptor) -> TypeAliasDescriptor
    ) {
        typeAliasesByPackage.getOrPut(packageFqName) { mutableListOf() } += produceDescriptor
    }

    override fun getPackageFragmentProvider(
        project: Project,
        module: ModuleDescriptor,
        storageManager: StorageManager,
        trace: BindingTrace,
        moduleInfo: ModuleInfo?,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider? {
        return object : PackageFragmentProvider {
            override fun getSubPackagesOf(
                fqName: FqName,
                nameFilter: (Name) -> Boolean
            ): Collection<FqName> = emptyList()

            override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
                val properties = propertiesByPackage[fqName]?.let { producers ->
                    storageManager.createMemoizedFunction { packageFragment: PackageFragmentDescriptor ->
                        producers
                            .map { it(packageFragment) }
                    }
                }
                val typeAliases = typeAliasesByPackage[fqName]?.let { producers ->
                    storageManager.createMemoizedFunction { packageFragment: PackageFragmentDescriptor ->
                        producers
                            .map { it(packageFragment) }
                    }
                }
                if (properties == null && typeAliases == null) return emptyList()
                return listOf(
                    object : PackageFragmentDescriptorImpl(module, fqName) {
                        val packageFragment = this
                        override fun getMemberScope(): MemberScope {
                            return object : MemberScope {
                                override fun getClassifierNames(): Set<Name>? =
                                    typeAliases?.invoke(packageFragment)
                                        ?.map { it.name }
                                        ?.toSet()

                                override fun getFunctionNames(): Set<Name> = emptySet()

                                override fun getVariableNames(): Set<Name> =
                                    properties?.invoke(packageFragment)
                                        ?.map { it.name }?.toSet() ?: emptySet()

                                override fun getContributedClassifier(
                                    name: Name,
                                    location: LookupLocation
                                ): ClassifierDescriptor? = typeAliases
                                    ?.invoke(packageFragment)
                                    ?.firstOrNull { it.name == name }

                                override fun getContributedDescriptors(
                                    kindFilter: DescriptorKindFilter,
                                    nameFilter: (Name) -> Boolean
                                ): Collection<DeclarationDescriptor> = (
                                    properties?.invoke(packageFragment) ?: emptyList()) +
                                        (typeAliases?.invoke(packageFragment) ?: emptyList())

                                override fun getContributedFunctions(
                                    name: Name,
                                    location: LookupLocation
                                ): Collection<SimpleFunctionDescriptor> = emptyList()

                                override fun getContributedVariables(
                                    name: Name,
                                    location: LookupLocation
                                ): Collection<PropertyDescriptor> = properties
                                    ?.invoke(packageFragment) ?: emptyList()

                                override fun printScopeStructure(p: Printer) {
                                }
                            }
                        }
                    }
                )
            }
        }
    }

}
