package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.transform.factory.FactoryModuleTransformer
import com.ivianuu.injekt.compiler.transform.factory.RootFactoryTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.referenceFunction

class InjektDeclarationStore(private val pluginContext: IrPluginContext) {

    lateinit var classFactoryTransformer: ClassFactoryTransformer
    lateinit var factoryTransformer: RootFactoryTransformer
    lateinit var factoryModuleTransformer: FactoryModuleTransformer
    lateinit var membersInjectorTransformer: MembersInjectorTransformer
    lateinit var moduleTransformer: ModuleTransformer

    private fun IrDeclaration.isExternalDeclaration() = origin ==
            IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
            origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB

    fun getFactoryForClass(clazz: IrClass): IrClass {
        return if (!clazz.isExternalDeclaration()) {
            classFactoryTransformer.getFactoryForClass(clazz)
        } else {
            val memberScope =
                (clazz.descriptor.containingDeclaration as? ClassDescriptor)?.unsubstitutedMemberScope
                    ?: (clazz.descriptor.containingDeclaration as? PackageFragmentDescriptor)?.getMemberScope()
                    ?: error("Unexpected parent ${clazz.descriptor.containingDeclaration} for ${clazz.dump()}")
            memberScope.getContributedDescriptors()
                .filterIsInstance<ClassDescriptor>()
                .single { it.name == InjektNameConventions.getFactoryNameForClass(clazz.name) }
                .let { pluginContext.symbolTable.referenceClass(it) }
                .ensureBound(pluginContext.irProviders)
                .owner
        }
    }

    fun getMembersInjectorForClass(clazz: IrClass): IrClass {
        return if (!clazz.isExternalDeclaration()) {
            membersInjectorTransformer.getMembersInjectorForClass(clazz)
        } else {
            val memberScope =
                (clazz.descriptor.containingDeclaration as? ClassDescriptor)?.unsubstitutedMemberScope
                    ?: (clazz.descriptor.containingDeclaration as? PackageFragmentDescriptor)?.getMemberScope()
                    ?: error("Unexpected parent ${clazz.descriptor.containingDeclaration} for ${clazz.dump()}")
            memberScope.getContributedDescriptors()
                .filterIsInstance<ClassDescriptor>()
                .single { it.name == InjektNameConventions.getMembersInjectorNameForClass(clazz.name) }
                .let { pluginContext.symbolTable.referenceClass(it) }
                .ensureBound(pluginContext.irProviders)
                .owner
        }
    }

    fun getModuleFunctionForFactory(factoryFunction: IrFunction): IrFunction {
        return if (!factoryFunction.isExternalDeclaration()) {
            factoryModuleTransformer.getModuleFunctionForFactoryFunction(factoryFunction)
        } else {
            val memberScope =
                (factoryFunction.descriptor.containingDeclaration as? ClassDescriptor)?.unsubstitutedMemberScope
                    ?: (factoryFunction.descriptor.containingDeclaration as? PackageFragmentDescriptor)?.getMemberScope()
                    ?: error("Unexpected parent ${factoryFunction.descriptor.containingDeclaration} for ${factoryFunction.dump()}")
            memberScope.getContributedDescriptors()
                .filterIsInstance<FunctionDescriptor>()
                .single {
                    it.name == InjektNameConventions.getModuleNameForFactoryFunction(factoryFunction)
                }
                .let { pluginContext.symbolTable.referenceFunction(it) }
                .ensureBound(pluginContext.irProviders)
                .owner
        }
    }

    fun getModuleClassForFunction(moduleFunction: IrFunction): IrClass {
        return getModuleClassForFunctionOrNull(moduleFunction)
            ?: throw IllegalStateException("Couldn't find module for ${moduleFunction.dump()}")
    }

    fun getModuleClassForFunctionOrNull(moduleFunction: IrFunction): IrClass? {
        return if (!moduleFunction.isExternalDeclaration()) {
            moduleTransformer.getModuleClassForFunction(moduleFunction)
        } else {
            val memberScope =
                (moduleFunction.descriptor.containingDeclaration as? ClassDescriptor)?.unsubstitutedMemberScope
                    ?: (moduleFunction.descriptor.containingDeclaration as? PackageFragmentDescriptor)?.getMemberScope()
                    ?: error("Unexpected parent ${moduleFunction.descriptor.containingDeclaration} for ${moduleFunction.dump()}")
            memberScope.getContributedClassifier(
                InjektNameConventions.getModuleClassNameForModuleFunction(moduleFunction),
                NoLookupLocation.FROM_BACKEND
            ).let { it as ClassDescriptor }
                .let { pluginContext.symbolTable.referenceClass(it) }
                .ensureBound(pluginContext.irProviders)
                .owner
        }
    }

}
