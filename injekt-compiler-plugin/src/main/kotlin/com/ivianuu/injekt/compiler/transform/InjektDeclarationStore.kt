package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.ensureBound
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.name.FqName

class InjektDeclarationStore(
    private val context: IrPluginContext,
    private val moduleFragment: IrModuleFragment,
    private val symbols: InjektSymbols
) {

    lateinit var classProviderTransformer: ClassProviderTransformer
    lateinit var factoryTransformer: TopLevelFactoryTransformer
    lateinit var membersInjectorTransformer: MembersInjectorTransformer
    lateinit var moduleTransformer: ModuleTransformer

    fun getProvider(clazz: IrClass): IrClass {
        classProviderTransformer.providersByClass[clazz]?.let { return it }
        val memberScope =
            (clazz.descriptor.containingDeclaration as? ClassDescriptor)?.unsubstitutedMemberScope
                ?: (clazz.descriptor.containingDeclaration as? PackageFragmentDescriptor)?.getMemberScope()
                ?: error("Unexpected parent ${clazz.descriptor.containingDeclaration} for ${clazz.dump()}")
        return memberScope.getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()
            .single { it.name == InjektNameConventions.getProviderNameForClass(clazz.name) }
            .let { context.symbolTable.referenceClass(it) }
            .ensureBound(context.irProviders)
            .owner
    }

    fun getMembersInjector(clazz: IrClass): IrClass {
        membersInjectorTransformer.membersInjectorByClass[clazz]?.let { return it }
        val memberScope =
            (clazz.descriptor.containingDeclaration as? ClassDescriptor)?.unsubstitutedMemberScope
                ?: (clazz.descriptor.containingDeclaration as? PackageFragmentDescriptor)?.getMemberScope()
                ?: error("Unexpected parent ${clazz.descriptor.containingDeclaration} for ${clazz.dump()}")
        return memberScope.getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()
            .single { it.name == InjektNameConventions.getMembersInjectorNameForClass(clazz.name) }
            .let { context.symbolTable.referenceClass(it) }
            .ensureBound(context.irProviders)
            .owner
    }

    fun getModule(fqName: FqName): IrClass {
        return getModuleOrNull(fqName)
            ?: throw DeclarationNotFound("Couldn't find module for $fqName")
    }

    fun getModuleOrNull(fqName: FqName): IrClass? {
        return try {
            moduleTransformer.getGeneratedModuleClass(fqName)
                ?: throw DeclarationNotFound()
        } catch (e: DeclarationNotFound) {
            try {
                moduleFragment.files
                    .flatMap { it.declarations }
                    .filterIsInstance<IrClass>()
                    .firstOrNull { it.fqNameForIrSerialization == fqName }
                    ?: throw DeclarationNotFound()
            } catch (e: DeclarationNotFound) {
                try {
                    symbols.getTopLevelClass(fqName).owner
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

}

class DeclarationNotFound(message: String? = null) : RuntimeException(message)
