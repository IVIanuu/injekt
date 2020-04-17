package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.name.FqName

class InjektDeclarationStore(
    private val pluginContext: IrPluginContext,
    private val moduleFragment: IrModuleFragment
) {

    lateinit var componentTransformer: ComponentTransformer
    lateinit var moduleTransformer: ModuleTransformer

    fun getComponent(key: String): IrClass {
        return try {
            val componentPackage = pluginContext.moduleDescriptor
                .getPackage(InjektClassNames.InjektComponentsPackage)
            return pluginContext.symbolTable.referenceClass(
                componentPackage.memberScope
                    .getContributedDescriptors()
                    .filterIsInstance<ClassDescriptor>()
                    .single { it.name.asString().startsWith("$key\$") }
            ).ensureBound(pluginContext.irProviders).owner
        } catch (e: Exception) {
            try {
                moduleFragment.files
                    .flatMap { it.declarations }
                    .filterIsInstance<IrClass>()
                    .singleOrNull { it.name.asString().startsWith("$key\$") }
                    ?: throw IllegalStateException(e)
            } catch (e: Exception) {
                try {
                    componentTransformer.getProcessedComponent(key)
                        ?: throw IllegalStateException(e)
                } catch (e: Exception) {
                    throw IllegalStateException("Couldn't find component for $key", e)
                }
            }
        }
    }

    fun getModule(fqName: FqName): IrClass {
        return try {
            pluginContext.symbolTable.referenceClass(
                pluginContext.moduleDescriptor.getTopLevelClass(fqName)
            ).ensureBound(pluginContext.irProviders).owner
        } catch (e: Exception) {
            try {
                moduleFragment.files
                    .flatMap { it.declarations }
                    .filterIsInstance<IrClass>()
                    .firstOrNull { it.fqNameForIrSerialization == fqName }
                    ?: throw IllegalStateException(e)
            } catch (e: Exception) {
                try {
                    moduleTransformer.getProcessedModule(fqName)
                        ?: throw IllegalStateException(e)
                } catch (e: Exception) {
                    throw IllegalStateException("Couldn't find module for $fqName", e)
                }
            }
        }
    }

}
