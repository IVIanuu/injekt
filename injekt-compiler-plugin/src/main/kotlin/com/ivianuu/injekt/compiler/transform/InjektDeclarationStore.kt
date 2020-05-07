package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.transform.factory.FactoryModuleTransformer
import com.ivianuu.injekt.compiler.transform.factory.TopLevelFactoryTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.referenceFunction

class InjektDeclarationStore(private val pluginContext: IrPluginContext) {

    lateinit var classProviderTransformer: ClassProviderTransformer
    lateinit var factoryTransformer: TopLevelFactoryTransformer
    lateinit var factoryModuleTransformer: FactoryModuleTransformer
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
            .let { pluginContext.symbolTable.referenceClass(it) }
            .ensureBound(pluginContext.irProviders)
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
            .let { pluginContext.symbolTable.referenceClass(it) }
            .ensureBound(pluginContext.irProviders)
            .owner
    }

    fun getModuleFunctionForFactory(factoryFunction: IrFunction): IrFunction {
        factoryModuleTransformer.moduleFunctionsByFactoryFunctions[factoryFunction]?.let { return it }
        val memberScope =
            (factoryFunction.descriptor.containingDeclaration as? ClassDescriptor)?.unsubstitutedMemberScope
                ?: (factoryFunction.descriptor.containingDeclaration as? PackageFragmentDescriptor)?.getMemberScope()
                ?: error("Unexpected parent ${factoryFunction.descriptor.containingDeclaration} for ${factoryFunction.dump()}")
        return memberScope.getContributedDescriptors()
            .filterIsInstance<FunctionDescriptor>()
            .single {
                it.name == InjektNameConventions.getModuleNameForFactoryFunction(
                    factoryFunction.name
                )
            }
            .let { pluginContext.symbolTable.referenceFunction(it) }
            .ensureBound(pluginContext.irProviders)
            .owner
    }

    fun getModuleClass(moduleFunction: IrFunction): IrClass {
        return getModuleClassOrNull(moduleFunction)
            ?: throw DeclarationNotFound("Couldn't find module for ${moduleFunction.dump()}")
    }

    fun getModuleClassOrNull(moduleFunction: IrFunction): IrClass? {
        return try {
            moduleTransformer.getGeneratedModuleClass(moduleFunction)
                ?: throw DeclarationNotFound()
        } catch (e: DeclarationNotFound) {
            val memberScope =
                (moduleFunction.descriptor.containingDeclaration as? ClassDescriptor)?.unsubstitutedMemberScope
                    ?: (moduleFunction.descriptor.containingDeclaration as? PackageFragmentDescriptor)?.getMemberScope()
                    ?: error("Unexpected parent ${moduleFunction.descriptor.containingDeclaration} for ${moduleFunction.dump()}")
            return memberScope.getContributedClassifier(
                InjektNameConventions.getModuleClassNameForModuleFunction(moduleFunction.name),
                NoLookupLocation.FROM_BACKEND
            ).let { it as ClassDescriptor }
                .let { pluginContext.symbolTable.referenceClass(it) }
                .ensureBound(pluginContext.irProviders)
                .owner
        }
    }

}

class DeclarationNotFound(message: String? = null) : RuntimeException(message)
