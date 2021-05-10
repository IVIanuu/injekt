package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.com.intellij.openapi.project.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.extensions.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.storage.*
import org.jetbrains.kotlin.utils.*

class TypeClassPackageFragmentProviderExtension : PackageFragmentProviderExtension {
    override fun getPackageFragmentProvider(
        project: Project,
        module: ModuleDescriptor,
        storageManager: StorageManager,
        trace: BindingTrace,
        moduleInfo: ModuleInfo?,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider? = object : PackageFragmentProvider {
        private val context = InjektContext(module)

        override fun getSubPackagesOf(
            fqName: FqName,
            nameFilter: (Name) -> Boolean
        ): Collection<FqName> = emptyList()

        override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> =
            listOf(TypeClassCallablePackageFragmentDescriptor(context, fqName))
    }
}

private class TypeClassCallablePackageFragmentDescriptor(
    context: InjektContext,
    fqName: FqName
) : PackageFragmentDescriptorImpl(context.module, fqName) {
    private val functions by lazy {
        context.module.getPackage(fqName)
            .fragments
            .filter { it != this }
            .flatMap { otherPackageFragment ->
                (otherPackageFragment.getMemberScope()
                    .getClassifierNames()
                    ?: emptySet())
                    .mapNotNull {
                        otherPackageFragment.getMemberScope()
                            .getContributedClassifier(
                                it,
                                NoLookupLocation.FROM_BACKEND
                            )
                    }
            }
            .filterIsInstance<ClassDescriptor>()
            .filter { it.hasAnnotation(InjektFqNames.Extension) }
            .flatMap { it.getTypeClassFunctions() }
            .map { TypeClassFunctionDescriptor(context, it, this) }
    }

    private val _memberScope = object : MemberScopeImpl() {
        override fun getFunctionNames(): Set<Name> = functions
            .mapTo(mutableSetOf()) { it.name }

        override fun getContributedFunctions(
            name: Name,
            location: LookupLocation
        ): Collection<SimpleFunctionDescriptor> = functions
            .filter { it.name == name }

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> = functions

        override fun printScopeStructure(p: Printer) {
        }
    }
    override fun getMemberScope(): MemberScope = _memberScope
}
